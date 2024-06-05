package io.tonblocks.overlay

import io.github.andreypfau.tl.serialization.TL
import io.github.andreypfau.tl.serialization.encodeToSink
import io.github.reactivecircus.cache4k.Cache
import io.tonblocks.adnl.AdnlAddressResolver
import io.tonblocks.adnl.AdnlClient
import io.tonblocks.adnl.AdnlConnection
import io.tonblocks.adnl.AdnlIdShort
import io.tonblocks.adnl.AdnlLocalNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.Clock
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import tl.ton.overlay.OverlayMessage
import tl.ton.overlay.OverlayNodes
import tl.ton.overlay.OverlayQuery
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

interface Overlay : CoroutineScope {
    val id: OverlayIdFull
    val isPublic: Boolean
    val addressResolver: AdnlAddressResolver

    val maxPeers: Int get() = 20

    val maxNeighbours: Int get() = 5

    fun addPeer(node: OverlayNode)

    suspend fun receiveMessage(source: AdnlClient, data: Source)

    suspend fun receiveQuery(source: AdnlClient, data: Source): Buffer

    suspend fun receiveBroadcast(source: ByteString, data: Source)

    suspend fun checkBroadcast(source: Source, data: Source) {}
}

abstract class AbstractOverlay(
    val localNode: AdnlLocalNode,
    override val id: OverlayIdFull,
    override val isPublic: Boolean,
    nodes: List<OverlayNode> = emptyList(),
    coroutineContext: CoroutineContext = localNode.coroutineContext
) : Overlay {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = coroutineContext + job
    private val peers_ = OverlayPeerList<OverlayNode>(nodes.toMutableList())
    private val connections = Cache.Builder<AdnlIdShort, OverlayConnection>()
        .expireAfterAccess(15.minutes)
        .build()
    private val selfNode by lazy {
        selfOverlayNode().sign(localNode.key).tl()
    }

    val peers: List<OverlayNode> get() = peers_

    override fun addPeer(node: OverlayNode) {
        if (node.overlayId != id.shortId()) return
        if (!node.checkSignature()) return
        peers_.add(node)?.let { contested ->
            removePeer(contested)
        }
    }

    fun removePeer(node: OverlayNode) {
        connections.invalidate(node.source.shortId())
        peers_.evict(node)
    }

    private suspend fun connection(node: OverlayNode): OverlayConnection? {
        val shortId = node.source.shortId()
        var connection = connections.get(shortId)
        if (connection != null) {
            return connection
        }
        println("try to connect to $node")
        val adnlConnection = localNode.connection(node.source, addressResolver) ?: return null
        connection = OverlayConnection(adnlConnection)
        connections.put(shortId, connection)
        return connection
    }

    suspend fun searchRandomPeers(nodesResolver: OverlayNodesResolver) {
        val randomPeers = nodesResolver.resolveNodes(id) ?: return
        println("$this found ${randomPeers.size} peers from dht")
        randomPeers.forEach {
            addPeer(it)
        }
    }

    suspend fun searchRandomPeers(node: OverlayNode) {
        val connection = requireNotNull(connection(node)) { "Unreachable: $node" }
        val knownPeers = OverlayNodes(
            listOf(selfNode) + peers_.asSequence().shuffled().take(maxNeighbours - 1).map { it.tl() }.toList()
        )
        val client = OverlayClient(connection)
        val randomPeers = client.getRandomPeers(knownPeers).nodes.map { OverlayNode(it) }
        println("found ${randomPeers.size} from $node")
        randomPeers.forEach {
            addPeer(it)
        }
    }

    private inner class OverlayConnection(
        val connection: AdnlConnection
    ) : AdnlClient {
        override suspend fun sendQuery(data: Source): Source {
            val buffer = Buffer()
            TL.Boxed.encodeToSink(buffer, OverlayQuery(id.shortId().publicKeyHash))
            data.transferTo(buffer)
            return connection.sendQuery(buffer)
        }

        override suspend fun sendMessage(data: Source) {
            val buffer = Buffer()
            TL.encodeToSink(buffer, OverlayMessage(id.shortId().publicKeyHash))
            data.transferTo(buffer)
            connection.sendQuery(buffer)
        }
    }

    private fun selfOverlayNode(
        version: Int = Clock.System.now().epochSeconds.toInt()
    ): OverlayNode = OverlayNode(localNode.id, id.shortId(), version).sign(localNode.key)

    override fun toString(): String = "[$id]"
}

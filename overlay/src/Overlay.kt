package io.tonblocks.overlay

import io.github.andreypfau.tl.serialization.TL
import io.github.reactivecircus.cache4k.Cache
import io.tonblocks.adnl.AdnlAddressResolver
import io.tonblocks.adnl.AdnlClient
import io.tonblocks.adnl.AdnlConnection
import io.tonblocks.adnl.AdnlIdShort
import io.tonblocks.adnl.AdnlLocalNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.io.Buffer
import kotlinx.io.Source
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
}

abstract class OverlayImpl(
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

    val peers: List<OverlayNode> get() = peers_

    override fun addPeer(node: OverlayNode) {
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
        println("resolve nodes: ${id.shortId()}")
        val randomPeers = nodesResolver.resolveNodes(id) ?: return
        println("$this found ${randomPeers.size} peers from dht")
        randomPeers.forEach {
            addPeer(it)
        }
    }

    suspend fun searchRandomPeers(node: OverlayNode) {
        println("try to find other peers in $node")
        val connection = connection(node) ?: return
        println("Got connection, asking query")
        val randomPeers = OverlayClient(connection).getRandomPeers(
            OverlayNodes(peers_.asSequence().shuffled().take(maxNeighbours).map { it.tl() }.toList())
        ).nodes.map { OverlayNode(it) }
        randomPeers.forEach {
            addPeer(it)
        }
    }

    private inner class OverlayConnection(
        val connection: AdnlConnection
    ) : AdnlClient {
        override suspend fun sendQuery(query: Source): Source {
            val buffer = Buffer()
            val queryPrefix = OverlayQuery(
                overlay = id.shortId().publicKeyHash
            )
            TL.encodeToSink(OverlayQuery.serializer(), buffer, queryPrefix)
            query.transferTo(buffer)
            val answer = connection.sendQuery(buffer)
            val answerPrefix = TL.Boxed.decodeFromSource(OverlayQuery.serializer(), answer)
            check(queryPrefix == answerPrefix) {
                "Invalid answer overlay prefix, expected: $queryPrefix, actual: $answerPrefix"
            }
            return answer
        }

        override suspend fun sendCustom(data: Source) {
            val buffer = Buffer()
            val messagePrefix = OverlayMessage(
                overlay = id.shortId().publicKeyHash
            )
            TL.encodeToSink(OverlayMessage.serializer(), buffer, messagePrefix)
            data.transferTo(buffer)
            connection.sendQuery(buffer)
        }
    }

    override fun toString(): String = "[$id]"
}

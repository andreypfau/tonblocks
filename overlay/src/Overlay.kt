package io.tonblocks.overlay

import io.github.andreypfau.tl.serialization.TL
import io.github.andreypfau.tl.serialization.decodeFromSource
import io.github.andreypfau.tl.serialization.encodeToSink
import io.github.reactivecircus.cache4k.Cache
import io.tonblocks.adnl.AdnlConnection
import io.tonblocks.adnl.AdnlIdShort
import io.tonblocks.overlay.broadcast.BroadcastIdShort
import io.tonblocks.overlay.broadcast.SimpleBroadcastHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.io.InternalIoApi
import kotlinx.io.Sink
import kotlinx.io.Source
import tl.ton.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

typealias OverlayMessageCallback = suspend (OverlayConnection).(message: Source) -> Unit
typealias OverlayQueryCallback = suspend (OverlayConnection).(query: Source, answer: Sink) -> Unit

interface Overlay : CoroutineScope {
    val localNode: OverlayLocalNode
    val id: OverlayIdFull
    val isPublic: Boolean

    suspend fun connection(adnlConnection: AdnlConnection): OverlayConnection

    suspend fun receiveMessage(connection: OverlayConnection, data: Source)

    suspend fun receiveQuery(connection: OverlayConnection, query: Source, answer: Sink)

    fun subscribeMessage(callback: OverlayMessageCallback)

    fun subscribeQuery(callback: OverlayQueryCallback)
}

class OverlayImpl(
    override val localNode: OverlayLocalNode,
    override val id: OverlayIdFull,
    override val isPublic: Boolean,
    nodes: List<OverlayNode> = emptyList(),
    coroutineContext: CoroutineContext = localNode.coroutineContext
) : Overlay {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = coroutineContext + job
    private val peers_ = OverlayPeerList(nodes.toMutableList())
    private val connections = Cache.Builder<AdnlIdShort, OverlayConnection>()
        .expireAfterAccess(1.minutes)
        .build()
    private val simpleBroadcasts = SimpleBroadcastHandler(this)
    private val messageCallbacks = ArrayList<OverlayMessageCallback>()
    private val queryCallbacks = ArrayList<OverlayQueryCallback>()

    val peers: List<OverlayNode> get() = peers_

    override suspend fun connection(adnlConnection: AdnlConnection): OverlayConnection {
        return connections.get(adnlConnection.remotePeer.id.shortId()) {
            OverlayConnection(this, adnlConnection)
        }
    }

    @OptIn(InternalIoApi::class)
    override suspend fun receiveMessage(connection: OverlayConnection, data: Source) {
        val broadcast = try {
            TL.Boxed.decodeFromSource<TlOverlayBroadcast>(data.peek())
        } catch (e: Exception) {
            null
        }
        if (broadcast != null) {
            data.buffer.clear()
            when (broadcast) {
                is OverlayBroadcast.Broadcast -> simpleBroadcasts.handleBroadcast(connection, broadcast)
                else -> TODO("Unsupported broadcast: ${broadcast::class.simpleName} from ${connection.adnl.remotePeer.id.shortId()}")
            }
        } else {
            val iterator = messageCallbacks.iterator()
            while (!data.exhausted() && iterator.hasNext()) {
                iterator.next().invoke(connection, data)
            }
        }
    }

    override suspend fun receiveQuery(connection: OverlayConnection, query: Source, answer: Sink) {
        suspend fun handleCustomQuery() {
            val iterator = queryCallbacks.iterator()
            while (!query.exhausted() && iterator.hasNext()) {
                iterator.next().invoke(connection, query, answer)
            }
        }

        val function = try {
            TL.Boxed.decodeFromSource<TonApiFunction>(query.peek())
        } catch (e: Exception) {
            return handleCustomQuery()
        }

        return when (function) {
            is OverlayGetRandomPeers -> {
                throw IllegalStateException("Dropping $function from ${connection.adnl.remotePeer.id.shortId()}")
            }

            is OverlayGetBroadcast -> {
                val id = BroadcastIdShort(function.hash)
                val broadcast = simpleBroadcasts[id]?.tl()
                TL.Boxed.encodeToSink(answer, broadcast ?: OverlayBroadcast.BroadcastNotFound)
            }

            is OverlayGetBroadcastList -> {
                throw IllegalStateException("Dropping $function from ${connection.adnl.remotePeer.id.shortId()}")
            }

            else -> {
                handleCustomQuery()
            }
        }
    }

    override fun subscribeMessage(callback: OverlayMessageCallback) {
        messageCallbacks.add(callback)
    }

    override fun subscribeQuery(callback: OverlayQueryCallback) {
        queryCallbacks.add(callback)
    }

//    private suspend fun connection(node: OverlayNode): OverlayConnection? {
//        val shortId = node.source.shortId()
//        var connection = connections.get(shortId)
//        if (connection != null) {
//            return connection
//        }
//        println("try to connect to $node")
//        val adnlConnection = localNode.connection(node.source, addressResolver) ?: return null
//        connection = OverlayConnection(this, adnlConnection)
//        connections.put(shortId, connection)
//        return connection
//    }

//    suspend fun searchRandomPeers(nodesResolver: OverlayNodesResolver) {
//        val randomPeers = nodesResolver.resolveNodes(id) ?: return
//        println("$this found ${randomPeers.size} peers from dht")
//        randomPeers.forEach {
//            addPeer(it)
//        }
//    }
//
//    suspend fun searchRandomPeers(node: OverlayNode) {
//        val connection = requireNotNull(connection(node)) { "Unreachable: $node" }
//        val knownPeers = OverlayNodes(
//            listOf(selfNode) + peers_.asSequence().shuffled().take(maxNeighbours - 1).map { it.tl() }.toList()
//        )
//        val client = OverlayClient(connection)
//        val randomPeers = client.getRandomPeers(knownPeers).nodes.map { OverlayNode(it) }
//        println("found ${randomPeers.size} from $node")
//        randomPeers.forEach {
//            addPeer(it)
//        }
//    }

//    private fun selfOverlayNode(
//        version: Int = Clock.System.now().epochSeconds.toInt()
//    ): OverlayNode = OverlayNode(localNode.id, id.shortId(), version).sign(localNode.key)

    override fun toString(): String = "[overlay ${id.shortId()}]"
}

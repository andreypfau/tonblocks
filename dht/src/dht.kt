package io.tonblocks.dht

import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.*
import io.tonblocks.adnl.transport.AdnlTransport
import io.tonblocks.crypto.ed25519.Ed25519
import io.tonblocks.kv.KeyValueRepository
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import tl.ton.dht.DhtValueResult
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

/**
 * TON Distributed Hash Table interface
 */
interface Dht : AdnlAddressResolver, CoroutineScope {
    /**
     * The Kademlia distance between nodes in the DHT
     */
    val k: Int

    /**
     * The number of nodes to query in parallel
     */
    val a: Int

    /**
     * Get a value from the DHT by key hash
     */
    suspend fun get(hash: DhtKeyHash): DhtValue?

    /**
     * Get a value from the DHT by key preimage
     */
    suspend fun get(key: DhtKey): DhtValue? = get(key.hash())

    /**
     * Set a value in the DHT
     */
    suspend fun set(value: DhtValue)
}

class DhtImpl(
    val localNode: AdnlLocalNode,
    val transport: AdnlTransport,
    val repository: KeyValueRepository<DhtKeyHash, DhtValue>,
    override val k: Int = 10,
    override val a: Int = 3,
) : Dht {
    private val job = Job()
    override val coroutineContext: CoroutineContext = transport.coroutineContext + job

    val keyId = localNode.key.hash()
    val routingTable = KademliaRoutingTable<RemoteDhtNode>(
        keyId,
        k,
        { it.node.id.shortId().publicKeyHash }
    )

    fun addNode(node: DhtNode): RemoteDhtNode {
        val nodeNode = RemoteDhtNode(node)
        return routingTable.add(nodeNode) ?: nodeNode
    }

    override suspend fun set(value: DhtValue) {
        TODO()
//        if (value.isExpired()) {
//            return
//        }
//        val hash = value.description.key.hash()
//        repository.put(hash, value)
//
//        val nodes = routingTable.nearest(hash, k * 2)
//        coroutineScope {
//            nodes
//                .asSequence()
//                .chunked(a)
//                .forEach { nodes ->
//                    nodes.map {
//                        async {
//                            it.sendStore(value)
//                        }
//                    }.awaitAll()
//                }
//        }
    }

    override suspend fun get(hash: DhtKeyHash): DhtValue? {
        val storedValue = repository.get(hash)
        if (storedValue != null) {
            if (storedValue.isExpired()) {
                repository.remove(hash)
            } else {
                return storedValue
            }
        }
        val foundValue = CompletableDeferred<DhtValue?>()
        val findJeb = launch {
            val visitedNodes = HashSet<RemoteDhtNode>()
            var newNodes = routingTable.nearest(hash, k)
            while (true) {
                newNodes = newNodes.filter { visitedNodes.add(it) }
                if (newNodes.isEmpty()) {
                    println("No more new nodes. Closing search.")
                    break
                }

                newNodes = newNodes.map {
                    async {
                        println("${it.node.id.shortId()} query...")
                        val answer = withTimeoutOrNull(10.seconds) {
                            it.findValue(hash)
                        }
                        println("${it.node.id.shortId()} answer: $answer")
                        if (answer != null) {
                            val (value, nextNodes) = answer
                            if (value != null) {
                                println("FOUND VALUE: $value")
                                foundValue.complete(value)
                            }
                            nextNodes
                        } else {
                            emptyList()
                        }
                    }
                }.awaitAll().flatten().map {
                    addNode(it)
                }
            }
            foundValue.complete(null)
        }
        val value = foundValue.await()
        findJeb.cancel()
        return value
    }

    override suspend fun resolveAddress(id: AdnlNodeIdShort): AdnlAddressList? {
        val dhtValue = get(DhtKey(id.publicKeyHash, "address", 0)) ?: return null
        return AdnlAddressList(
            TL.Boxed.decodeFromByteString(TlAdnlAddressList.serializer(), dhtValue.value)
        )
    }

    inner class RemoteDhtNode(
        val node: DhtNode,
    ) : Comparable<RemoteDhtNode> {
        private val latencyHistory = arrayOfNulls<Duration>(5)
        private val latencyHistoryIndex = atomic(0)
        private val client by lazy {
            DhtTlClient(
                AdnlConnection(
                    transport,
                    localNode.key,
                    node.id.publicKey as Ed25519.PublicKey,
                    node.addressList
                )
            )
        }

        override fun compareTo(other: RemoteDhtNode): Int {
            return 0
        }

        suspend fun sendPing() {
            val latency = withTimeoutOrNull(10.seconds) {
                val ping = Clock.System.now().toEpochMilliseconds()
                val (pong, latency) = measureTimedValue {
                    client.ping(ping).randomId
                }
                check(ping == pong) {
                    "Invalid PONG: $pong, expected: $ping"
                }
                latency
            }
            if (latency != null) {
                latencyHistory[latencyHistoryIndex.getAndIncrement() % latencyHistory.size] = latency
            }
        }

        suspend fun findValue(hash: DhtKeyHash): Pair<DhtValue?, List<DhtNode>> {
            return when (val answer = client.findValue(hash, k)) {
                is DhtValueResult.ValueFound -> DhtValue(answer.value) to emptyList()
                is DhtValueResult.ValueNotFound -> null to answer.nodes.nodes.map { DhtNode(it) }
            }
        }

        override fun toString(): String = "RemoteDhtNode(node=$node)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RemoteDhtNode) return false
            if (node.id != other.node.id) return false
            return true
        }

        override fun hashCode(): Int = node.id.hashCode()
    }


//        val adnlConnection = object : AdnlConnection(transport, AdnlAddressList(dhtNode.addrList)) {
//            override val localNode: AdnlLocalNode = adnlLocalNode
//            override val remotePeer: AdnlPeer get() = AdnlPeer(PublicKey(dhtNode.id))
//
//            override suspend fun handleCustom(data: Source) {
//            }
//
//            override suspend fun handleQuery(queryId: AdnlQueryId, data: Source) {
//            }
//        }
//
//        override fun compareTo(other: RemoteDhtNode): Int = roundTripTime.compareTo(other.roundTripTime)
//
//        suspend fun sendStore(value: DhtValue): Boolean {
//            return true
//        }
//
//        suspend fun sendFindValue(hash: DhtKeyHash): Pair<DhtValue?, List<DhtNode>> {
//            val buffer = Buffer()
//            TL.Boxed.encodeToSink(DhtFindValue.serializer(), buffer, DhtFindValue(hash, k))
//            var (answer, time) = measureTimedValue {
//                withTimeoutOrNull(3.seconds) {
//                    adnlConnection.sendQuery(buffer)
//                }
//            }
//            roundTripTime = time
//            if (answer != null) {
//                val peekAnswer = answer.peek()
//                try {
//                    val newDhtNode = TL.Boxed.decodeFromSource(DhtNode.serializer(), peekAnswer)
//                    answer = peekAnswer
//                } catch (_: Exception) {
//                }
//                TL.Boxed.decodeFromSource(DhtValueResult.serializer(), answer)
//            }
//            return Pair(null, emptyList())
//        }
//
//        private fun demote() {
//
//        }
//
//        private fun Source.decodeValueResult(): Pair<DhtNode?, DhtValueResult> {
//            var source = this
//            val peek = source.peek()
//
//            var dhtNode: DhtNode? = null
//            try {
//                dhtNode = TL.Boxed.decodeFromSource(DhtNode.serializer(), peek)
//                source = peek
//            } catch (_: Exception) {
//            }
//            val valueResult = TL.Boxed.decodeFromSource(DhtValueResult.serializer(), source)
//            return Pair(dhtNode, valueResult)
//        }
}

package io.tonblocks.dht

import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.*
import io.tonblocks.kv.KeyValueRepository
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.io.bytestring.ByteString
import tl.ton.dht.DhtValueResult
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
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
    val repository: KeyValueRepository<DhtKeyHash, DhtValue>,
    override val k: Int = 10,
    override val a: Int = 3,
) : Dht {
    private val job = Job()
    override val coroutineContext: CoroutineContext = localNode.coroutineContext + job

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
        return beamSearch(hash)
    }

    suspend fun findNodes() {
        val key = Random.nextBytes(32)
        localNode.id.shortId().publicKeyHash.copyInto(
            key,
            endIndex = Random.nextInt(8)
        )
        findNodesQuery(ByteString(key))
    }

    private suspend fun beamSearch(key: DhtKeyHash): DhtValue? {
        var currentBeam = routingTable.nearest(key, k * 2)
        val visited = mutableSetOf<AdnlIdShort>()

        while (currentBeam.isNotEmpty()) {
            println("current beam: ${currentBeam.map { it.node.id.shortId() }}")

            val results = currentBeam.map {
                async {
                    withTimeoutOrNull(Random.nextInt(2000, 4000).milliseconds) {
                        it.findValue(key)
                    }
                }
            }.awaitAll().filterNotNull()

            val expanded = mutableSetOf<DhtNode>()
            for ((value, nodes) in results) {
                if (value != null) {
                    return value
                }
                for (node in nodes) {
                    if (visited.add(node.id.shortId())) {
                        expanded.add(node)
                    }
                }
            }

            println("visited: $visited")
            println("expanded: $expanded")

            currentBeam = expanded.asSequence()
                .map { addNode(it) }
                .sortedBy { it.node.id.shortId().publicKeyHash.xorDist(key) }
                .take(a)
                .toList()
            println("new beam: ${currentBeam.map { it.node.id.shortId() }}")
        }

        return null
    }

    private suspend fun findNodesQuery(
        key: ByteString
    ) = coroutineScope {
        val list = routingTable.nearest(key, k * 2)
        flow {
            var currentBeam = list
            val visited = mutableSetOf<AdnlIdShort>()

            while (currentBeam.isNotEmpty()) {
                println("current beam: ${currentBeam.map { it.node.id.shortId() }}")
                currentBeam.forEach {
                    emit(it)
                }
                val expanded = currentBeam.map {
                    async {
                        withTimeoutOrNull(Random.nextInt(2000, 4000).milliseconds) {
                            it.findNode(key)
                        } ?: emptyList()
                    }
                }.awaitAll().asSequence().flatten().distinctBy { it.id.shortId() }.filter {
                    it.id.shortId() !in visited
                }.toList()
                visited.addAll(expanded.map { it.id.shortId() })
                println("visited: $visited")
                println("expanded: $expanded")
                currentBeam = expanded.map { addNode(it) }
                    .sortedBy { it.node.id.shortId().publicKeyHash.xorDist(key) }
                    .take(a)
                    .toList()
                println("new beam: ${currentBeam.map { it.node.id.shortId() }}")
            }
        }.collect()
    }

    override suspend fun resolveAddress(id: AdnlIdShort): AdnlAddressList? {
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

        override fun compareTo(other: RemoteDhtNode): Int {
            return 0
        }

        suspend fun client() = DhtTlClient(localNode.connection(node.id, node.addressList))

        suspend fun sendPing() {
            val client = client()
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
            val client = client()
            return when (val answer = client.findValue(hash, k)) {
                is DhtValueResult.ValueFound -> DhtValue(answer.value) to emptyList()
                is DhtValueResult.ValueNotFound -> null to answer.nodes.nodes.map { DhtNode(it) }
            }
        }

        suspend fun findNode(hash: DhtKeyHash): List<DhtNode> {
            val client = client()
            return client.findNode(hash, k).nodes.map { DhtNode(it) }
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

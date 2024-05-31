package io.tonblocks.dht

import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.*
import io.tonblocks.kv.KeyValueRepository
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString
import tl.ton.dht.DhtValueResult
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
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

    private val pingJob = launch {
        while (true) {
            delay(5000)
            routingTable.forEach { node ->
                if ((Clock.System.now() - node.lastPingedAt) >= 1.minutes) {
                    node.sendPing()
                }
                delay(500)
            }
        }
    }

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
        return beamSearch(
            key = hash,
            query = { findValue(it) },
            condition = { (value, _) -> value != null && !value.isExpired() && value.isValid() },
            nextNodes = { (_, nodes) -> nodes }
        )?.first
    }

    suspend fun findNodes() {
        val key = Random.nextBytes(32)
        localNode.id.shortId().publicKeyHash.copyInto(
            key,
            endIndex = Random.nextInt(8)
        )
        beamSearch(
            key = ByteString(key),
            query = { findNode(it) },
            condition = { false },
            nextNodes = { it }
        )
    }

    override suspend fun resolveAddress(id: AdnlIdShort): AdnlAddressList? {
        val dhtValue = get(DhtKey(id.publicKeyHash, "address", 0)) ?: return null
        return AdnlAddressList(
            TL.Boxed.decodeFromByteString(TlAdnlAddressList.serializer(), dhtValue.value)
        )
    }

    private suspend fun <T : Any> beamSearch(
        key: DhtKeyHash,
        query: suspend RemoteDhtNode.(DhtKeyHash) -> T,
        condition: (T) -> Boolean,
        nextNodes: (T) -> List<DhtNode>
    ): T? {
        var currentBeam = routingTable.nearest(key, k * 2)
        val visited = mutableSetOf<AdnlIdShort>()
//        println("Look for key: ${key.toHexString()} ${key[0].toUByte().toString(2).padStart(8, '0')}")
        var foundResult: T? = null
        while (foundResult == null && currentBeam.isNotEmpty()) {
//            println("Beam iteration: ${currentBeam.size}\n${currentBeam.joinToString("\n") { "${it.xorDistance(key)} ${it.node.id.shortId()} | ${it.node.id.shortId().publicKeyHash[0].toUByte().toString(2).padStart(8, '0')}" }}")
            val expanded = mutableListOf<DhtNode>()
            channelFlow {
                currentBeam.forEach { node ->
                    launch {
                        val result = withTimeoutOrNull(Random.nextInt(2000, 4000).milliseconds) {
                            query(node, key)
                        }
                        if (result != null) {
                            send(result)
                        }
                    }
                }
            }.takeWhile { result ->
                if (condition(result)) {
                    foundResult = result
                    false
                } else {
                    val nodes = nextNodes(result)
                    for (node in nodes) {
                        if (visited.add(node.id.shortId())) {
                            expanded.add(node)
                        }
                    }
                    true
                }
            }.collect()
            if (foundResult != null) {
                break
            }

            currentBeam = expanded.asSequence()
                .map { addNode(it) }
                .sortedBy { it xorDistance key }
                .take(a * 2)
                .toList()
        }

        return foundResult
    }

    inner class RemoteDhtNode(
        val node: DhtNode,
    ) : Comparable<RemoteDhtNode> {
        private val latencyHistory = arrayOfNulls<Duration>(5)
        private val latencyHistoryIndex = atomic(0)
        var lastPingedAt by atomic(Instant.DISTANT_PAST)
            private set

        val averageLatency: Duration
            get() {
                var nonNull = 0
                var sum = Duration.ZERO
                for (duration in latencyHistory) {
                    if (duration != null) {
                        sum += duration
                        nonNull++
                    }
                }
                if (sum == Duration.ZERO) {
                    sum = INFINITE
                }
                return sum / nonNull
            }

        override fun compareTo(other: RemoteDhtNode): Int {
            return averageLatency.compareTo(other.averageLatency)
        }

        infix fun xorDistance(key: DhtKeyHash) = key xorDistance node.id.shortId().publicKeyHash

        suspend fun client() = DhtTlClient(localNode.connection(node.id, node.addressList))

        suspend fun sendPing() {
            val client = client()
            val now = Clock.System.now()
            val latency = withTimeoutOrNull(Random.nextInt(2000..4000).milliseconds) {
                val ping = now.toEpochMilliseconds()
                val (pong, latency) = measureTimedValue {
                    client.ping(ping).randomId
                }
                check(ping == pong) {
                    "Invalid PONG: $pong, expected: $ping"
                }
                latency
            }
            lastPingedAt = now
            latencyHistory[latencyHistoryIndex.getAndIncrement() % latencyHistory.size] = latency
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
}

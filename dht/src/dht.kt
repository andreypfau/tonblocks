package io.tonblocks.dht

import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.*
import io.tonblocks.adnl.query.AdnlQueryId
import io.tonblocks.adnl.transport.AdnlTransport
import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.ed25519.Ed25519
import io.tonblocks.crypto.ed25519.hash
import io.tonblocks.kv.KeyValueRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Instant
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import tl.ton.adnl.AdnlNode
import tl.ton.dht.*
import tl.ton.dht.Dht
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

/**
 * TON Distributed Hash Table interface
 */
interface Dht : AdnlAddressResolver {
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

class DhtTlClient(
    private val connection: AdnlConnection
) : TlDht {
    // TODO: fix DhtPing constructor id (expected: 0xCBEB3F18, actual: 0xCF6643AA)
    override suspend fun ping(randomId: Long): DhtPong {
        val buffer = Buffer()
        TL.Boxed.encodeToSink(DhtPing.serializer(), buffer, DhtPing(randomId))
        val answer = connection.sendQuery(buffer)
        return TL.Boxed.decodeFromSource(DhtPong.serializer(), answer)
    }

    // TODO: fix DhtStore constructor id (expected: 0x34934212)
    override suspend fun store(value: tl.ton.dht.DhtValue): DhtStored {
        val buffer = Buffer()
        TL.Boxed.encodeToSink(TlDhtStore.serializer(), buffer, TlDhtStore(value))
        val answer = connection.sendQuery(buffer)
        return TL.Boxed.decodeFromSource(DhtStored.serializer(), answer)
    }

    // TODO: fix DhtFindNodes constructor id (expected: 0x7974A0BE)
    override suspend fun findNode(key: ByteString, k: Int): TlDhtNodes {
        val buffer = Buffer()
        TL.Boxed.encodeToSink(TlDhtFindNode.serializer(), buffer, TlDhtFindNode(key, k))
        val answer = connection.sendQuery(buffer)
        return TL.Boxed.decodeFromSource(TlDhtNodes.serializer(), answer)
    }

    // TODO: fix DhtFindValue constructor id (expected: 0xAE4B6011)
    override suspend fun findValue(key: ByteString, k: Int): TlDhtValueResult {
        val buffer = Buffer()
        TL.Boxed.encodeToSink(TlDhtFindValue.serializer(), buffer, TlDhtFindValue(key, k))
        val answer = connection.sendQuery(buffer)
        println(answer)
        // TODO: fix deserialize polymorphic using TL.Boxed
        return TL.decodeFromSource(TlDhtValueResult.serializer(), answer)
    }

    override suspend fun getSignedAddressList(): DhtNode {
        TODO("Not yet implemented")
    }

    override suspend fun registerReverseConnection(node: tl.ton.PublicKey, ttl: Int, signature: ByteString): DhtStored {
        TODO("Not yet implemented")
    }

    override suspend fun requestReversePing(
        target: AdnlNode,
        signature: ByteString,
        client: ByteString,
        k: Int
    ): DhtReversePingResult {
        TODO("Not yet implemented")
    }
}

//class DhtImpl(
//    val adnlLocalNode: AdnlLocalNode,
//    val transport: AdnlTransport,
//    val repository: KeyValueRepository<DhtKeyHash, DhtValue>,
//    override val k: Int = 10,
//    override val a: Int = 3
//) : Dht {
//    val keyId = adnlLocalNode.key.hash()
//    val routingTable = KademliaRoutingTable<RemoteDhtNode>(
//        keyId,
//        k,
//        { it.dhtNode.id.hash() }
//    )
//
//    override suspend fun set(value: DhtValue) {
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
//    }
//
//    override suspend fun get(hash: DhtKeyHash): DhtValue? {
//        val storedValue = repository.get(hash)
//        if (storedValue != null) {
//            if (storedValue.isExpired()) {
//                repository.remove(hash)
//            } else {
//                return storedValue
//            }
//        }
//
//        return coroutineScope {
//            val activeNodes = routingTable.nearest(hash, k)
//
//            val nodes = Channel<RemoteDhtNode>()
//            val responses = Channel<Pair<DhtValue?, List<DhtNode>>>()
//
//            val semaphore = Semaphore(a)
//            val findValueJob = launch {
//                while (true) {
//                    val node = nodes.receive()
//                    launch {
//                        semaphore.withPermit {
//                            responses.send(node.sendFindValue(hash))
//                        }
//                    }
//                }
//            }
//
//            val visitedNodes = mutableSetOf<RemoteDhtNode>()
//            visitedNodes.addAll(activeNodes)
//            var foundValue: DhtValue? = null
//            while (foundValue == null) {
//                val (value, nextNodes) = responses.receive()
//                if (value != null) {
//                    foundValue = value
//                }
//                var addedNewNodes = false
//                nextNodes.forEach {
//                    val connectedNode = tryToAddNode(it)
//                    if (connectedNode != null && visitedNodes.add(connectedNode)) {
//                        addedNewNodes = true
//                        nodes.send(connectedNode)
//                    }
//                }
//                if (!addedNewNodes) {
//                    break
//                }
//            }
//            findValueJob.cancel()
//            nodes.close()
//            foundValue
//        }
//    }
//
//    private fun tryToAddNode(dhtNode: DhtNode): RemoteDhtNode? {
//        return null
//    }
//
//    override suspend fun resolveAddress(id: AdnlNodeIdShort): AdnlAddressList? {
//        val dhtValue = get(DhtKey(id.publicKeyHash, "address", 0)) ?: return null
//        return AdnlAddressList(
//            TL.Boxed.decodeFromByteString(TlAdnlAddressList.serializer(), dhtValue.value)
//        )
//    }
//
//    inner class RemoteDhtNode(
//        var dhtNode: DhtNode,
//        var roundTripTime: Duration
//    ) : Comparable<RemoteDhtNode> {
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
//    }
//}

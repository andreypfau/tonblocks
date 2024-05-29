package io.tonblocks.dht

import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.AdnlAddressList
import io.tonblocks.adnl.AdnlConnection
import io.tonblocks.adnl.AdnlNodeIdFull
import io.tonblocks.adnl.tl
import kotlinx.datetime.Instant
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import tl.ton.adnl.AdnlNode
import tl.ton.dht.DhtPing
import tl.ton.dht.DhtPong
import tl.ton.dht.DhtReversePingResult
import tl.ton.dht.DhtStored

typealias TlDhtKey = tl.ton.dht.DhtKey
typealias TlDhtKeyDescription = tl.ton.dht.DhtKeyDescription
typealias TlDhtNode = tl.ton.dht.DhtNode
typealias TlDhtUpdateRule = tl.ton.dht.DhtUpdateRule
typealias TlDht = tl.ton.dht.Dht
typealias TlDhtFindNode = tl.ton.dht.DhtFindNode
typealias TlDhtNodes = tl.ton.dht.DhtNodes
typealias TlDhtFindValue = tl.ton.dht.DhtFindValue
typealias TlDhtValueResult = tl.ton.dht.DhtValueResult
typealias TlDhtStore = tl.ton.dht.DhtStore
typealias TlDhtValue = tl.ton.dht.DhtValue

fun DhtNode(tl: TlDhtNode): DhtNode = DhtNode(
    id = AdnlNodeIdFull(tl.id),
    version = tl.version,
    addressList = AdnlAddressList(tl.addrList),
    signature = tl.signature
)

fun DhtNode.tl(): TlDhtNode = TlDhtNode(
    id = id.publicKey.tl(),
    addrList = addressList.tl(),
    version = version,
    signature = signature
)

fun DhtValue(tl: TlDhtValue): DhtValue = DhtValue(
    description = DhtKeyDescription(tl.key),
    value = tl.value,
    ttl = Instant.fromEpochSeconds(tl.ttl.toLong()),
    signature = tl.signature
)

class DhtTlClient(
    val connection: AdnlConnection
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

    override suspend fun getSignedAddressList(): TlDhtNode {
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

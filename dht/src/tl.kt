package io.tonblocks.dht

import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.AdnlAddressList
import io.tonblocks.adnl.AdnlConnection
import io.tonblocks.adnl.AdnlIdFull
import io.tonblocks.adnl.tl
import kotlinx.datetime.Instant
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteArray
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
    id = AdnlIdFull(tl.id),
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
    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun findNode(key: ByteString, k: Int): TlDhtNodes {
        val buffer = Buffer()
        TL.Boxed.encodeToSink(TlDhtFindNode.serializer(), buffer, TlDhtFindNode(key, k))
        val answer = connection.sendQuery(buffer).readByteArray()
        return try {
            TL.Boxed.decodeFromByteArray(TlDhtNodes.serializer(), answer)
        } catch (e: Throwable) {
            throw IllegalStateException("Can't parse answer: ${answer.toHexString()}", e)
        }
    }
    // bea07479 // nodes
    // 0a000000c6b4134866cae7b6f9f088156105e6f421eb42ae8313744669b1ab49e3178e7c8f926ddd01000000e7a60d67243bb040eea500001e941a661e941a660000000000000000cdae5866408546fc74a8bbb89e7395e290a2058dc921f6fec3a03a2818528b8ddbfc50df5a5c692861c438c0b322e01c1d7b570ca4f83c598736eb572ee2d94faa91b25104000000c6b413484511ffa25cabb4bb06dd33aa3ad9dcf629cf1370d9c422078f6ddb08bd7a20fc01000000e7a60d67ee94d95f9e18000048885466488854660000000000000000cdae58664061cd8e3c1a70646fe8e10d144793c8e2928e830cd7e3075eba370025d476dcb3f88e7f80293c73047b9a7e147598846298997d5675aad157119c40bb6d355b06000000c6b413481262f15725a2cda33ee91f535590bcd10110ef47924d8d64cebe1697d5b0377a01000000e7a60d67ac48f388ea700000c82e5766c82e57660000000000000000d6ae586640aa63ee2537f6c78caa5312e5b726fc4228b98c7e4e147d927e65d05c50fe6365b4e2fb57304a1def8b8d6b37dff0b87da1b734fdad3f24828c3d69480cac1a0d000000c6b41348aa6bf7c2c9487e6feac7e891d77f17932373b2b251fb8748555926cee554762a01000000e7a60d67ba0ade33a154000048055866480558660000000000000000d1ae58664024514662098a0fa5c755b4656ebe2da697df841545f464febad183af65df949938c82467311c0d8e91d5e696c3fb3ccc26d88d7c76436004e3d5b48c0b8b1009000000c6b4134884d8e63fcae4c2179745949a916306ecf2624f1f00c31e00d3b2aa8330f9b44d01000000e7a60d67844b144192b50000adf90d64adf90d640000000000000000e0ae5866407fb3d3cab86b3237130b3e4107b3006d5ce295e20acd12fa0e5abff7178d26f986ce116274ac13d75c599663828029dfc78dc98181205698f2dc5d6b9fd59e09000000c6b413489f3c313cd79ca41958b2ffd0d3c75c094efdd284944e619996e06a8cc13d9c5f01000000e7a60d67b2ee6c4124540000d40a4b66d40a4b660000000000000000daae5866400dbe58dc815121e59d5f87472355def10855bbe5cc94415b517acc639491993a70eb6e457f402065fab608154300d97413a68a937378add64e9a070e47820b0d000000c6b4134820f8a5d43a150a62a8753d226b296c88da8452571a531b28a8c855e107b5d33201000000e7a60d67e25f77a885840000d6f65466d6f654660000000000000000d3ae586640fd92341a820fb79f991fbc53725b9ac4438a4d11556d4e9559b8aaef3caedf6a5f26b06a770a7e2123da818d2351dd1469f3ed837d5e996cac2b7a1dc70b290b000000c6b413485833b0bf00c8177a6bd3444c39f2575d532f93c28a4dde8c6ca94d13aaac434101000000e7a60d6769513b92121800008dc157668dc157660000000000000000ddae58664040fc1947f95dd9f319105dff35926297e1a4eb1214d92757a2dd5b796fa83e3238127ac801f26f8f1837a369c77f4a712bf52f3871cbe946a32304681aa5f309000000c6b4134808e1e767e20d778cf3daf57651e18b0aa706d8dd25dbbe0af894c1825e9ad6bd01000000e7a60d671042892e32750000619e2666619e26660000000000000000d2ae5866400fb33a36af0264ad0c967a908c6bd87b181bd14e9ae4f811d401e8fa9ae75e8869442ef6b8716e01fe08a19fdf1c086948daa1a7f82a90f7251b44c03ecf8205000000c6b4134866a9d27d25571829cead5e017aed1fa85660e1d161e4fe901e4d950a8bcdc49d01000000e7a60d67316bdb4371fd00009faf57669faf57660000000000000000b2ae586640e8293f868d09d40cf98a0c631035f86d2bdc222763c7e778d0b57760f5ea511a9d0faaa18e2d726c0ffa2159a5a6320b2c107e4e2302584a0dd369dc2d20220d000000

    // 74f70ce4 // valueFound
    // cb27ad90
    // eff34526dd05e93e4f4520d1ca63db279fc06432d43fb01161c85c287c2dd950076164647265737300000000c6b4134861c7df97bacdf2ff933324dfab5853c2951f9966009df6f9e7f3bba22fec03a7f7319fcc407cc6db46c5b3bccec0de385094b888b42d21037fdaeeaa999119ef818815b3f605c7a9974f78a40330925f4772d7e2f5e52363c2fd30f2d30134262b32bf59090000002458e6272201000000e7a60d677994c3335f760000698558666c4e4c660000000000000000000000799358664088f7b7dceaca4343e1ef645583f97ebc8500258ba5cf47a86d85ddad5dbcd2ac59585ba48c2e3170342078ce3d4a91c3a4a4b355b5f9b9afa29aec14ab849f09000000

    // TODO: fix DhtFindValue constructor id (expected: 0xAE4B6011)
    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun findValue(key: ByteString, k: Int): TlDhtValueResult {
        val buffer = Buffer()
        TL.Boxed.encodeToSink(TlDhtFindValue.serializer(), buffer, TlDhtFindValue(key, k))
        val answer = connection.sendQuery(buffer).readByteArray()

        // TODO: fix deserialize polymorphic using TL.Boxed
        return try {
            TL.decodeFromByteArray(TlDhtValueResult.serializer(), answer)
        } catch (e: Exception) {
            throw IllegalStateException("Can't parse answer: ${answer.toHexString()}", e)
        }
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

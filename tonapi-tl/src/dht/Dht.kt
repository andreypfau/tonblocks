package tl.ton.dht

import kotlinx.io.bytestring.ByteString
import tl.ton.PublicKey
import tl.ton.adnl.AdnlNode

interface Dht {
    suspend fun ping(randomId: Long): DhtPong
    suspend fun store(value: DhtValue): DhtStored
    suspend fun findNode(key: ByteString, k: Int): DhtNodes
    suspend fun findValue(key: ByteString, k: Int): DhtValueResult
    suspend fun getSignedAddressList(): DhtNode
    suspend fun registerReverseConnection(node: PublicKey, ttl: Int, signature: ByteString): DhtStored
    suspend fun requestReversePing(target: AdnlNode, signature: ByteString, client: ByteString, k: Int): DhtReversePingResult
}

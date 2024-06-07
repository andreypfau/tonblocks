package tl.ton

import kotlinx.io.bytestring.ByteString

interface Dht {
    suspend fun ping(randomId: Long): DhtPong
    suspend fun store(value: DhtValue): DhtStored
    suspend fun findNode(key: ByteString, k: Int): DhtNodes
    suspend fun findValue(key: ByteString, k: Int): DhtValueResult
    suspend fun getSignedAddressList(): DhtNode
    suspend fun registerReverseConnection(node: PublicKey, ttl: Int, signature: ByteString): DhtStored
    suspend fun requestReversePing(target: AdnlNode, signature: ByteString, client: ByteString, k: Int): DhtReversePingResult
}

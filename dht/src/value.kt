package io.tonblocks.dht

import io.tonblocks.adnl.AdnlAddressList
import io.tonblocks.adnl.AdnlIdFull
import io.tonblocks.crypto.PublicKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString

data class DhtValue(
    val description: DhtKeyDescription,
    val value: ByteString,
    val ttl: Instant,
    val signature: ByteString
) {
    fun isExpired(now: Instant = Clock.System.now()): Boolean = ttl < now
}

data class DhtNode(
    val id: AdnlIdFull,
    val addressList: AdnlAddressList,
    val version: Int = 0,
    val signature: ByteString = ByteString()
) {
    constructor(
        publicKey: PublicKey,
        addressList: AdnlAddressList,
        version: Int = 0,
        signature: ByteString = ByteString()
    ) : this(AdnlIdFull(publicKey), addressList, version, signature)

    val publicKey: PublicKey get() = id.publicKey

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        return "DhtNode(publicKey=$publicKey, addressList=$addressList, version=$version, signature=${signature.toHexString()})"
    }
}

package io.tonblocks.dht

import io.tonblocks.adnl.AdnlAddressList
import io.tonblocks.adnl.AdnlNodeIdFull
import io.tonblocks.crypto.PublicKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString

data class DhtValue(
    val description: DhtKeyDescription,
    val value: ByteString,
    val ttl: Instant,
    val signature: ByteString
) {
    fun isExpired(now: Instant = Clock.System.now()): Boolean = ttl < now
}

data class DhtNode(
    val id: AdnlNodeIdFull,
    val version: Int,
    val addressList: AdnlAddressList,
    val signature: ByteString
) {
    constructor(
        id: PublicKey,
        version: Int,
        addressList: AdnlAddressList,
        signature: ByteString
    ) : this(AdnlNodeIdFull(id), version, addressList, signature)
}

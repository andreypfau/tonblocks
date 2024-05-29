package io.tonblocks.adnl

import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.PublicKeyHash
import kotlinx.io.bytestring.toHexString
import tl.ton.adnl.id.AdnlIdShort

data class AdnlNodeIdShort(
    val publicKeyHash: PublicKeyHash
) : Comparable<AdnlNodeIdShort> {
    constructor(tl: AdnlIdShort) : this(tl.id)
    constructor(publicKey: PublicKey) : this(publicKey.hash())

    override fun compareTo(other: AdnlNodeIdShort): Int {
        return publicKeyHash.compareTo(other.publicKeyHash)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = publicKeyHash.toHexString()

    fun tl(): AdnlIdShort = AdnlIdShort(publicKeyHash)
}

data class AdnlNodeIdFull(
    val publicKey: PublicKey
) {
    constructor(tl: tl.ton.PublicKey) : this(PublicKey(tl))

    private val shortId by lazy {
        AdnlNodeIdShort(publicKey.hash())
    }

    fun shortId(): AdnlNodeIdShort = shortId

    override fun hashCode(): Int = shortId().hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdnlNodeIdFull) return false
        return shortId() == other.shortId()
    }
}

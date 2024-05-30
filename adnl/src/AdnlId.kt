package io.tonblocks.adnl

import io.ktor.util.*
import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.PublicKeyHash
import kotlinx.io.bytestring.toHexString
import tl.ton.adnl.id.AdnlIdShort

data class AdnlIdShort(
    val publicKeyHash: PublicKeyHash
) : Comparable<io.tonblocks.adnl.AdnlIdShort> {
    constructor(tl: AdnlIdShort) : this(tl.id)
    constructor(publicKey: PublicKey) : this(publicKey.hash())

    override fun compareTo(other: io.tonblocks.adnl.AdnlIdShort): Int {
        return publicKeyHash.compareTo(other.publicKeyHash)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = publicKeyHash.toHexString()

    fun tl(): AdnlIdShort = AdnlIdShort(publicKeyHash)

    fun base64() = publicKeyHash.toByteArray().encodeBase64()
}

data class AdnlIdFull(
    val publicKey: PublicKey
) {
    constructor(tl: tl.ton.PublicKey) : this(PublicKey(tl))

    private val shortId by lazy {
        io.tonblocks.adnl.AdnlIdShort(publicKey.hash())
    }

    fun shortId(): io.tonblocks.adnl.AdnlIdShort = shortId

    override fun hashCode(): Int = shortId().hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdnlIdFull) return false
        return shortId() == other.shortId()
    }

    override fun toString(): String {
        return "AdnlIdFull($publicKey)"
    }
}

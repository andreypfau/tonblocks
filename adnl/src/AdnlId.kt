package io.tonblocks.adnl

import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.PublicKeyHash
import io.tonblocks.crypto.ShortId
import kotlinx.io.bytestring.toHexString

data class AdnlIdShort(
    val publicKeyHash: PublicKeyHash
) : ShortId<AdnlIdShort> {
    constructor(tl: tl.ton.adnl.id.AdnlIdShort) : this(tl.id)
    constructor(publicKey: PublicKey) : this(publicKey.hash())
    constructor(shortId: ShortId<AdnlIdShort>) : this(shortId.shortId().publicKeyHash)

    override fun compareTo(other: AdnlIdShort): Int {
        return publicKeyHash.compareTo(other.publicKeyHash)
    }

    override fun shortId(): AdnlIdShort = this

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = publicKeyHash.toHexString()

    fun tl(): tl.ton.adnl.id.AdnlIdShort = tl.ton.adnl.id.AdnlIdShort(publicKeyHash)
}

data class AdnlIdFull(
    val publicKey: PublicKey
) : ShortId<AdnlIdShort> {
    constructor(tl: tl.ton.PublicKey) : this(PublicKey(tl))

    private val shortId by lazy {
        AdnlIdShort(publicKey.hash())
    }

    override fun shortId(): AdnlIdShort = shortId

    override fun compareTo(other: AdnlIdShort): Int = shortId.compareTo(other)

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

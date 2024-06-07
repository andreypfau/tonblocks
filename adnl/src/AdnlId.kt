package io.tonblocks.adnl

import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.ShortId
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString

class AdnlIdShort(
    val publicKeyHash: ByteString
) : ShortId<AdnlIdShort> {
    constructor(byteArray: ByteArray, startIndex: Int = 0) : this(ByteString(byteArray, startIndex, startIndex + 32))
    constructor(tl: tl.ton.AdnlIdShort) : this(tl.id)
    constructor(publicKey: PublicKey) : this(publicKey.hash())
    constructor(shortId: ShortId<AdnlIdShort>) : this(shortId.shortId().publicKeyHash)

    init {
        require(publicKeyHash.size == 32) { "Invalid id size: ${publicKeyHash.size} != 32 bytes" }
    }

    override fun shortId(): AdnlIdShort = this

    fun tl(): tl.ton.AdnlIdShort = tl.ton.AdnlIdShort(publicKeyHash)

    override fun compareTo(other: ShortId<AdnlIdShort>): Int = publicKeyHash.compareTo(other.shortId().publicKeyHash)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = publicKeyHash.toHexString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdnlIdShort) return false
        return publicKeyHash == other.publicKeyHash
    }

    override fun hashCode(): Int = publicKeyHash.hashCode()
}

class AdnlIdFull(
    val publicKey: PublicKey
) : ShortId<AdnlIdShort> {
    constructor(tl: tl.ton.PublicKey) : this(PublicKey(tl))

    private val shortId by lazy {
        AdnlIdShort(publicKey.hash())
    }

    override fun shortId(): AdnlIdShort = shortId

    override fun compareTo(other: ShortId<AdnlIdShort>): Int = shortId.compareTo(other.shortId())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdnlIdFull) return false
        return shortId() == other.shortId()
    }

    override fun hashCode(): Int = shortId().hashCode()

    override fun toString(): String {
        return "AdnlIdFull($publicKey)"
    }
}

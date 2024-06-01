package io.tonblocks.overlay

import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.ShortId
import io.tonblocks.crypto.ed25519.hash
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString

class OverlayIdShort(
    val publicKeyHash: ByteString
) : ShortId<OverlayIdShort> {
    constructor(byteArray: ByteArray, startIndex: Int = 0) : this(ByteString(byteArray, startIndex, startIndex + 32))
    constructor(publicKey: PublicKey) : this(publicKey.hash())
    constructor(shortId: ShortId<OverlayIdShort>) : this(shortId.shortId().publicKeyHash)

    init {
        require(publicKeyHash.size == 32) { "Invalid public key hash size: ${publicKeyHash.size}, expected 32 bytes" }
    }

    fun tl(): OverlayIdShort = OverlayIdShort(publicKeyHash)

    override fun shortId(): OverlayIdShort = this

    override fun compareTo(other: ShortId<OverlayIdShort>): Int = publicKeyHash.compareTo(other.shortId().publicKeyHash)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = publicKeyHash.toHexString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverlayIdShort) return false
        return publicKeyHash == other.publicKeyHash
    }

    override fun hashCode(): Int = publicKeyHash.hashCode()
}

data class OverlayIdFull(
    val name: ByteString
) : ShortId<OverlayIdShort> {
    constructor(name: ByteArray) : this(ByteString(name))

    private val shortId by lazy {
        OverlayIdShort(
            tl.ton.PublicKey.Overlay(name).hash()
        )
    }

    override fun shortId(): OverlayIdShort = shortId

    override fun compareTo(other: ShortId<OverlayIdShort>): Int = shortId.compareTo(other.shortId())

    override fun toString(): String = "OverlayIdFull($name)"

    override fun hashCode(): Int = shortId.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverlayIdFull) return false
        return shortId() == other.shortId()
    }
}

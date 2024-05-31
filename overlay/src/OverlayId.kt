package io.tonblocks.overlay

import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.PublicKeyHash
import io.tonblocks.crypto.ShortId
import io.tonblocks.crypto.ed25519.hash
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString

data class OverlayIdShort(
    val publicKeyHash: PublicKeyHash
) : ShortId<OverlayIdShort> {
    constructor(publicKey: PublicKey) : this(publicKey.hash())
    constructor(shortId: ShortId<OverlayIdShort>) : this(shortId.shortId().publicKeyHash)

    override fun compareTo(other: OverlayIdShort): Int {
        return publicKeyHash.compareTo(other.publicKeyHash)
    }

    override fun shortId(): OverlayIdShort = this

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = publicKeyHash.toHexString()

    fun tl(): OverlayIdShort = OverlayIdShort(publicKeyHash)
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

    override fun compareTo(other: OverlayIdShort): Int {
        return shortId.compareTo(other)
    }

    override fun hashCode(): Int = shortId().hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverlayIdFull) return false
        return shortId() == other.shortId()
    }

    override fun toString(): String {
        return "OverlayIdFull($name)"
    }
}

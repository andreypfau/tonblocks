package io.tonblocks.overlay.broadcast

import io.github.andreypfau.kotlinx.crypto.sha256
import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.AdnlIdFull
import io.tonblocks.crypto.ShortId
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString
import kotlinx.serialization.encodeToByteArray
import tl.ton.OverlayBroadcastId

class BroadcastIdShort(
    val hash: ByteString
) : ShortId<BroadcastIdShort> {
    override fun compareTo(other: ShortId<BroadcastIdShort>): Int {
        return hash.compareTo(other.shortId().hash)
    }

    override fun shortId(): BroadcastIdShort {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BroadcastIdShort) return false
        if (hash != other.hash) return false
        return true
    }

    override fun hashCode(): Int = hash.hashCode()

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = hash.toHexString()
}

class BroadcastIdFull(
    val source: AdnlIdFull,
    val dataHash: ByteString,
    val flags: Int
) : ShortId<BroadcastIdShort> {
    private val shortId: BroadcastIdShort by lazy {
        BroadcastIdShort(ByteString(*sha256(TL.Boxed.encodeToByteArray(tl()))))
    }

    fun tl(): OverlayBroadcastId = OverlayBroadcastId(
        source.shortId().publicKeyHash,
        dataHash,
        flags
    )

    override fun compareTo(other: ShortId<BroadcastIdShort>): Int {
        return shortId.compareTo(other)
    }

    override fun shortId(): BroadcastIdShort = shortId

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BroadcastIdFull) return false
        if (source != other.source) return false
        if (dataHash != other.dataHash) return false
        if (flags != other.flags) return false
        return true
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + dataHash.hashCode()
        result = 31 * result + flags
        return result
    }
}

package io.tonblocks.overlay.broadcast

import io.tonblocks.adnl.AdnlIdShort
import io.tonblocks.crypto.ShortId
import io.tonblocks.overlay.OverlayCertificate
import io.tonblocks.overlay.TlOverlayBroadcast
import io.tonblocks.overlay.TlOverlayBroadcastSimple
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString

abstract class Broadcast {
    abstract fun addDelivered(id: ShortId<AdnlIdShort>): Boolean

    abstract fun hasDelivered(id: ShortId<AdnlIdShort>): Boolean

    abstract fun tl(): TlOverlayBroadcast
}

class BroadcastSimple private constructor(
    private var tl: TlOverlayBroadcastSimple?,
    val id: BroadcastIdFull,
    val date: Instant,
    val certificate: OverlayCertificate,
    val flags: Int,
    val data: ByteString,
    val signature: ByteString
) : Broadcast() {
    constructor(
        tl: TlOverlayBroadcastSimple,
        id: BroadcastIdFull,
        date: Instant = Instant.fromEpochSeconds(tl.date.toLong()),
    ) : this(tl, id, date, OverlayCertificate(tl.certificate), tl.flags, tl.data, tl.signature)

    private val delivered = HashSet<AdnlIdShort>()

    init {
        delivered.add(id.source.shortId())
    }

    override fun addDelivered(id: ShortId<AdnlIdShort>): Boolean {
        return delivered.add(id.shortId())
    }

    override fun hasDelivered(id: ShortId<AdnlIdShort>): Boolean = delivered.contains(id.shortId())

    override fun tl(): TlOverlayBroadcastSimple {
        return tl ?: TlOverlayBroadcastSimple(
            src = id.source.publicKey.tl(),
            certificate = certificate.tl(),
            flags = flags,
            data = data,
            date = date.epochSeconds.toInt(),
            signature = signature
        ).also {
            tl = it
        }
    }
}

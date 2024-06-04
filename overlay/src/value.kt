package io.tonblocks.overlay

import io.tonblocks.adnl.AdnlIdFull
import kotlinx.io.bytestring.ByteString

data class OverlayNode(
    val source: AdnlIdFull,
    val overlayId: OverlayIdShort,
    val version: Int,
    val signature: ByteString
) {
    constructor(tl: tl.ton.overlay.OverlayNode) : this(
        source = AdnlIdFull(tl.id),
        overlayId = OverlayIdShort(tl.overlay),
        version = tl.version,
        signature = tl.signature
    )

    fun tl(): tl.ton.overlay.OverlayNode = tl.ton.overlay.OverlayNode(
        id = source.publicKey.tl(),
        overlay = overlayId.publicKeyHash,
        version = version,
        signature = signature
    )
}

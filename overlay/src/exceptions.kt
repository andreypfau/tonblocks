package io.tonblocks.overlay

import io.tonblocks.adnl.AdnlIdShort
import io.tonblocks.crypto.ShortId

class UnknownOverlayMember(
    val id: ShortId<OverlayIdShort>,
    val dest: ShortId<AdnlIdShort>
) : RuntimeException("Unknown local member overlay ${id.shortId()}@${dest.shortId()}")

class UnknownOverlay(
    val id: ShortId<OverlayIdShort>,
    val dest: ShortId<AdnlIdShort>
) : RuntimeException("Unknown overlay ${id.shortId()}@${dest.shortId()}")

package io.tonblocks.overlay

import io.github.andreypfau.tl.serialization.TL
import io.github.andreypfau.tl.serialization.encodeToSink
import io.tonblocks.adnl.AdnlClient
import io.tonblocks.adnl.AdnlConnection
import kotlinx.io.Buffer
import kotlinx.io.Source
import tl.ton.OverlayMessage
import tl.ton.OverlayQuery

class OverlayConnection(
    val overlay: Overlay,
    val adnl: AdnlConnection
) : AdnlClient {
    override suspend fun sendQuery(data: Source): Source {
        val buffer = Buffer()
        TL.Boxed.encodeToSink(buffer, OverlayQuery(overlay.id.shortId().publicKeyHash))
        data.transferTo(buffer)
        return adnl.sendQuery(buffer)
    }

    override suspend fun sendMessage(data: Source) {
        val buffer = Buffer()
        TL.encodeToSink(buffer, OverlayMessage(overlay.id.shortId().publicKeyHash))
        data.transferTo(buffer)
        adnl.sendQuery(buffer)
    }
}

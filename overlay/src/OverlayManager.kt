package io.tonblocks.overlay

import io.github.andreypfau.tl.serialization.TL
import io.github.andreypfau.tl.serialization.decodeFromSource
import io.tonblocks.adnl.AdnlLocalNode
import io.tonblocks.utils.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

class OverlayLocalNode(
    val adnl: AdnlLocalNode,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = adnl.coroutineContext
    private val overlays = ConcurrentHashMap<OverlayIdShort, Overlay>()

    init {
        adnl.subscribeMessage { rawMessage ->
            val message = TL.Boxed.decodeFromSource<TlOverlayMessage>(rawMessage)
            val overlayId = OverlayIdShort(message.overlay)
            val overlay =
                overlays[overlayId] ?: throw IllegalStateException("Unknown overlay ${adnl.id.shortId()}@$overlayId")
            val connection = overlay.connection(this)
            overlay.receiveMessage(connection, rawMessage)
        }
        adnl.subscribeQuery { rawQuery, rawAnswer ->
            val query = TL.Boxed.decodeFromSource<TlOverlayQuery>(rawQuery)
            val overlayId = OverlayIdShort(query.overlay)
            val overlay =
                overlays[overlayId] ?: throw IllegalStateException("Unknown overlay ${adnl.id.shortId()}@$overlayId")
            val connection = overlay.connection(this)
            overlay.receiveQuery(connection, rawQuery, rawAnswer)
        }
    }

    fun register(overlay: Overlay) {
        overlays.getOrPut(overlay.id.shortId()) {
            overlay
        }
    }

    fun overlay(
        id: OverlayIdFull,
        isPublic: Boolean,
        nodes: List<OverlayNode> = emptyList(),
        builder: (Overlay).() -> Unit
    ): Overlay {
        val overlay = OverlayImpl(this, id, isPublic, nodes).apply(builder)
        register(overlay)
        return overlay
    }
}

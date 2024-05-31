package io.tonblocks.overlay

import io.tonblocks.adnl.AdnlLocalNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

interface Overlay : CoroutineScope {
    val id: OverlayIdFull

    fun addPeer(node: OverlayNode)
    fun randomPeers(maxPeers: Int)
}

class OverlayImpl(
    val localNode: AdnlLocalNode,
    override val id: OverlayIdFull,
    coroutineContext: CoroutineContext
) : Overlay {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = coroutineContext + job

    override fun addPeer(node: OverlayNode) {
        if (node.overlayId != id.shortId()) {
            return
        }
        if (localNode.id.shortId() == node.source.shortId()) {
            return
        }
    }

    override fun randomPeers(maxPeers: Int) {
        TODO("Not yet implemented")
    }
}

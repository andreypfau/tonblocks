package io.tonblocks.overlay

import io.tonblocks.crypto.ShortId

interface OverlayNodesResolver {
    suspend fun resolveNodes(id: ShortId<OverlayIdShort>): List<OverlayNode>?
}

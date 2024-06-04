package io.tonblocks.overlay

import io.tonblocks.adnl.AdnlClient
import io.tonblocks.adnl.sendTlQuery
import kotlinx.io.bytestring.ByteString
import tl.ton.overlay.OverlayBroadcast
import tl.ton.overlay.OverlayBroadcastList
import tl.ton.overlay.OverlayGetBroadcast
import tl.ton.overlay.OverlayGetBroadcastList
import tl.ton.overlay.OverlayGetRandomPeers
import tl.ton.overlay.OverlayNodes

class OverlayClient(
    private val adnlClient: AdnlClient
) {
    suspend fun getRandomPeers(peers: OverlayNodes): OverlayNodes {
        return adnlClient.sendTlQuery(OverlayGetRandomPeers(peers))
    }

    suspend fun getBroadcast(hash: ByteString): OverlayBroadcast {
        return adnlClient.sendTlQuery(OverlayGetBroadcast(hash))
    }

    suspend fun getBroadcastList(list: OverlayBroadcastList): OverlayBroadcastList {
        return adnlClient.sendTlQuery(OverlayGetBroadcastList(list))
    }
}

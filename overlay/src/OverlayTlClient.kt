package io.tonblocks.overlay

import io.tonblocks.adnl.AdnlClient
import io.tonblocks.adnl.sendTlQuery
import kotlinx.io.bytestring.ByteString
import tl.ton.*

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

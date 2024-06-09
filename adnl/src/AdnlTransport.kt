package io.tonblocks.adnl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

interface AdnlTransport : CoroutineScope {
    suspend fun send(packet: AdnlPacket)

    suspend fun receive(): AdnlPacket
}

class AdnlNodeTransport(
    private val peerPair: AdnlPeerPair
) : AdnlTransport {
    private val transportJob = Job()
    override val coroutineContext: CoroutineContext = peerPair.coroutineContext + transportJob

    override suspend fun send(packet: AdnlPacket) {
        TODO("Not yet implemented")
    }

    override suspend fun receive(): AdnlPacket {
        TODO("Not yet implemented")
    }
}

package io.tonblocks.adnl.transport

import io.ktor.utils.io.core.*
import io.tonblocks.adnl.AdnlAddress
import io.tonblocks.adnl.AdnlIdShort
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class LoopbackAdnlTransport : AdnlNetworkTransport {
    override val reinitDate: Instant = Clock.System.now()
    private val allowedSources = mutableSetOf<AdnlIdShort>()
    private val allowedDestinations = mutableSetOf<AdnlIdShort>()
    private val handlers = mutableListOf<AdnlTransportHandler>()
    override val coroutineContext: CoroutineContext = Dispatchers.Default

    var lossPortability = 0.0
        set(value) {
            check(value in 0.0..1.0) {
                "lossPortability should be in range 0.0..1.0"
            }
            field = value
        }

    fun addNodeId(id: AdnlIdShort, allowSend: Boolean = true, allowReceive: Boolean = true) {
        if (allowSend) {
            allowedSources.add(id)
        } else {
            allowedSources.remove(id)
        }
        if (allowReceive) {
            allowedDestinations.add(id)
        } else {
            allowedDestinations.remove(id)
        }
    }

    override fun handle(handler: AdnlTransportHandler) {
        handlers.add(handler)
    }

    override suspend fun sendDatagram(
        destination: AdnlIdShort,
        address: AdnlAddress,
        datagram: ByteReadPacket
    ) {
        if (lossPortability > 0.0 && Random.nextDouble() < lossPortability) {
            return
        }
        if (destination !in allowedDestinations) {
            return
        }
        handlers.forEach {
            it.onReceiveDatagram(destination, address, datagram)
        }
    }
}

package io.tonblocks.adnl.transport

import io.ktor.utils.io.core.*
import io.tonblocks.adnl.AdnlAddress
import io.tonblocks.adnl.AdnlIdShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Instant

interface AdnlTransport : CoroutineScope {
    val reinitDate: Instant
    val mtu: Int get() = 1024

    fun handle(handler: AdnlTransportHandler)

    suspend fun sendDatagram(
        destination: AdnlIdShort,
        address: AdnlAddress,
        datagram: ByteReadPacket
    )
}

fun interface AdnlTransportHandler {
    suspend fun onReceiveDatagram(
        destination: AdnlIdShort,
        address: AdnlAddress,
        datagram: ByteReadPacket
    )
}

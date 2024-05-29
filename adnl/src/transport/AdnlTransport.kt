package io.tonblocks.adnl.transport

import io.ktor.utils.io.core.*
import io.tonblocks.adnl.AdnlAddress
import io.tonblocks.adnl.AdnlNodeIdShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Instant
import transport.AdnlCategoryMask

interface AdnlTransport : CoroutineScope {
    val reinitDate: Instant
    val mtu: Int get() = 1024

    fun handle(handler: AdnlTransportHandler)

    suspend fun sendDatagram(
        destination: AdnlNodeIdShort,
        destinationAddress: AdnlAddress,
        datagram: ByteReadPacket
    )
}

fun interface AdnlTransportHandler {
    suspend fun onReceiveDatagram(
        destination: AdnlNodeIdShort,
        address: AdnlAddress,
        datagram: ByteReadPacket
    )
}

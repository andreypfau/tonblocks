package io.tonblocks.adnl.transport

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import io.tonblocks.adnl.AdnlAddress
import io.tonblocks.adnl.AdnlIdShort
import io.tonblocks.adnl.toAdnlAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString
import kotlin.coroutines.CoroutineContext

class UdpAdnlTransport(
    bindAddress: InetSocketAddress,
    coroutineContext: CoroutineContext = Dispatchers.Default
) : AdnlTransport {
    private val transportJob = Job()
    override val coroutineContext: CoroutineContext = coroutineContext + transportJob

    constructor(port: Int, coroutineContext: CoroutineContext = Dispatchers.Default) : this("0.0.0.0", port, coroutineContext)

    constructor(host: String, port: Int, coroutineContext: CoroutineContext = Dispatchers.Default) : this(
        InetSocketAddress(host, port),
        coroutineContext
    )

    override val reinitDate: Instant = Clock.System.now()
    private val handlers = mutableListOf<AdnlTransportHandler>()
    private val socket = aSocket(SelectorManager()).udp().bind(bindAddress).apply {
        launch {
            while (true) {
                val datagram = receive()
                val address = (datagram.address as InetSocketAddress).toAdnlAddress()
                val destination = AdnlIdShort(ByteString(*datagram.packet.readBytes(32)))
                handlers.forEach {
                    launch {
                        it.onReceiveDatagram(
                            destination,
                            address,
                            datagram.packet
                        )
                    }
                }
            }
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
        val udpAddress = (address as? AdnlAddress.Udp)?.let {
            InetSocketAddress(it.address, it.port)
        } ?: return
        val udpDatagram = Datagram(
            packet = buildPacket {
                writeFully(destination.publicKeyHash.toByteArray())
                writePacket(datagram)
            },
            address = udpAddress
        )
        socket.send(udpDatagram)
    }
}

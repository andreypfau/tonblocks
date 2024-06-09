package io.tonblocks.adnl

import io.ktor.utils.io.core.*
import io.tonblocks.adnl.transport.AdnlNetworkTransport
import io.tonblocks.adnl.transport.AdnlTransportHandler

class AdnlNetworkManage(
    val transport: AdnlNetworkTransport,
    val peerTable: AdnlPeerTable,
) : AdnlTransportHandler {
    private val localNodes = HashMap<AdnlIdShort, AdnlNode>()

    init {
        transport.handle(this)
    }

    fun registerNode(node: AdnlNode) {
        val shortId = node.id.shortId()
        localNodes[shortId] = node
        println("Registered node: $node")
    }

    override suspend fun onReceiveDatagram(destination: AdnlIdShort, address: AdnlAddress, datagram: ByteReadPacket) {
        val channel = peerTable.getChannel(destination)
        if (channel != null) {
            channel.handleDatagram(datagram)
            return
        }

        val node = localNodes[destination]
        if (node != null) {
            node.processDatagram(address, datagram)
            return
        }

        println("Drop datagram, Unknown destination: $destination")
    }
}

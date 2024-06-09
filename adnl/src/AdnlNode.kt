package io.tonblocks.adnl

import io.github.andreypfau.tl.serialization.TL
import io.ktor.utils.io.core.*
import io.tonblocks.adnl.transport.AdnlNetworkTransport
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.serialization.decodeFromByteArray
import tl.ton.AdnlPacketContents

class AdnlNode(
    val key: Ed25519.PrivateKey,
    val peerTable: AdnlPeerTable,
    val transport: AdnlNetworkTransport,
    val addressList: AdnlAddressList = AdnlAddressList()
) {
    val id = AdnlIdFull(key.publicKey())

    suspend fun processDatagram(
        address: AdnlAddress,
        datagram: ByteReadPacket,
    ) {
        val decrypted = key.createDecryptor().decryptToByteArray(datagram.readBytes())
        val packet = AdnlPacket(tl = TL.Boxed.decodeFromByteArray<AdnlPacketContents>(decrypted))

        var addressList = packet.addressList
        if (addressList.isNullOrEmpty()) {
            addressList = AdnlAddressList(address)
        }

        val sourceKey = packet.source
        if (sourceKey != null) {
            if (!packet.checkSignature()) {
                println("$this Bad packet, invalid public key from: ${sourceKey.shortId()}")
                return
            }
//            val peer = peer(sourceKey, addressList)
//            return peer.processPacket(packet)
        }

//        val sourceId = packet.sourceShort ?: packet.source?.shortId()
//        if (sourceId != null) {
//            val peer = peer(sourceId, addressList)
//            return peer.processPacket(packet)
//        }

        println("$this Bad packet, unknown source")
    }

    override fun toString(): String = "[local ${id.shortId()}]"
}

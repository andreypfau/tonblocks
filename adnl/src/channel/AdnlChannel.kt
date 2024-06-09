package io.tonblocks.adnl.channel

import io.github.andreypfau.tl.serialization.TL
import io.ktor.utils.io.core.*
import io.tonblocks.adnl.*
import io.tonblocks.crypto.Decryptor
import io.tonblocks.crypto.Encryptor
import io.tonblocks.crypto.aes.PrivateKeyAes
import io.tonblocks.crypto.aes.PublicKeyAes
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromByteArray
import tl.ton.AdnlPacketContents

class AdnlChannel(
    @Deprecated("Use peer pair")
    val connection: AdnlConnection?,
    val peerPair: AdnlPeerPair?,
    val input: AdnlInputChannel,
    val output: AdnlOutputChannel,
    val date: Instant,
) {
    suspend fun handleDatagram(datagram: ByteReadPacket) {
        val decrypted = input.decryptor.decryptToByteArray(datagram.readBytes())
        val packet = AdnlPacket(tl = TL.Boxed.decodeFromByteArray<AdnlPacketContents>(decrypted))
        connection.handlePacket(packet)
    }

    companion object {
        // TODO: rewrite
        fun create(
            connection: AdnlConnection?,
            peerPair: AdnlPeerPair?,
            localKey: Ed25519.PrivateKey,
            remoteKey: Ed25519.PublicKey,
            date: Instant
        ): AdnlChannel {
            val localId: AdnlIdShort
            val remoteId: AdnlIdFull

            if (connection != null) {
                localId = connection.localNode.id.shortId()
                remoteId = connection.remotePeerId
            } else if (peerPair != null) {
                localId = peerPair.node.id.shortId()
                remoteId = AdnlIdFull(runBlocking { peerPair.peer.awaitPublicKey() })
            } else {
                throw IllegalStateException()
            }

            val secret = localKey.sharedKey(remoteKey)
            val reversedSecret = secret.reversedArray()

            val inputKey: PrivateKeyAes
            val outputKey: PublicKeyAes
            val compare = localId.compareTo(remoteId)
            when {
                compare < 0 -> {
                    inputKey = PrivateKeyAes(secret)
                    outputKey = PublicKeyAes(reversedSecret)
                }
                compare > 0 -> {
                    inputKey = PrivateKeyAes(reversedSecret)
                    outputKey = PublicKeyAes(secret)
                }
                else -> {
                    inputKey = PrivateKeyAes(secret)
                    outputKey = PublicKeyAes(secret)
                }
            }
            val input = AdnlInputChannel(inputKey)
            val output = AdnlOutputChannel(outputKey)
            return AdnlChannel(connection, peerPair, input, output, date)
        }
    }

    override fun toString(): String {
        return "[Channel ${output.id}-${input.id}]"
    }
}

class AdnlInputChannel(
    val id: AdnlIdShort,
    val decryptor: Decryptor
) {
    constructor(key: PrivateKeyAes) : this(AdnlIdShort(key.hash()), key.createDecryptor())
}

class AdnlOutputChannel(
    val id: AdnlIdShort,
    val encryptor: Encryptor,
) {
    constructor(key: PublicKeyAes) : this(AdnlIdShort(key.hash()), key.createEncryptor())
}

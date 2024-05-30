package io.tonblocks.adnl.channel

import io.github.andreypfau.tl.serialization.TL
import io.ktor.utils.io.core.*
import io.tonblocks.adnl.AdnlConnection
import io.tonblocks.adnl.AdnlIdShort
import io.tonblocks.adnl.AdnlPacket
import io.tonblocks.crypto.Decryptor
import io.tonblocks.crypto.Encryptor
import io.tonblocks.crypto.aes.PrivateKeyAes
import io.tonblocks.crypto.aes.PublicKeyAes
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromByteArray
import tl.ton.adnl.AdnlPacketContents

class AdnlChannel(
    val connection: AdnlConnection,
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
        fun create(
            connection: AdnlConnection,
            localKey: Ed25519.PrivateKey,
            remoteKey: Ed25519.PublicKey,
            date: Instant
        ): AdnlChannel {
            val localId = connection.localNode.id.shortId()
            val remoteId = connection.remotePeer.id.shortId()

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
            return AdnlChannel(connection, input, output, date)
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

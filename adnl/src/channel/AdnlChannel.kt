package io.tonblocks.adnl.channel

import io.tonblocks.crypto.Decryptor
import io.tonblocks.crypto.Encryptor
import io.tonblocks.crypto.PublicKeyHash
import io.tonblocks.crypto.aes.PrivateKeyAes
import io.tonblocks.crypto.aes.PublicKeyAes
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.toHexString

typealias AdnlChannelIdShort = PublicKeyHash

class AdnlChannel(
    val input: AdnlInputChannel,
    val output: AdnlOutputChannel,
    val date: Instant,
) {
    companion object {
        fun create(
            localKey: Ed25519.PrivateKey,
            remoteKey: Ed25519.PublicKey,
            date: Instant
        ): AdnlChannel {
            val secret = localKey.sharedKey(remoteKey)
            val revSecret = ByteArray(32)
            for (i in 0 until 32) {
                revSecret[i] = secret[31 - i]
            }
            val inputKey: PrivateKeyAes
            val outputKey: PublicKeyAes
            val compare = localKey.hash().compareTo(remoteKey.hash())
            when {
                compare < 0 -> {
                    inputKey = PrivateKeyAes(secret)
                    outputKey = PublicKeyAes(revSecret)
                }
                compare > 0 -> {
                    inputKey = PrivateKeyAes(revSecret)
                    outputKey = PublicKeyAes(secret)
                }
                else -> {
                    inputKey = PrivateKeyAes(secret)
                    outputKey = PublicKeyAes(secret)
                }
            }
            val input = AdnlInputChannel(inputKey)
            val output = AdnlOutputChannel(outputKey)
            return AdnlChannel(input, output, date)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        return "AdnlChannel(${output.id.toHexString()} - ${input.id.toHexString()})"
    }
}

class AdnlInputChannel(
    val id: AdnlChannelIdShort,
    val decryptor: Decryptor
) {
    constructor(key: PrivateKeyAes) : this(key.hash(), key.createDecryptor())
}

class AdnlOutputChannel(
    val id: AdnlChannelIdShort,
    val encryptor: Encryptor,
) {
    constructor(key: PublicKeyAes) : this(key.hash(), key.createEncryptor())
}

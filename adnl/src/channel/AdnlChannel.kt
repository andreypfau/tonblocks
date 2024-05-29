package io.tonblocks.adnl.channel

import io.tonblocks.crypto.*
import io.tonblocks.crypto.aes.*
import io.tonblocks.crypto.ed25519.*
import kotlinx.datetime.Instant

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

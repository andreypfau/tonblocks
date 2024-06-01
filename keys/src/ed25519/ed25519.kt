package io.tonblocks.crypto.ed25519

import io.github.andreypfau.curve25519.ed25519.Ed25519
import io.github.andreypfau.curve25519.ed25519.Ed25519PublicKey
import io.github.andreypfau.kotlinx.crypto.sha256
import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.crypto.Decryptor
import io.tonblocks.crypto.Encryptor
import io.tonblocks.crypto.aes.DecryptorAes
import io.tonblocks.crypto.aes.EncryptorAes
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.encodeToByteArray
import kotlin.random.Random

object Ed25519 {
    fun random(): PrivateKey {
        return PrivateKey(Ed25519.generateKey(Random))
    }

    fun fromSeed(source: ByteArray, offset: Int = 0): PrivateKey {
        return PrivateKey(source.copyOfRange(offset, offset + 32))
    }

    /**
     * Ed25519 public key.
     */
    class PublicKey(
        internal val key: Ed25519PublicKey
    ) : io.tonblocks.crypto.PublicKey {
        constructor(key: ByteArray) : this(Ed25519PublicKey(key))

        constructor(tl: tl.ton.PublicKey.Ed25519) : this(tl.key.toByteArray())

        private val hash: ByteString by lazy {
            super.hash()
        }

        override fun hash(): ByteString = hash

        fun toByteArray(): ByteArray = key.toByteArray()

        override fun createEncryptor() = EncryptorEd25519(key)

        override fun tl(): tl.ton.PublicKey {
            return tl.ton.PublicKey.Ed25519(ByteString(*toByteArray()))
        }

        @OptIn(ExperimentalStdlibApi::class)
        override fun toString(): String = key.toByteArray().toHexString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PublicKey) return false
            if (!key.toByteArray().contentEquals(other.key.toByteArray())) return false
            return true
        }

        override fun hashCode(): Int {
            return key.hashCode()
        }
    }

    /**
     * Ed25519 private key.
     */
    class PrivateKey(
        internal val key: io.github.andreypfau.curve25519.ed25519.Ed25519PrivateKey
    ) : io.tonblocks.crypto.PrivateKey {
        constructor(key: ByteArray) : this(Ed25519.keyFromSeed(key))

        constructor(tl: tl.ton.PrivateKey.Ed25519) : this(tl.key.toByteArray())

        private val public = PublicKey(this.key.publicKey())

        fun toByteArray(): ByteArray = key.seed()

        override fun publicKey(): PublicKey = public

        override fun createDecryptor() = DecryptorEd25519(key)

        override fun tl(): tl.ton.PrivateKey = tl.ton.PrivateKey.Ed25519(ByteString(*key.seed()))

        fun sharedKey(remoteKey: PublicKey): ByteArray = key.sharedKey(remoteKey.key)
    }
}

class EncryptorEd25519 internal constructor(
    private val remoteKey: Ed25519PublicKey
) : Encryptor {
    override fun encryptToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        val result = ByteArray(64 + endIndex - startIndex)
        encryptIntoByteArray(source, result, 0, startIndex, endIndex)
        return result
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun encryptIntoByteArray(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int,
        startIndex: Int,
        endIndex: Int
    ) {
        val key = Ed25519.generateKey(Random)
        key.publicKey().toByteArray(destination, destinationOffset)
        val shared = key.sharedKey(remoteKey)
        val encryptor = EncryptorAes(shared)
        encryptor.encryptIntoByteArray(source, destination, destinationOffset + 32, startIndex, endIndex)
    }

    override fun checkSignature(message: ByteArray, signature: ByteArray): Boolean {
        return remoteKey.verify(message, signature)
    }
}

class DecryptorEd25519 internal constructor(
    private val key: io.github.andreypfau.curve25519.ed25519.Ed25519PrivateKey,
) : Decryptor {
    override fun decryptToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        val result = ByteArray(endIndex - startIndex - 64)
        decryptIntoByteArray(source, result, 0, startIndex, endIndex)
        return result
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun decryptIntoByteArray(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int,
        startIndex: Int,
        endIndex: Int
    ) {
        val pub = Ed25519PublicKey(source.copyOfRange(startIndex, startIndex + 32))
        val shared = key.sharedKey(pub)
        val decryptor = DecryptorAes(shared)
        decryptor.decryptIntoByteArray(source, destination, destinationOffset, startIndex + 32, endIndex)
    }

    override fun sign(message: ByteArray): ByteArray {
        return key.sign(message)
    }
}

fun tl.ton.PublicKey.hash(): ByteString =
    ByteString(*sha256(TL.Boxed.encodeToByteArray(this)))


package io.tonblocks.crypto.aes

import io.github.andreypfau.kotlinx.crypto.AES
import io.github.andreypfau.kotlinx.crypto.CTRBlockCipher
import io.github.andreypfau.kotlinx.crypto.Sha256
import io.tonblocks.crypto.Decryptor
import io.tonblocks.crypto.Encryptor
import io.tonblocks.crypto.PrivateKey
import io.tonblocks.crypto.PublicKey
import kotlinx.io.bytestring.ByteString

class PublicKeyAes(
    private val sharedSecret: ByteArray
) : PublicKey {
    constructor(tl: tl.ton.PublicKey.Aes) : this(tl.key.toByteArray())

    override fun createEncryptor() = EncryptorAes(sharedSecret)

    fun toByteArray(): ByteArray = sharedSecret.copyOf()

    override fun tl(): tl.ton.PublicKey {
        return tl.ton.PublicKey.Aes(ByteString(*sharedSecret))
    }
}

class PrivateKeyAes(
    private val sharedSecret: ByteArray
) : PrivateKey {
    constructor(tl: tl.ton.PrivateKey.Aes) : this(tl.key.toByteArray())

    override fun publicKey(): PublicKey = PublicKeyAes(sharedSecret)

    override fun createDecryptor() = DecryptorAes(sharedSecret)

    fun toByteArray(): ByteArray = sharedSecret.copyOf()

    override fun tl(): tl.ton.PrivateKey {
        return tl.ton.PrivateKey.Aes(ByteString(*sharedSecret))
    }
}

class EncryptorAes(
    private val secret: ByteArray
) : Encryptor {
    override fun encryptToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        val result = ByteArray(32 + endIndex - startIndex)
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
//        println("AES ENCRYPT destinationOffset = $destinationOffset startIndex = $startIndex endIndex = $endIndex")
        Sha256().update(source, startIndex, endIndex).digest(destination, destinationOffset)
//        println("AES ENCRYPT digest = ${destination.copyOfRange(destinationOffset, destinationOffset + 32).toHexString()}")

        val key = ByteArray(32)
        secret.copyInto(key, destinationOffset = 0, startIndex = 0, endIndex = 16)
        destination.copyInto(
            key,
            destinationOffset = 16,
            startIndex = destinationOffset + 16,
            endIndex = destinationOffset + 32
        )
//        println("encrypt key = ${key.toHexString()}")

        val iv = ByteArray(16)
        destination.copyInto(
            iv,
            destinationOffset = 0,
            startIndex = destinationOffset,
            endIndex = destinationOffset + 16
        )
        secret.copyInto(iv, destinationOffset = 4, startIndex = 20, endIndex = 32)
//        println("encrypt iv = ${iv.toHexString()}")

        CTRBlockCipher(AES(key), iv).processBytes(source, destination, destinationOffset + 32, startIndex, endIndex)
//        println("AES ENCRYPT destination = ${destination.toHexString()}")
    }

    override fun checkSignature(message: ByteArray, signature: ByteArray): Boolean {
        throw UnsupportedOperationException("AES Encryptor cannot check signature")
    }
}

class DecryptorAes(
    val secret: ByteArray
) : Decryptor {
    override fun decryptToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        val result = ByteArray(endIndex - startIndex - 32)
        decryptIntoByteArray(source, result, 0, startIndex, endIndex)
        return result
    }

    override fun decryptIntoByteArray(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int,
        startIndex: Int,
        endIndex: Int
    ) {
//        println("AES DECRYPT destinationOffset = $destinationOffset startIndex = $startIndex endIndex = $endIndex")
        val digest = source.copyOfRange(startIndex, startIndex + 32)
//        println("AES DECRYPT digest = ${digest.toHexString()}")

        val key = ByteArray(32)
        secret.copyInto(key, destinationOffset = 0, startIndex = 0, endIndex = 16)
        source.copyInto(
            key,
            destinationOffset = 16,
            startIndex = startIndex + 16,
            endIndex = startIndex + 32
        )

//        println("decrypt key = ${key.toHexString()}")

        val iv = ByteArray(16)
        source.copyInto(
            iv,
            destinationOffset = 0,
            startIndex = startIndex,
            endIndex = startIndex + 4
        )
        secret.copyInto(iv, destinationOffset = 4, startIndex = 20, endIndex = 32)
//        println("decrypt iv = ${iv.toHexString()}")

        CTRBlockCipher(AES(key), iv).processBytes(source, destination, destinationOffset, startIndex + 32, endIndex)
        val actualDigest = Sha256().update(destination, destinationOffset, endIndex - startIndex - 32).digest()
        check(actualDigest.contentEquals(digest)) { "Digest mismatch" }
    }

    override fun sign(message: ByteArray): ByteArray {
        throw UnsupportedOperationException("AES Decryptor cannot sign")
    }
}

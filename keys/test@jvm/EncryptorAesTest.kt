package org.ton.keys

import io.github.andreypfau.kotlinx.crypto.sha256
import org.ton.Decryptor
import org.ton.Encryptor
import org.ton.aes.DecryptorAes
import org.ton.aes.EncryptorAes
import org.ton.ed25519.Ed25519
import kotlin.test.Test

class EncryptorAesTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun encryptTest() {
        val plain = "test".toByteArray()

//        val encryptor = EncryptorAes(sha256("test".toByteArray()))
//        val decryptor = DecryptorAes(sha256("test".toByteArray()))
//        encryptTest(encryptor, decryptor, plain)

        val alice = Ed25519.PrivateKey(sha256("test".toByteArray()))
        val encryptor2 = alice.publicKey().createEncryptor()
        val decryptor2 = alice.createDecryptor()
        encryptTest(encryptor2, decryptor2, plain)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun encryptTest(encryptor: Encryptor, decryptor: Decryptor, plain: ByteArray) {
        println(plain.toHexString())
        val encrypted = encryptor.encryptToByteArray(plain)
        val decrypted = decryptor.decryptToByteArray(encrypted)
        println(encrypted.toHexString())
        println(decrypted.toHexString())
    }
}

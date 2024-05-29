package org.ton.keys

import io.github.andreypfau.curve25519.internal.Sha512Pure.Companion.k
import io.github.andreypfau.kotlinx.crypto.sha256
import io.github.andreypfau.tl.serialization.TL
import kotlinx.io.bytestring.toHexString
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ton.ed25519.Ed25519
import tl.ton.PublicKey
import kotlin.random.Random
import kotlin.test.Test

class KeyTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testKey() {
//        val key = Ed25519.PrivateKey(sha256("test".encodeToByteArray()))
//        val public = key.publicKey()
//        println(public.toByteArray().toHexString())
//        println(public.hash().toHexString())
//        println(key.hash().toHexString())
//        println(TL.encodeToByteArray(public.tl()).toHexString())
//        println(TL.encodeToByteArray(public.tl()).decodeToString())
//        println(Json.encodeToString(public.tl()))
//
//        println(TL.decodeFromByteArray<PublicKey>(TL.encodeToByteArray(public.tl())))



        fun foo(key: ByteArray, data: ByteArray) {
            val k = Ed25519.PrivateKey(key)
            val signature = k.createDecryptor().sign(data)
            println("key  = ${k.tl()}")
            println("pub  = ${k.publicKey().toByteArray().toHexString()}")
            println("sign = ${signature.toHexString()}")
            val b = k.publicKey().createEncryptor().checkSignature(data, signature)
            println("res  = $b")
        }

        //B{81b637d8fcd2c6da6359e6963113a1170de795e4b725b84d1e0b4cfd9ec58ce9} priv>pub dup Bx. B{cafebabe} swap ed25519_sign Bx.

//        foo(sha256("test".encodeToByteArray()))
//        foo(sha256("bob".encodeToByteArray()))

        val data = "cafebabe".hexToByteArray()
        val key = sha256("test".encodeToByteArray())
        foo(key, data)
        println("  ")
        println(Ed25519.PrivateKey(key).createDecryptor().sign(data).toHexString(HexFormat.UpperCase))
        println(fift("B{${key.toHexString()}} dup priv>pub swap Bx. space Bx."))
        println(fift("B{${data.toHexString()}} B{${key.toHexString()}} ed25519_sign Bx."))
    }

    fun fift(command: String): String {
        val process = ProcessBuilder().command("fift").start()
        println(command)
        process.outputWriter().append(command).close()
        return process.inputReader().readText()
    }

//    @Test
//    fun fuzz() {
//        test(sha256("test".encodeToByteArray()), "test".encodeToByteArray())
//        val iterations = 10_000
//        var success = 0
//        repeat(iterations) {
//            try {
//                test(Random.nextBytes(32), Random.nextBytes(Random.nextInt(1, 100)))
//                println("Success = $it")
//                success++
//            } catch (e: Exception) {
//                println(e.message)
//            }
//        }
//        println("Failed percent = ${100 * (iterations - success) / iterations}% | success=$success")
//    }

    @OptIn(ExperimentalStdlibApi::class)
    fun test(key: ByteArray, data: ByteArray) {
        val k = Ed25519.PrivateKey(key)
        val signature = k.createDecryptor().sign(data)
        val b = k.publicKey().createEncryptor().checkSignature(data, signature)
        check(b) {
            "Signature check failed: key=${key.toHexString()} data=${data.toHexString()}"
        }
    }
}

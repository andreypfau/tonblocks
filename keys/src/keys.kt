package io.tonblocks.crypto

import io.github.andreypfau.kotlinx.crypto.sha256
import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.crypto.aes.PrivateKeyAes
import io.tonblocks.crypto.aes.PublicKeyAes
import io.tonblocks.crypto.ed25519.Ed25519
import io.tonblocks.crypto.overlay.OverlayPublicKey
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.encodeToByteArray

typealias PublicKeyHash = ByteString

interface PublicKey {
    fun hash(): PublicKeyHash =  ByteString(*sha256(TL.encodeToByteArray(tl())))

    fun createEncryptor(): Encryptor

    fun tl(): tl.ton.PublicKey
}

interface PrivateKey {
    fun publicKey(): PublicKey

    fun hash(): PublicKeyHash = publicKey().hash()

    fun createDecryptor(): Decryptor

    fun tl(): tl.ton.PrivateKey
}

public fun PublicKey(tl: tl.ton.PublicKey): PublicKey = when (tl) {
    is tl.ton.PublicKey.Ed25519 -> Ed25519.PublicKey(tl)
    is tl.ton.PublicKey.Aes -> PublicKeyAes(tl)
    is tl.ton.PublicKey.Unenc -> TODO()
    is tl.ton.PublicKey.Overlay -> OverlayPublicKey(tl)
}

public fun PrivateKey(tl: tl.ton.PrivateKey): PrivateKey = when (tl) {
    is tl.ton.PrivateKey.Ed25519 -> Ed25519.PrivateKey(tl)
    is tl.ton.PrivateKey.Aes -> PrivateKeyAes(tl)
    is tl.ton.PrivateKey.Unenc -> TODO()
    is tl.ton.PrivateKey.Overlay -> TODO()
}

package io.tonblocks.dht

import io.github.andreypfau.kotlinx.crypto.sha256
import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.PublicKeyHash
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.encodeToByteArray

typealias DhtKeyHash = ByteString

data class DhtKey(
    val id: PublicKeyHash,
    val name: String,
    val index: Int
) {
    constructor(tl: TlDhtKey) : this(tl.id, tl.name.decodeToString(), tl.idx)

    init {
        check(name.length <= 127) {
            "Too big name length. length=${name.length}"
        }
        check(name.isNotEmpty()) {
            "Empty name"
        }
        check(index in 0..15) {
            "Bad index. index=$index"
        }
    }

    fun tl(): TlDhtKey = TlDhtKey(
        id = id,
        name = name.encodeToByteString(),
        idx = index
    )

    fun hash(): DhtKeyHash = ByteString(*sha256(TL.Boxed.encodeToByteArray(tl())))
}

data class DhtKeyDescription(
    val key: DhtKey,
    val publicKey: PublicKey,
    val updateRule: DhtUpdateRule,
    val signature: ByteString
) {
    constructor(tl: TlDhtKeyDescription) : this(
        key = DhtKey(tl.key),
        publicKey = PublicKey(tl.id),
        updateRule = DhtUpdateRule(tl.updateRule),
        signature = tl.signature
    )

    init {
        check(key.id == publicKey.hash()) {
            "Key hash mismatch: key.id=${key.id}, publicKey.hash()=${publicKey.hash()}"
        }
    }

    fun tl(): TlDhtKeyDescription = TlDhtKeyDescription(
        key = key.tl(),
        id = publicKey.tl(),
        updateRule = updateRule.tl(),
        signature = signature
    )

    fun checkSignature(): Boolean {
        val toSign = TL.Boxed.encodeToByteArray(
            tl().copy(
                signature = ByteString()
            )
        )
        val encryptor = publicKey.createEncryptor()
        return encryptor.checkSignature(toSign, signature.toByteArray())
    }
}

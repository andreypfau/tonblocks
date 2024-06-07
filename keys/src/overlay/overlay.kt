package io.tonblocks.crypto.overlay

import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.crypto.Encryptor
import io.tonblocks.crypto.PublicKey
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.isNotEmpty
import tl.ton.DhtKeyDescription
import tl.ton.DhtUpdateRule

object OverlayEncryptor : Encryptor {
    override fun encryptToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        throw UnsupportedOperationException("Overlay can't be used for encryption")
    }

    override fun encryptIntoByteArray(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int,
        startIndex: Int,
        endIndex: Int
    ) {
        throw UnsupportedOperationException("Overlay can't be used for encryption")
    }

    override fun checkSignature(message: ByteArray, signature: ByteArray): Boolean {
        if (signature.isNotEmpty()) {
            return false
        }
        val description = try {
            TL.Boxed.decodeFromByteArray(DhtKeyDescription.serializer(), message)
        } catch (e: Exception) {
            return false
        }
        if (description.updateRule != DhtUpdateRule.OVERLAY_NODES) {
            return false
        }
        if (description.signature.isNotEmpty()) {
            return false
        }
        return true
    }
}

class OverlayPublicKey(
    val name: ByteString
) : PublicKey {
    constructor(tl: tl.ton.PublicKey.Overlay) : this(tl.name)

    override fun createEncryptor() = OverlayEncryptor

    override fun tl() = tl.ton.PublicKey.Overlay(
        name
    )
}

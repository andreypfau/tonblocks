package io.tonblocks.overlay

import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.AdnlIdFull
import io.tonblocks.crypto.PrivateKey
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.encodeToByteArray
import tl.ton.OverlayNode
import tl.ton.OverlayNodeToSign

data class OverlayNode(
    val source: AdnlIdFull,
    val overlayId: OverlayIdShort,
    val version: Int,
    var signature: ByteString = ByteString()
) {
    constructor(tl: OverlayNode) : this(
        source = AdnlIdFull(tl.id),
        overlayId = OverlayIdShort(tl.overlay),
        version = tl.version,
        signature = tl.signature
    )

    fun tl(): OverlayNode = OverlayNode(
        id = source.publicKey.tl(),
        overlay = overlayId.publicKeyHash,
        version = version,
        signature = signature
    )

    fun sign(privateKey: PrivateKey) = apply {
        val toSign = toSign()
        signature = ByteString(*privateKey.createDecryptor().sign(toSign))
    }

    fun checkSignature(): Boolean {
        return source.publicKey.createEncryptor().checkSignature(toSign(), signature.toByteArray())
    }

    private fun toSign(): ByteArray = TL.Boxed.encodeToByteArray(
        OverlayNodeToSign(
            id = source.shortId().tl(),
            overlay = overlayId.publicKeyHash,
            version = version
        )
    )
}

package io.tonblocks.dht

import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.AdnlIdFull
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.isEmpty
import kotlinx.io.bytestring.isNotEmpty
import tl.ton.OverlayNodeToSign

sealed class DhtUpdateRule {
    abstract val needRepublish: Boolean

    abstract fun checkValue(value: DhtValue): Boolean

    abstract fun tl(): TlDhtUpdateRule

    data object Signature : DhtUpdateRule() {
        override val needRepublish: Boolean = true

        override fun checkValue(value: DhtValue): Boolean {
            val encryptor = value.description.publicKey.createEncryptor()
            val signature = value.signature
            val tl = value.tl().copy(signature = ByteString())
            return encryptor.checkSignature(
                TL.Boxed.encodeToByteArray(TlDhtValue.serializer(), tl),
                signature.toByteArray()
            )
        }

        override fun tl(): TlDhtUpdateRule = TlDhtUpdateRule.SIGNATURE
    }

    data object AnyBody : DhtUpdateRule() {
        override val needRepublish: Boolean = false

        override fun checkValue(value: DhtValue): Boolean {
            return value.signature.isEmpty()
        }

        override fun tl(): TlDhtUpdateRule = TlDhtUpdateRule.ANYBODY
    }

    data object OverlayNodes : DhtUpdateRule() {
        override val needRepublish: Boolean = false

        override fun checkValue(value: DhtValue): Boolean {
            if (value.signature.isNotEmpty()) {
                return false
            }
            val nodes = TL.Boxed.decodeFromByteString(
                tl.ton.OverlayNodes.serializer(),
                value.value
            )
            for (node in nodes.nodes) {
                if (node.overlay != value.description.key.id) {
                    return false
                }
                val pub = AdnlIdFull(node.id)
                val toSign = TL.Boxed.encodeToByteArray(
                    OverlayNodeToSign.serializer(),
                    OverlayNodeToSign(
                        pub.shortId().tl(),
                        node.overlay,
                        node.version
                    )
                )
                val signature = node.signature.toByteArray()
                val encryptor = pub.publicKey.createEncryptor()
                if (!encryptor.checkSignature(toSign, signature)) {
                    return false
                }
            }
            return true
        }

        override fun tl(): TlDhtUpdateRule = TlDhtUpdateRule.OVERLAY_NODES
    }
}

fun DhtUpdateRule(tl: TlDhtUpdateRule): DhtUpdateRule {
    return when (tl) {
        TlDhtUpdateRule.SIGNATURE -> DhtUpdateRule.Signature
        TlDhtUpdateRule.ANYBODY -> DhtUpdateRule.AnyBody
        TlDhtUpdateRule.OVERLAY_NODES -> DhtUpdateRule.OverlayNodes
    }
}

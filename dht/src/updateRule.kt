package io.tonblocks.dht

sealed class DhtUpdateRule {
    abstract val needRepublish: Boolean

    abstract fun checkValue(value: DhtValue): Boolean

    abstract fun tl(): TlDhtUpdateRule

    data object Signature : DhtUpdateRule() {
        override val needRepublish: Boolean = true

        override fun checkValue(value: DhtValue): Boolean {
            TODO("Not yet implemented")
        }

        override fun tl(): TlDhtUpdateRule = TlDhtUpdateRule.SIGNATURE
    }

    data object AnyBody : DhtUpdateRule() {
        override val needRepublish: Boolean
            get() = TODO("Not yet implemented")

        override fun checkValue(value: DhtValue): Boolean {
            TODO("Not yet implemented")
        }

        override fun tl(): TlDhtUpdateRule = TlDhtUpdateRule.ANYBODY
    }

    data object OverlayNodes : DhtUpdateRule() {
        override val needRepublish: Boolean
            get() = TODO("Not yet implemented")

        override fun checkValue(value: DhtValue): Boolean {
            TODO("Not yet implemented")
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

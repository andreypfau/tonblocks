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
}

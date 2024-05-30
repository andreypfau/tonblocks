package io.tonblocks.adnl

import io.tonblocks.crypto.PublicKey

class AdnlPeer(
    val id: AdnlIdFull,
) {
    constructor(id: PublicKey) : this(AdnlIdFull(id))

    override fun toString(): String = "AdnlPeer($id)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdnlPeer) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int = id.hashCode()
}

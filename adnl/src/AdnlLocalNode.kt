package io.tonblocks.adnl

import io.tonblocks.crypto.ed25519.Ed25519

class AdnlLocalNode(
    val key: Ed25519.PrivateKey,
    val addressList: AdnlAddressList
) {
    val id = AdnlNodeIdFull(key.publicKey())

    fun tl(): tl.ton.adnl.AdnlNode {
        return tl.ton.adnl.AdnlNode(
            id = key.publicKey().tl(),
            addrList = addressList.tl()
        )
    }
}

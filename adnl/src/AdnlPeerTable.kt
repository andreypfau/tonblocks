package io.tonblocks.adnl

interface AdnlPeerTable {
    fun addPeer(
        localId: AdnlIdShort,
        id: AdnlIdFull,
        addressList: AdnlAddressList
    ): AdnlPeer

    fun getPeer(peerId: AdnlIdShort): AdnlPeer
}

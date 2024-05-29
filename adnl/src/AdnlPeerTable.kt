package io.tonblocks.adnl

interface AdnlPeerTable {
    fun addPeer(
        localId: AdnlNodeIdShort,
        id: AdnlNodeIdFull,
        addressList: AdnlAddressList
    ): AdnlPeer

    fun getPeer(peerId: AdnlNodeIdShort): AdnlPeer
}

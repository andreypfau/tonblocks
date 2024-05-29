package io.tonblocks.adnl

interface AdnlAddressResolver {
    suspend fun resolveAddress(id: AdnlNodeIdShort): AdnlAddressList?
}

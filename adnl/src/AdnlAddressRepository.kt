package io.tonblocks.adnl

import io.tonblocks.crypto.ShortId

interface AdnlAddressRepository {
    suspend fun saveAddress(id: ShortId<AdnlIdShort>, addressList: AdnlAddressList)

    suspend fun loadAddress(id: ShortId<AdnlIdShort>): AdnlAddressList?
}

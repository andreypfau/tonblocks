package io.tonblocks.adnl

import io.tonblocks.crypto.ShortId

fun interface AdnlAddressResolver {
    suspend fun resolveAddress(id: ShortId<AdnlIdShort>): AdnlAddressList?
}

package io.tonblocks.adnl

import kotlinx.datetime.Instant

data class AdnlAddressList(
    private val addresses: List<AdnlAddress> = emptyList(),
    val version: Int = 0,
    val reinitDate: Instant = Instant.fromEpochMilliseconds(0),
    val priority: Int = 0,
    val expireAt: Instant = Instant.fromEpochMilliseconds(0),
) : Collection<AdnlAddress> by addresses {
    constructor(vararg addresses: AdnlAddress) : this(addresses.toList())
}

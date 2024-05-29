package io.tonblocks.adnl

import kotlinx.datetime.Instant

typealias TlAdnlAddress = tl.ton.adnl.AdnlAddress
typealias TlAdnlAddressList = tl.ton.adnl.AdnlAddressList

fun AdnlAddressList.tl(): TlAdnlAddressList = TlAdnlAddressList(
    addrs = map { it.tl() },
    version = version,
    reinitDate = reinitDate.epochSeconds.toInt(),
    priority = priority,
    expireAt = expireAt.epochSeconds.toInt()
)

fun AdnlAddressList(tl: TlAdnlAddressList): AdnlAddressList = AdnlAddressList(
    reinitDate = Instant.fromEpochSeconds(tl.reinitDate.toLong()),
    expireAt = Instant.fromEpochSeconds(tl.expireAt.toLong()),
    priority = tl.priority,
    addresses = tl.addrs.map { AdnlAddress(it) }
)

fun AdnlAddress.tl(): TlAdnlAddress = when(this) {
    is AdnlAddress.Udp -> {
        tl.ton.adnl.AdnlAddress.Udp(
            ip = ip,
            port = port,
        )
    }
}

fun AdnlAddress(tl: TlAdnlAddress): AdnlAddress = when(tl) {
    is tl.ton.adnl.AdnlAddress.Udp -> {
        AdnlAddress.Udp(
            ip = tl.ip,
            port = tl.port,
        )
    }
    is tl.ton.adnl.AdnlAddress.Reverse -> TODO()
    is tl.ton.adnl.AdnlAddress.Tunnel -> TODO()
    is tl.ton.adnl.AdnlAddress.Udp -> TODO()
    is tl.ton.adnl.AdnlAddress.Udp6 -> TODO()
}

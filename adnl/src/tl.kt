package io.tonblocks.adnl

import kotlinx.datetime.Instant

typealias TlAdnlAddress = tl.ton.AdnlAddress
typealias TlAdnlAddressList = tl.ton.AdnlAddressList

fun AdnlAddressList.tl(): TlAdnlAddressList = TlAdnlAddressList(
    addrs = map { it.tl() },
    version = version,
    reinitDate = reinitDate.epochSeconds.toInt(),
    priority = priority,
    expireAt = expireAt?.epochSeconds?.toInt() ?: 0
)

fun AdnlAddressList(tl: TlAdnlAddressList): AdnlAddressList = AdnlAddressList(
    reinitDate = Instant.fromEpochSeconds(tl.reinitDate.toLong()),
    expireAt = if (tl.expireAt == 0) null else Instant.fromEpochSeconds(tl.expireAt.toLong()),
    priority = tl.priority,
    addresses = tl.addrs.map { AdnlAddress(it) }
)

fun AdnlAddress.tl(): TlAdnlAddress = when(this) {
    is AdnlAddress.Udp -> {
        tl.ton.AdnlAddress.Udp(
            ip = ip,
            port = port,
        )
    }
}

fun AdnlAddress(tl: TlAdnlAddress): AdnlAddress = when(tl) {
    is tl.ton.AdnlAddress.Udp -> {
        AdnlAddress.Udp(
            ip = tl.ip,
            port = tl.port,
        )
    }

    is tl.ton.AdnlAddress.Reverse -> TODO()
    is tl.ton.AdnlAddress.Tunnel -> TODO()
    is tl.ton.AdnlAddress.Udp -> TODO()
    is tl.ton.AdnlAddress.Udp6 -> TODO()
}

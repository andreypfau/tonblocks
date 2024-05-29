package io.tonblocks.adnl

import io.ktor.network.sockets.*

actual fun InetSocketAddress.toAdnlAddress(): AdnlAddress {
    return AdnlAddress.Udp(AdnlAddress.Udp.addressToInt(hostname), port)
}

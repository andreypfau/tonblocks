package io.tonblocks.adnl

import io.ktor.network.sockets.*
import io.ktor.util.network.*

actual fun InetSocketAddress.toAdnlAddress(): AdnlAddress {
    return AdnlAddress.Udp(AdnlAddress.Udp.addressToInt(this.toJavaAddress().address), port)
}

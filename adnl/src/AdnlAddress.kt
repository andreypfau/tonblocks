package io.tonblocks.adnl

import io.ktor.network.sockets.*
import kotlin.jvm.JvmStatic

sealed class AdnlAddress {
    class Udp(
        val ip: Int,
        val port: Int
    ) : AdnlAddress() {
        constructor(address: String, port: Int) : this(
            addressToInt(address),
            port
        )

        val address: String
            get() =
                "${(ip shr 24) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 8) and 0xFF}.${ip and 0xFF}"

        override fun toString(): String = "$address:$port"

        companion object {
            @JvmStatic
            fun addressToInt(address: String): Int {
                return when (address) {
                    "localhost" -> addressToInt("127.0.0.1")
                    else -> address.split('.').fold(0) { acc, it -> (acc shl 8) or it.toInt() }
                }
            }
        }
    }
}

expect fun InetSocketAddress.toAdnlAddress(): AdnlAddress

package io.tonblocks.dht

import io.ktor.util.*
import io.tonblocks.adnl.AdnlAddress
import io.tonblocks.adnl.AdnlAddressList
import io.tonblocks.adnl.AdnlConnection
import io.tonblocks.adnl.transport.UdpAdnlTransport
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString
import kotlin.random.Random
import kotlin.test.Test

class DhtClientTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testConnection(): Unit = runBlocking {

//        val connection = AdnlConnection(
//            transport = UdpAdnlTransport(3000),
//            localKey = Ed25519.random(),
//            remoteKey = Ed25519.PublicKey("IDs2z127Mu5B8gi8PGnuHfD6v5az8iy34mDeqFVm1PE=".decodeBase64Bytes()),
//            addressList = AdnlAddressList(AdnlAddress.Udp(2130706433, 3278))
//        )

        val connection = AdnlConnection(
            transport = UdpAdnlTransport(3000),
            localKey = Ed25519.random(),
            remoteKey = Ed25519.PublicKey("vhFPq+tgjJi+4ZbEOHBo4qjpqhBdSCzNZBdgXyj3NK8=".decodeBase64Bytes()),
            addressList = AdnlAddressList(AdnlAddress.Udp(85383775, 36752))
        )

        val dhtClient = DhtTlClient(connection)

        println(dhtClient.findValue(ByteString(Random.nextBytes(32)), 5))
    }
}

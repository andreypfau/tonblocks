package io.tonblocks.dht

import io.ktor.util.*
import io.tonblocks.adnl.*
import io.tonblocks.adnl.transport.UdpAdnlTransport
import io.tonblocks.crypto.ed25519.Ed25519
import io.tonblocks.kv.MapKeyValueRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString
import kotlin.random.Random
import kotlin.test.Test

class DhtClientTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testConnection(): Unit = runBlocking {

        val connection = AdnlConnection(
            transport = UdpAdnlTransport(3000),
            localKey = Ed25519.random(),
            remoteKey = Ed25519.PublicKey("IDs2z127Mu5B8gi8PGnuHfD6v5az8iy34mDeqFVm1PE=".decodeBase64Bytes()),
            addressList = AdnlAddressList(AdnlAddress.Udp(2130706433, 3278))
        )

        val dhtClient = DhtTlClient(connection)

        println(dhtClient.findValue(ByteString(Random.nextBytes(32)), 5))
        delay(1000)
        println(dhtClient.findValue(ByteString(Random.nextBytes(32)), 5))
    }

    @Test
    fun dhtNodeTest(): Unit = runBlocking {
        val dht = DhtImpl(
            localNode = AdnlLocalNode(Ed25519.random(), AdnlAddressList()),
            transport = UdpAdnlTransport(3000),
            repository = MapKeyValueRepository(),
        )
        val remoteNode = DhtNode(
            Ed25519.PublicKey("vhFPq+tgjJi+4ZbEOHBo4qjpqhBdSCzNZBdgXyj3NK8=".decodeBase64Bytes()),
            -1,
            AdnlAddressList(AdnlAddress.Udp(85383775, 36752)),
            signature = ByteString("kBwAIgJVkz8AIOGoZcZcXWgNmWq8MSBWB2VhS8Pd+f9LLPIeeFxlDTtwAe8Kj7NkHDSDC+bPXLGQZvPv0+wHCg==".decodeBase64Bytes())
        )
        dht.addNode(remoteNode)
        val res = dht.resolveAddress(AdnlNodeIdShort(ByteString(Random.nextBytes(32))))
//        println(res)
    }
}

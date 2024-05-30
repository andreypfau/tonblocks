package io.tonblocks.dht

import io.ktor.util.*
import io.tonblocks.adnl.AdnlAddress
import io.tonblocks.adnl.AdnlAddressList
import io.tonblocks.adnl.AdnlIdShort
import io.tonblocks.adnl.AdnlLocalNode
import io.tonblocks.adnl.transport.UdpAdnlTransport
import io.tonblocks.crypto.ed25519.Ed25519
import io.tonblocks.kv.MapKeyValueRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString
import kotlin.random.Random
import kotlin.test.Test
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

class DhtClientTest {
    val node = AdnlLocalNode(
        transport = UdpAdnlTransport(3000),
        key = Ed25519.random()
    )

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testConnection(): Unit = runBlocking {
        val dht = DhtImpl(
            localNode = node,
            repository = MapKeyValueRepository(),
        )
        val node = dht.addNode(
            DhtNode(
                publicKey = Ed25519.PublicKey("IDs2z127Mu5B8gi8PGnuHfD6v5az8iy34mDeqFVm1PE=".decodeBase64Bytes()),
                addressList = AdnlAddressList(AdnlAddress.Udp(2130706433, 3278))
            )
        )

        while (true) {
            println(node.client().findValue(ByteString(Random.nextBytes(32)), 5))
            delay(1000)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun dhtNodeTest(): Unit = runBlocking {
//        val node = AdnlLocalNode(
//            transport = UdpAdnlTransport(3000),
//            key = Ed25519.random()
//        )
        val dht = DhtImpl(
            localNode = node,
            repository = MapKeyValueRepository(),
        )
        val remoteNode = DhtNode(
            publicKey = Ed25519.PublicKey("vhFPq+tgjJi+4ZbEOHBo4qjpqhBdSCzNZBdgXyj3NK8=".decodeBase64Bytes()),
            version = -1,
            addressList = AdnlAddressList(AdnlAddress.Udp(85383775, 36752)),
            signature = ByteString("kBwAIgJVkz8AIOGoZcZcXWgNmWq8MSBWB2VhS8Pd+f9LLPIeeFxlDTtwAe8Kj7NkHDSDC+bPXLGQZvPv0+wHCg==".decodeBase64Bytes())
        )

        dht.addNode(remoteNode)
        val time = measureTime {
            dht.findNodes()
        }
        println("Populated table: $time")
        println("Table size: ${dht.routingTable.size}")
        val (value, dur) = measureTimedValue {
            dht.resolveAddress(AdnlIdShort(Ed25519.PublicKey("WT1sM59M3dNe+drdIH9z5b9ewds/NewsRaKtOx0uris=".decodeBase64Bytes())))
        }
        println("Found for $dur value: $value")
        println("Table size: ${dht.routingTable.size}")
    }
}

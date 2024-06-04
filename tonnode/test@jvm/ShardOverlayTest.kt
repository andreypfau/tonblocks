package io.tonblocks.tonnode

import io.tonblocks.adnl.AdnlAddress
import io.tonblocks.adnl.AdnlAddressList
import io.tonblocks.adnl.AdnlLocalNode
import io.tonblocks.adnl.transport.UdpAdnlTransport
import io.tonblocks.crypto.ed25519.Ed25519
import io.tonblocks.dht.DhtImpl
import io.tonblocks.dht.DhtNode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToByteString
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test

@OptIn(ExperimentalEncodingApi::class)
class ShardOverlayTest {
    val TON_MAINNET = Base64.decodeToByteString("XplPz01CXAps5qeSWUtxcyBfdAo5zVb1N979KLSKD24=")
    val TON_TESTNET = Base64.decodeToByteString("Z+IKwYS54DmmJmesw/nAD5DzWadnOCMzee+kdgSYDOg=")
    val EVERSCALE_MAINNET = Base64.decodeToByteString("0nC4eylStbp9qnCq8KjDYb789NjS25L5ZA1UQwcIOOQ=")
    val EVERSCALE_TESTNET = Base64.decodeToByteString("2Q2lg3IWbHJo9q2YXv1j2lwsmBtTuiT/djB66WEUd3c=")
    val VENOM_MAINNET = Base64.decodeToByteString("ywj7H75tJ3PgbEeX+UNP3j0iR1x9imIIJJuQgrlCr8s=")

    val localNode = AdnlLocalNode(
        transport = UdpAdnlTransport(3000),
        key = Ed25519.random()
    )

    @Test
    fun testShardOverlay() = runBlocking {
        val dht = DhtImpl(localNode)
        dht.addNode(
            DhtNode(
                publicKey = Ed25519.PublicKey(Base64.decode("vhFPq+tgjJi+4ZbEOHBo4qjpqhBdSCzNZBdgXyj3NK8=")),
                version = -1,
                addressList = AdnlAddressList(AdnlAddress.Udp(85383775, 36752)),
                signature = ByteString(Base64.decode("kBwAIgJVkz8AIOGoZcZcXWgNmWq8MSBWB2VhS8Pd+f9LLPIeeFxlDTtwAe8Kj7NkHDSDC+bPXLGQZvPv0+wHCg=="))
            )
        )

        val shard = TonNodeShardImpl(localNode, dht, ShardIdFull(ShardIdFull.MASTERCHAIN_ID), TON_MAINNET)
        shard.overlay.searchRandomPeers(dht)
        shard.overlay.peers.forEach {
            println("\n\n\nstart check $it")
            val res = withTimeoutOrNull(30000) {
                shard.overlay.searchRandomPeers(it)
            }
            if (res == null) {
                println("failed: $it")
            }
        }

    }
}

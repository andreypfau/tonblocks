import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.core.*
import io.tonblocks.adnl.AdnlAddress
import io.tonblocks.adnl.AdnlAddressList
import io.tonblocks.adnl.AdnlLocalNode
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.datetime.Clock
import kotlinx.io.Source
import org.ton.adnl.*
import org.ton.adnl.query.AdnlQueryId
import org.ton.adnl.transport.LoopbackAdnlTransport
import kotlin.random.Random
import kotlin.test.Test

class AdnlConnectionTest {

    @Test
    fun packetTest() {
        val transport = LoopbackAdnlTransport()
        val aliceKey = Ed25519.PrivateKey(Random(1).nextBytes(32))
        val bobKey = Ed25519.PrivateKey(Random(2).nextBytes(32))

        println("Alice key: ${aliceKey.tl()} ${aliceKey.publicKey()}")
        println("Bob key: ${bobKey.tl()} ${bobKey.publicKey()}")

        transport.addNodeId(AdnlNodeIdShort(aliceKey.publicKey()))
        transport.addNodeId(AdnlNodeIdShort(bobKey.publicKey()))

        val aliceConnection = object : AdnlConnection(

            transport,
            AdnlAddressList(Clock.System.now(), listOf(AdnlAddress.Udp(0, 0)))
        ) {
            override val localNode: AdnlLocalNode = AdnlLocalNode(aliceKey)
            override val remotePeer: AdnlPeer = AdnlPeer(bobKey.publicKey())

            override suspend fun handleCustom(data: Source) {
            }

            override suspend fun handleQuery(queryId: AdnlQueryId, data: Source) {
            }
        }

        val bobConnection = object : AdnlConnection(
            transport,
            AdnlAddressList(Clock.System.now(), listOf(AdnlAddress.Udp(0, 0)))
        ) {
            override val localNode: AdnlLocalNode = AdnlLocalNode(bobKey)
            override val remotePeer: AdnlPeer = AdnlPeer(aliceKey.publicKey())

            override suspend fun handleCustom(data: Source) {
            }

            override suspend fun handleQuery(queryId: AdnlQueryId, data: Source) {
            }
        }

        val connections = listOf(aliceConnection, bobConnection)

        transport.handle { dest, _, datagram ->
            connections.find { it.localId == dest }?.handlePacket(datagram)
        }
    }

//    @OptIn(ExperimentalStdlibApi::class)
//    @Test
//    fun staticNodeTest() = runBlocking {
//        val transport = UdpAdnlTransport(InetSocketAddress("0.0.0.0", 3000))
//        val connections = ArrayList<AdnlConnection>()
//        transport.handle { dest, address, datagram ->
//            connections.find { connection ->
//                connection.localId == dest || connection.channel?.input?.id == dest.publicKeyHash
//            }?.handlePacket(datagram)
//        }
//
////        val keyFile = File("/Users/andreypfau/mylocalton/dht/keyring/5FB010A3C85B0793AC3BCE44413CF34651ECF614F543FD7B76CD2E876569231B")
////        val key = Ed25519.PrivateKey(keyFile.readBytes().copyOfRange(4, 36))
////        println(key.publicKey().toByteArray().encodeBase64())
////        println(key.publicKey().toByteArray().toHexString())
////        println(key.hash().toByteArray().encodeBase64())
////        println(key.hash().toByteArray().toHexString())
//
//        val connection = AdnlConnection(
//            AdnlLocalNode(Ed25519.fromSeed(sha256("alice".toByteArray()))),
//            AdnlPeer(Ed25519.PublicKey("IDs2z127Mu5B8gi8PGnuHfD6v5az8iy34mDeqFVm1PE=".decodeBase64Bytes())),
//            transport,
//            AdnlAddressList(
//                Clock.System.now(), listOf(AdnlAddress.Udp(2130706433, 3278))
//            )
//        )
//
//
//    }
}

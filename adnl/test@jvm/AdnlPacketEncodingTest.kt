import io.github.andreypfau.tl.serialization.TL
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.ton.adnl.AdnlNodeIdFull
import org.ton.adnl.AdnlPacket
import org.ton.adnl.message.AdnlMessageCustom
import org.ton.adnl.message.AdnlMessageNop
import org.ton.ed25519.Ed25519
import tl.ton.AdnlPacketContents
import kotlin.test.Test

class AdnlPacketEncodingTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun packetEncodingTest() {
        val packet = AdnlPacket(
            rand1 = "cafebabe".hexToByteArray(),
            rand2 = "feedbeef".hexToByteArray(),
        )
        packet.source = AdnlNodeIdFull(Ed25519.PrivateKey(ByteArray(32)).publicKey())
        packet.addMessages(AdnlMessageNop, AdnlMessageCustom("deadbeef".hexToByteArray()))
        val packetTl = packet.tl()
        println(packetTl)
        val result = TL.Boxed.encodeToByteArray(packetTl)
        println(result.toHexString())

        val actualPacketTl = TL.Boxed.decodeFromByteArray<AdnlPacketContents>(result)
        println(actualPacketTl)
    }
}

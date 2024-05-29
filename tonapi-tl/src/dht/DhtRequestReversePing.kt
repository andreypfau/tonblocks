// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.dht

import io.github.andreypfau.tl.serialization.Base64ByteStringSerializer
import io.github.andreypfau.tl.serialization.TLCombinatorId
import io.github.andreypfau.tl.serialization.TLFixedSize
import kotlin.jvm.JvmName
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tl.ton.adnl.AdnlNode

@Serializable
@SerialName("dht.requestReversePing")
@TLCombinatorId(0x0FA65596)
public data class DhtRequestReversePing(
    @get:JvmName("target")
    public val target: AdnlNode,
    @get:JvmName("signature")
    public val signature: @Serializable(Base64ByteStringSerializer::class) ByteString,
    @TLFixedSize(value = 32)
    @get:JvmName("client")
    public val client: @Serializable(Base64ByteStringSerializer::class) ByteString,
    @get:JvmName("k")
    public val k: Int,
) {
    public companion object
}

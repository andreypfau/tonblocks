// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.Base64ByteStringSerializer
import io.github.andreypfau.tl.serialization.TLCombinatorId
import io.github.andreypfau.tl.serialization.TLFixedSize
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("dht.requestReversePingCont")
@TLCombinatorId(0xDBADC105)
public data class DhtRequestReversePingCont(
    @get:JvmName("target")
    public val target: AdnlNode,
    @get:JvmName("signature")
    public val signature: @Serializable(Base64ByteStringSerializer::class) ByteString,
    @TLFixedSize(value = 32)
    @get:JvmName("client")
    public val client: @Serializable(Base64ByteStringSerializer::class) ByteString,
) {
    public companion object
}
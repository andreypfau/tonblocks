// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.Base64ByteStringSerializer
import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.ihrMessage")
@TLCombinatorId(0x4534C307)
public data class TonNodeIhrMessage(
    @get:JvmName("data")
    public val `data`: @Serializable(Base64ByteStringSerializer::class) ByteString,
) {
    public companion object
}
// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.overlay

import io.github.andreypfau.tl.serialization.Base64ByteStringSerializer
import io.github.andreypfau.tl.serialization.TLCombinatorId
import io.github.andreypfau.tl.serialization.TLFixedSize
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("overlay.message")
@TLCombinatorId(0x75252420)
public data class OverlayMessage(
    @TLFixedSize(value = 32)
    @get:JvmName("overlay")
    public val overlay: @Serializable(Base64ByteStringSerializer::class) ByteString,
) {
    public companion object
}

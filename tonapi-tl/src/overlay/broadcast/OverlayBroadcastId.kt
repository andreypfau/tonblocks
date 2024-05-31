// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.overlay.broadcast

import io.github.andreypfau.tl.serialization.Base64ByteStringSerializer
import io.github.andreypfau.tl.serialization.TLCombinatorId
import io.github.andreypfau.tl.serialization.TLFixedSize
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("overlay.broadcast.id")
@TLCombinatorId(0x51FD789A)
public data class OverlayBroadcastId(
    @TLFixedSize(value = 32)
    @get:JvmName("src")
    public val src: @Serializable(Base64ByteStringSerializer::class) ByteString,
    @SerialName("data_hash")
    @TLFixedSize(value = 32)
    @get:JvmName("dataHash")
    public val dataHash: @Serializable(Base64ByteStringSerializer::class) ByteString,
    @get:JvmName("flags")
    public val flags: Int,
) {
    public companion object
}

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
@SerialName("dht.findValue")
@TLCombinatorId(0xAE4B6011)
public data class DhtFindValue(
    @TLFixedSize(value = 32)
    @get:JvmName("key")
    public val key: @Serializable(Base64ByteStringSerializer::class) ByteString,
    @get:JvmName("k")
    public val k: Int,
) {
    public companion object
}

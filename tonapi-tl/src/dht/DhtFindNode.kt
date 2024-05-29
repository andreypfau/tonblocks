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

@Serializable
@SerialName("dht.findNode")
@TLCombinatorId(0x6CE2CE6B)
public data class DhtFindNode(
    @TLFixedSize(value = 32)
    @get:JvmName("key")
    public val key: @Serializable(Base64ByteStringSerializer::class) ByteString,
    @get:JvmName("k")
    public val k: Int,
) {
    public companion object
}

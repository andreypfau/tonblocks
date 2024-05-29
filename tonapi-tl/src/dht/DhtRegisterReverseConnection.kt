// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.dht

import io.github.andreypfau.tl.serialization.Base64ByteStringSerializer
import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlin.jvm.JvmName
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tl.ton.PublicKey

@Serializable
@SerialName("dht.registerReverseConnection")
@TLCombinatorId(0xE6149BD8)
public data class DhtRegisterReverseConnection(
    @get:JvmName("node")
    public val node: PublicKey,
    @get:JvmName("ttl")
    public val ttl: Int,
    @get:JvmName("signature")
    public val signature: @Serializable(Base64ByteStringSerializer::class) ByteString,
) {
    public companion object
}

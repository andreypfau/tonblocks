// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.adnl

import io.github.andreypfau.tl.serialization.Base64ByteStringSerializer
import io.github.andreypfau.tl.serialization.TLCombinatorId
import io.github.andreypfau.tl.serialization.TLFixedSize
import kotlin.jvm.JvmName
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("adnl.proxyToFastHash")
@TLCombinatorId(0xDDBDF85E)
public data class AdnlProxyTo(
    @get:JvmName("ip")
    public val ip: Int,
    @get:JvmName("port")
    public val port: Int,
    @get:JvmName("date")
    public val date: Int,
    @SerialName("data_hash")
    @TLFixedSize(value = 32)
    @get:JvmName("dataHash")
    public val dataHash: @Serializable(Base64ByteStringSerializer::class) ByteString,
    @SerialName("shared_secret")
    @TLFixedSize(value = 32)
    @get:JvmName("sharedSecret")
    public val sharedSecret: @Serializable(Base64ByteStringSerializer::class) ByteString,
) {
    public companion object
}

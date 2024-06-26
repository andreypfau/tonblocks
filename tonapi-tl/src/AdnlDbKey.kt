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
@SerialName("adnl.db.node.key")
@TLCombinatorId(0xC5A3E42E)
public data class AdnlDbKey(
    @SerialName("local_id")
    @TLFixedSize(value = 32)
    @get:JvmName("localId")
    public val localId: @Serializable(Base64ByteStringSerializer::class) ByteString,
    @SerialName("peer_id")
    @TLFixedSize(value = 32)
    @get:JvmName("peerId")
    public val peerId: @Serializable(Base64ByteStringSerializer::class) ByteString,
) {
    public companion object
}

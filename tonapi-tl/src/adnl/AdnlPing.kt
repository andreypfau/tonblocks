// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.adnl

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlin.jvm.JvmName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("adnl.ping")
@TLCombinatorId(0x1B27DD0D)
public data class AdnlPing(
    @get:JvmName("value")
    public val `value`: Long,
) {
    public companion object
}

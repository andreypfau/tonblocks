// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.overlay

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("overlay.getBroadcastList")
@TLCombinatorId(0xD5E1F11B)
public data class OverlayGetBroadcastList(
    @get:JvmName("list")
    public val list: OverlayBroadcastList,
) {
    public companion object
}

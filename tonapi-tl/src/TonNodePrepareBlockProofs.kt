// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.prepareBlockProofs")
@TLCombinatorId(0x88C38F0F)
public data class TonNodePrepareBlockProofs(
    @get:JvmName("blocks")
    public val blocks: List<TonNodeBlockIdExt>,
    @SerialName("allow_partial")
    @get:JvmName("allowPartial")
    public val allowPartial: Boolean,
) {
    public companion object
}
// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.tonnode

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.getPrevBlocksDescription")
@TLCombinatorId(0x2C5F9EA4)
public data class TonNodeGetPrevBlocksDescription(
    @SerialName("next_block")
    @get:JvmName("nextBlock")
    public val nextBlock: TonNodeBlockIdExt,
    @get:JvmName("limit")
    public val limit: Int,
    @SerialName("cutoff_seqno")
    @get:JvmName("cutoffSeqno")
    public val cutoffSeqno: Int,
) {
    public companion object
}

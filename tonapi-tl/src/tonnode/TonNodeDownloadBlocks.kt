// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.tonnode

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.downloadBlocks")
@TLCombinatorId(0xC2F64774)
public data class TonNodeDownloadBlocks(
    @get:JvmName("blocks")
    public val blocks: List<TonNodeBlockIdExt>,
) {
    public companion object
}
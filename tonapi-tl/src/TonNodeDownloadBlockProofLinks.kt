// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.downloadBlockProofLinks")
@TLCombinatorId(0x7751781B)
public data class TonNodeDownloadBlockProofLinks(
    @get:JvmName("blocks")
    public val blocks: List<TonNodeBlockIdExt>,
) {
    public companion object
}
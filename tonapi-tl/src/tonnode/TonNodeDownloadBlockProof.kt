// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.tonnode

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.downloadBlockProof")
@TLCombinatorId(0xEB78A8BE)
public data class TonNodeDownloadBlockProof(
    @get:JvmName("block")
    public val block: TonNodeBlockIdExt,
) {
    public companion object
}
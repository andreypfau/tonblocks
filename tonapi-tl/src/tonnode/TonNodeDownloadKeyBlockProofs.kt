// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.tonnode

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.downloadKeyBlockProofs")
@TLCombinatorId(0xAF674A67)
public data class TonNodeDownloadKeyBlockProofs(
    @get:JvmName("blocks")
    public val blocks: List<TonNodeBlockIdExt>,
) {
    public companion object
}

// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.downloadKeyBlockProof")
@TLCombinatorId(0x32F214D3)
public data class TonNodeDownloadKeyBlockProof(
    @get:JvmName("block")
    public val block: TonNodeBlockIdExt,
) {
    public companion object
}

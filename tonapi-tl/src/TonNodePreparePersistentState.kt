// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.preparePersistentState")
@TLCombinatorId(0x93DA3605)
public data class TonNodePreparePersistentState(
    @get:JvmName("block")
    public val block: TonNodeBlockIdExt,
    @SerialName("masterchain_block")
    @get:JvmName("masterchainBlock")
    public val masterchainBlock: TonNodeBlockIdExt,
) {
    public companion object
}

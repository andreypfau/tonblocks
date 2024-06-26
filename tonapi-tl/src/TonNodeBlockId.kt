// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.blockId")
@TLCombinatorId(0xB7CDB167)
public data class TonNodeBlockId(
    @get:JvmName("workchain")
    public val workchain: Int,
    @get:JvmName("shard")
    public val shard: Long,
    @get:JvmName("seqno")
    public val seqno: Int,
) {
    public companion object
}

// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.dht

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlin.jvm.JvmName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("dht.store")
@TLCombinatorId(0x34934212)
public data class DhtStore(
    @get:JvmName("value")
    public val `value`: DhtValue,
) {
    public companion object
}

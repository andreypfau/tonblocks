// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.TLCombinatorId
import io.github.andreypfau.tl.serialization.TLFunction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("dht.ping")
@TLCombinatorId(0xCBEB3F18)
public data class DhtPing(
    @SerialName("random_id")
    @get:JvmName("randomId")
    public val randomId: Long,
) : TLFunction {
    public companion object
}

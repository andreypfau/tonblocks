// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("dht.query")
@TLCombinatorId(0x18403C12)
public data class DhtQuery(
    @get:JvmName("node")
    public val node: DhtNode,
) {
    public companion object
}

// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("tonNode.slave.sendExtMessage")
@TLCombinatorId(0x8DEAA0C7)
public data class TonNodeSlaveSendExtMessage(
    @get:JvmName("message")
    public val message: TonNodeExternalMessage,
) {
    public companion object
}
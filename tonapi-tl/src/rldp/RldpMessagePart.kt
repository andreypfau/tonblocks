// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.rldp

import io.github.andreypfau.tl.serialization.Base64ByteStringSerializer
import io.github.andreypfau.tl.serialization.TLCombinatorId
import io.github.andreypfau.tl.serialization.TLFixedSize
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import tl.ton.fec.FecType
import kotlin.jvm.JvmName

@Serializable
@JsonClassDiscriminator("@type")
public sealed interface RldpMessagePart {
    @Serializable
    @SerialName("rldp.messagePart")
    @TLCombinatorId(0x185C22CC)
    public data class MessagePart(
        @SerialName("transfer_id")
        @TLFixedSize(value = 32)
        @get:JvmName("transferId")
        public val transferId: @Serializable(Base64ByteStringSerializer::class) ByteString,
        @SerialName("fec_type")
        @get:JvmName("fecType")
        public val fecType: FecType,
        @get:JvmName("part")
        public val part: Int,
        @SerialName("total_size")
        @get:JvmName("totalSize")
        public val totalSize: Long,
        @get:JvmName("seqno")
        public val seqno: Int,
        @get:JvmName("data")
        public val `data`: @Serializable(Base64ByteStringSerializer::class) ByteString,
    ) : RldpMessagePart {
        public companion object
    }

    @Serializable
    @SerialName("rldp.confirm")
    @TLCombinatorId(0xF582DC58)
    public data class Confirm(
        @SerialName("transfer_id")
        @TLFixedSize(value = 32)
        @get:JvmName("transferId")
        public val transferId: @Serializable(Base64ByteStringSerializer::class) ByteString,
        @get:JvmName("part")
        public val part: Int,
        @get:JvmName("seqno")
        public val seqno: Int,
    ) : RldpMessagePart {
        public companion object
    }

    @Serializable
    @SerialName("rldp.complete")
    @TLCombinatorId(0xBC0CB2BF)
    public data class Complete(
        @SerialName("transfer_id")
        @TLFixedSize(value = 32)
        @get:JvmName("transferId")
        public val transferId: @Serializable(Base64ByteStringSerializer::class) ByteString,
        @get:JvmName("part")
        public val part: Int,
    ) : RldpMessagePart {
        public companion object
    }

    public companion object
}

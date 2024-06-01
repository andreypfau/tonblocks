// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.tonnode

import io.github.andreypfau.tl.serialization.Base64ByteStringSerializer
import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlin.jvm.JvmName

@Serializable
@JsonClassDiscriminator("@type")
public sealed interface TonNodeBroadcast {
    @Serializable
    @SerialName("tonNode.blockBroadcast")
    @TLCombinatorId(0xAE2E1105)
    public data class BlockBroadcast(
        @get:JvmName("id")
        public val id: TonNodeBlockIdExt,
        @SerialName("catchain_seqno")
        @get:JvmName("catchainSeqno")
        public val catchainSeqno: Int,
        @SerialName("validator_set_hash")
        @get:JvmName("validatorSetHash")
        public val validatorSetHash: Int,
        @get:JvmName("signatures")
        public val signatures: List<TonNodeBlockSignature>,
        @get:JvmName("proof")
        public val proof: @Serializable(Base64ByteStringSerializer::class) ByteString,
        @get:JvmName("data")
        public val `data`: @Serializable(Base64ByteStringSerializer::class) ByteString,
    ) : TonNodeBroadcast {
        public companion object
    }

    @Serializable
    @SerialName("tonNode.ihrMessageBroadcast")
    @TLCombinatorId(0x525DA4B3)
    public data class IhrMessageBroadcast(
        @get:JvmName("message")
        public val message: TonNodeIhrMessage,
    ) : TonNodeBroadcast {
        public companion object
    }

    @Serializable
    @SerialName("tonNode.externalMessageBroadcast")
    @TLCombinatorId(0x3D1B1867)
    public data class ExternalMessageBroadcast(
        @get:JvmName("message")
        public val message: TonNodeExternalMessage,
    ) : TonNodeBroadcast {
        public companion object
    }

    @Serializable
    @SerialName("tonNode.newShardBlockBroadcast")
    @TLCombinatorId(0x0AF2FABC)
    public data class NewShardBlockBroadcast(
        @get:JvmName("block")
        public val block: TonNodeNewShardBlock,
    ) : TonNodeBroadcast {
        public companion object
    }

    public companion object
}
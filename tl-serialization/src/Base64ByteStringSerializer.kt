package io.github.andreypfau.tl.serialization

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToByteString
import kotlinx.io.bytestring.encode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object Base64ByteStringSerializer : KSerializer<ByteString> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ByteString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ByteString {
        return Base64.decodeToByteString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: ByteString) {
        encoder.encodeString(Base64.encode(value))
    }
}

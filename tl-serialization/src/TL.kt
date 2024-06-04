package io.github.andreypfau.tl.serialization

import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

open class TL(
    val boxed: Boolean,
    override val serializersModule: SerializersModule
) : BinaryFormat {
    companion object Default : TL(false, EmptySerializersModule())
    data object Boxed : TL(true, EmptySerializersModule())

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val buffer = Buffer()
        encodeToSink(serializer, buffer, value)
        return buffer.readByteArray()
    }

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val buffer = Buffer()
        buffer.write(bytes)
        return decodeFromSource(deserializer, buffer)
    }

    fun <T> decodeFromByteString(deserializer: DeserializationStrategy<T>, bytes: ByteString): T {
        val buffer = Buffer()
        buffer.write(bytes)
        return decodeFromSource(deserializer, buffer)
    }

    fun <T> encodeToByteString(serializer: SerializationStrategy<T>, value: T): ByteString {
        val buffer = Buffer()
        encodeToSink(serializer, buffer, value)
        return buffer.readByteString()
    }

    fun <T> encodeToSink(serializationStrategy: SerializationStrategy<T>, sink: Sink, value: T) {
        val encoder = TLEncoder(this, sink, intArrayOf(), boxed)
        encoder.encodeSerializableValue(serializationStrategy, value)
    }

    fun <T> decodeFromSource(deserializer: DeserializationStrategy<T>, source: Source): T {
        val decoder = TLDecoder(this, source, intArrayOf(), boxed)
        return decoder.decodeSerializableValue(deserializer)
    }
}

inline fun <reified T> TL.decodeFromSource(source: Source): T =
    decodeFromSource(serializersModule.serializer(), source)

inline fun <reified T> TL.decodeFromByteString(bytes: ByteString): T =
    decodeFromByteString(serializersModule.serializer(), bytes)

inline fun <reified T> TL.encodeToSink(sink: Sink, value: T) =
    encodeToSink(serializersModule.serializer(), sink, value)

inline fun <reified T> TL.encodeToByteString(value: T): ByteString =
    encodeToByteString(serializersModule.serializer(), value)

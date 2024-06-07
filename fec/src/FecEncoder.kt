package io.tonblocks.fec

interface FecEncoder {
    val fecType: FecType

    fun encodeToByteArray(symbolId: Int, destination: ByteArray, offset: Int)
}

interface FecDecoder {
    val fecType: FecType

    fun addSymbol(
        symbolId: Int,
        data: ByteArray,
    ): Boolean

    fun decode(destination: ByteArray, destinationOffset: Int = 0)
}

sealed interface FecType {
    val dataSize: Int
    val symbolsCount: Int
    val symbolSize: Int

    fun createDecoder(): FecDecoder

    fun createEncoder(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size)

    class RaptorQ(
        override val dataSize: Int,
        override val symbolsCount: Int,
        override val symbolSize: Int,
    ) : FecType {
        override fun createDecoder() = RaptorQDecoder(this)

        override fun createEncoder(source: ByteArray, startIndex: Int, endIndex: Int) {
            TODO("Not yet implemented")
        }
    }
}

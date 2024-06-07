package io.tonblocks.fec.raptorq

import io.tonblocks.fec.FecDecoder
import io.tonblocks.fec.FecEncoder
import io.tonblocks.fec.FecType
import net.fec.openrq.OpenRQ
import net.fec.openrq.decoder.SourceBlockState
import net.fec.openrq.parameters.FECParameters

internal actual fun createRaptorQDecoder(fecType: FecType.RaptorQ): FecDecoder =
    OpenRQDecoder(fecType)

internal actual fun createRaptorQEncoder(fecType: FecType.RaptorQ, data: ByteArray, offset: Int): FecEncoder =
    OpenRQEncoder(fecType, data, offset)

private class OpenRQDecoder(
    override val fecType: FecType.RaptorQ
) : FecDecoder {
    private val decoder = OpenRQ.newDecoderWithOneOverhead(
        FECParameters.newParameters(fecType.dataSize.toLong(), fecType.symbolSize, 1)
    )

    private val sourceBlock = decoder.sourceBlock(0)

    override fun addSymbol(symbolId: Int, data: ByteArray): Boolean {
        return sourceBlock.putEncodingPacket(
            decoder.parsePacket(0, symbolId, data, false).value()
        ) == SourceBlockState.DECODED
    }

    override fun decode(destination: ByteArray, destinationOffset: Int) {
        decoder.dataArray().copyInto(destination, destinationOffset)
    }
}

private class OpenRQEncoder(
    override val fecType: FecType,
    data: ByteArray,
    offset: Int,
) : FecEncoder {
    private val encoder = OpenRQ.newEncoder(
        data,
        offset,
        FECParameters.newParameters(fecType.dataSize.toLong(), fecType.symbolSize, 1)
    )

    private val sourceBlock = encoder.sourceBlock(0)

    override fun encodeToByteArray(symbolId: Int, destination: ByteArray, offset: Int) {
        sourceBlock.encodingPacket(symbolId).writeTo(destination, offset)
    }
}

package io.tonblocks.fec.raptorq

import io.tonblocks.fec.FecDecoder
import io.tonblocks.fec.FecEncoder
import io.tonblocks.fec.FecType

internal expect fun createRaptorQDecoder(fecType: FecType.RaptorQ): FecDecoder
internal expect fun createRaptorQEncoder(fecType: FecType.RaptorQ, data: ByteArray, offset: Int = 0): FecEncoder

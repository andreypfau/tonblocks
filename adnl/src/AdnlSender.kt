package io.tonblocks.adnl

import io.ktor.utils.io.core.*

interface AdnlSender {
    suspend fun sendMessage(
        src: AdnlNodeIdShort,
        dst: AdnlNodeIdShort,
        message: ByteReadPacket
    )

    suspend fun sendQuery(
        src: AdnlNodeIdShort,
        dst: AdnlNodeIdShort,
        query: ByteReadPacket,
    ): ByteReadPacket
}

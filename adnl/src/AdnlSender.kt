package io.tonblocks.adnl

import io.ktor.utils.io.core.*

interface AdnlSender {
    suspend fun sendMessage(
        src: AdnlIdShort,
        dst: AdnlIdShort,
        message: ByteReadPacket
    )

    suspend fun sendQuery(
        src: AdnlIdShort,
        dst: AdnlIdShort,
        query: ByteReadPacket,
    ): ByteReadPacket
}

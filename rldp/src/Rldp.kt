package org.ton.rldp

import io.ktor.utils.io.core.*
import org.ton.adnl.AdnlNodeIdShort
import org.ton.adnl.AdnlSender

class Rldp(
    val adnl: Adnl
) : AdnlSender {
    override suspend fun sendMessage(src: AdnlNodeIdShort, dst: AdnlNodeIdShort, message: ByteReadPacket) {
        TODO("Not yet implemented")
    }

    override suspend fun sendQuery(src: AdnlNodeIdShort, dst: AdnlNodeIdShort, query: ByteReadPacket): ByteReadPacket {
        TODO("Not yet implemented")
    }
}

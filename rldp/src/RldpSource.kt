package org.ton.rldp

import io.tonblocks.utils.io.AsyncRawSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.io.Buffer
import tl.ton.rldp2.Rldp2MessagePart
import kotlin.coroutines.CoroutineContext

class RldpSource(
    val inputRldpMessageParts: Channel<Rldp2MessagePart>,
    coroutineContext: CoroutineContext = Dispatchers.IO
) : AsyncRawSource, CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = coroutineContext + job

    private val receiveJob = launch {
        while (true) {
            val messagePart = inputRldpMessageParts.receive()

        }
    }

    override suspend fun readAtMostTo(sing: Buffer, byteCount: Long): Long {

    }

    override suspend fun closeAndJoin() {
    }

    private fun onMessagePart(part: Rldp2MessagePart.MessagePart) {

    }
}

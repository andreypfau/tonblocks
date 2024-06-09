package io.tonblocks.adnl

import io.github.andreypfau.kotlinx.crypto.sha256
import io.github.andreypfau.tl.serialization.TL
import io.github.reactivecircus.cache4k.Cache
import io.tonblocks.adnl.message.AdnlMessage
import io.tonblocks.adnl.message.AdnlMessageProcessor
import io.tonblocks.adnl.message.AdnlPartMessage
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.decodeFromByteArray
import kotlin.time.Duration.Companion.seconds

class AdnlPartMessageProcessor private constructor(
    val channel: Channel<AdnlMessage>
) : AdnlMessageProcessor<AdnlPartMessage>, ReceiveChannel<AdnlMessage> by channel {
    constructor() : this(Channel(10))

    private val transfers = Cache.Builder<ByteString, AdnlPartMessageTransfer>()
        .expireAfterWrite(5.seconds)
        .maximumCacheSize(10)
        .build()

    override suspend fun processMessage(message: AdnlPartMessage) {
        val transferId = message.hash
        val transfer = transfers.get(transferId) {
            AdnlPartMessageTransfer(
                hash = transferId,
                totalSize = message.totalSize,
                updatedAt = Clock.System.now()
            )
        }
        transfer.update(message)
        if (transfer.isComplete() && transfer.isValid() && transfer.delivered.compareAndSet(false, true)) {
            val assembledMessage = AdnlMessage(TL.decodeFromByteArray<tl.ton.AdnlMessage>(transfer.data))
            channel.send(assembledMessage)
        }
    }
}

class AdnlPartMessageTransfer(
    val hash: ByteString,
    val totalSize: Int,
    updatedAt: Instant,
) {
    val data = ByteArray(totalSize)
    private val updatedAt_ = atomic(updatedAt)
    private val lock = reentrantLock()
    private val receivedOffsets = mutableSetOf<IntRange>()

    val updatedAt: Instant by updatedAt_
    val delivered = atomic(false)

    fun isComplete(): Boolean = receivedOffsets.sumOf {
        it.last - it.first
    } >= totalSize

    fun update(
        part: AdnlPartMessage
    ): Boolean {
        require(part.hash == hash) { "Bad part hash, expected: $hash, actual: ${part.hash}" }
        require(part.totalSize == totalSize) { "Bad part size, expected: $totalSize, actual: ${part.totalSize}" }
        require(part.offset + part.data.size <= part.totalSize) { "Bad part offset, ${part.offset}..${part.offset + part.data.size} not fits in ${part.totalSize}" }
        val added = lock.withLock {
            receivedOffsets.add(part.offset..(part.offset + part.serializedSize))
        }
        if (added) {
            updatedAt_.value = Clock.System.now()
            part.data.copyInto(data, part.offset)
        }
        return added
    }

    fun isValid(): Boolean {
        return ByteString(sha256(data)) == hash
    }
}

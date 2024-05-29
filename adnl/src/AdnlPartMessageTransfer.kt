package io.tonblocks.adnl

import io.github.andreypfau.kotlinx.crypto.sha256
import io.tonblocks.adnl.message.AdnlMessagePart
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString

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
        part: AdnlMessagePart
    ): Boolean {
        require(part.hash == hash) { "Bad part hash, expected: $hash, actual: ${part.hash}" }
        require(part.totalSize == totalSize) { "Bad part size, expected: $totalSize, actual: ${part.totalSize}" }
        require(part.offset + part.data.size <= part.totalSize) { "Bad part offset, ${part.offset}..${part.offset + part.data.size} not fits in ${part.totalSize}" }
        val added = lock.withLock {
            receivedOffsets.add(part.offset..(part.offset + part.size))
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

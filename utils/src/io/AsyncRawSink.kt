package io.tonblocks.utils.io

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import kotlinx.io.Buffer

interface AsyncRawSink {
    suspend fun write(source: Buffer, byteCount: Long)

    suspend fun flush()

    suspend fun closeAndJoin()
}

interface AsyncRawSource {
    suspend fun readAtMostTo(sing: Buffer, byteCount: Long): Long

    suspend fun closeAndJoin()
}

fun interface AwaitPredicate {
    suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean

    companion object {
        fun available(bytes: Long): AwaitPredicate {
            check(bytes >= 0) { "Bytes should not be negative: $bytes" }
            if (bytes.countOneBits() == 1) {
                val offset = bytes.countTrailingZeroBits()
                if (offset < dataAvailablePredicates.size) {
                    return dataAvailablePredicates[offset].value
                }
            }
            return DataAvailable(bytes)
        }

        private val dataAvailablePredicates = Array<Lazy<AwaitPredicate>>(16) {
            lazy(LazyThreadSafetyMode.NONE) {
                DataAvailable(1L shl it)
            }
        }
    }
}

private class DataAvailable(
    private val bytesCount: Long
) : AwaitPredicate {
    init {
        require(bytesCount > 0) { "The number of bytes should be positive, was: $bytesCount" }
    }

    override suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean {
        while (buffer.size < bytesCount && fetchMore()) {
            yield()
        }
        return buffer.size >= bytesCount
    }

    override fun toString(): String = "DataAvailable(bytesCount=$bytesCount)"
}

class AsyncSource(
    private val source: AsyncRawSource,
    private val fetchHint: Long = 8129L
) : AsyncRawSource {
    private var closed: Boolean = false

    val buffer = Buffer()
        get() {
            checkClosed()
            return field
        }

    suspend fun await(until: AwaitPredicate): Result<Boolean> {
        return try {
            checkClosed()
            currentCoroutineContext().ensureActive()
            Result.success(until.apply(buffer) {
                currentCoroutineContext().ensureActive()
                source.readAtMostTo(buffer, fetchHint) >= 0
            })
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun readAtMostTo(sing: Buffer, byteCount: Long): Long {
        checkClosed()
        return if (this.buffer.exhausted()) {
            source.readAtMostTo(buffer, byteCount)
        } else {
            this.buffer.readAtMostTo(sing, byteCount)
        }
    }

    override suspend fun closeAndJoin() {
        closed = true
        source.closeAndJoin()
    }

    private fun checkClosed() = check(!closed) { "The source is closed." }
}

fun AsyncRawSource.buffered(): AsyncSource = AsyncSource(this)

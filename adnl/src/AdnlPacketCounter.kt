package io.tonblocks.adnl

import kotlinx.atomicfu.atomic
import kotlinx.datetime.Instant

class AdnlPacketCounter(
    reinitDate: Instant = Instant.fromEpochSeconds(0)
) {
    private val _seqno = atomic(0L)
    private val _reinitDate = atomic(reinitDate.epochSeconds)

    var reinitDate: Instant
        get() = _reinitDate.value.let { Instant.fromEpochSeconds(it) }
        set(value) {
            _reinitDate.value = value.epochSeconds
        }

    fun nextSeqno(): Long =
        _seqno.getAndIncrement() + 1

    var seqno: Long
        get() = _seqno.value
        set(value) {
            while (true) {
                val lastSeqno = _seqno.value
                if (lastSeqno < value) {
                    if (_seqno.compareAndSet(lastSeqno, value)) {
                        break
                    }
                } else {
                    break
                }
            }
        }
}

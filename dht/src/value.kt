package io.tonblocks.dht

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString

data class DhtValue(
    val description: DhtKeyDescription,
    val value: ByteString,
    val ttl: Instant,
    val signature: ByteString
) {
    fun isExpired(now: Instant = Clock.System.now()): Boolean = ttl < now
}

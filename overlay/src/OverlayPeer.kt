package io.tonblocks.overlay

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

class OverlayPeer(
    node: OverlayNode,
    lastPingAt: Instant = Clock.System.now(),
) {
    private val node_ = atomic(node)
    val node by node_

    private val isAlive_ = atomic(true)
    val isAlive by isAlive_

    val idFull get() = node.source
    val id get() = idFull.shortId()

    private val missedPings = atomic(0)
    private val lastPingAt = atomic(lastPingAt)

    fun update(node: OverlayNode) {
        check(id == node.source.shortId()) {
            "Invalid overlay node id, expected: $id, actual: ${node.source.shortId()}"
        }
        if (node.version > node_.value.version) {
            node_.update { node }
        }
    }

    fun onSuccessPing(time: Instant = Clock.System.now()) {
        missedPings.value = 0
        lastPingAt.value = time
        isAlive_.value = true
    }

    fun onFailedPing(time: Instant = Clock.System.now()) {
        if (missedPings.getAndIncrement() + 1 < MAX_FAILED_PINGS) return
        if (time - lastPingAt.value < KEEP_ALIVE_DURATION) return
        isAlive_.value = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OverlayPeer) return false
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        const val MAX_FAILED_PINGS = 3
        val KEEP_ALIVE_DURATION = 15.seconds
    }
}

class OverlayPeerList<T>(
    private val entries: MutableList<T> = ArrayList(),
    private val maxPeers: Int = 25,
    private val maxReplacements: Int = 25
) : SynchronizedObject(), List<T> by entries {
    private val replacementCache = mutableListOf<T>()

    fun add(node: T): T? = synchronized(this) {
        // remove from the replacement cache
        replacementCache.remove(node)

        for (i in entries.indices) {
            if (entries[i] == node) {
                entries.removeAt(i)
                entries.add(0, node)
                return null
            }
        }

        if (entries.size >= maxPeers) {
            if (replacementCache.size >= maxReplacements) {
                replacementCache.removeAt(0)
            }
            replacementCache.add(node)
            return entries.last()
        }

        entries.add(0, node)
        return null
    }

    fun evict(node: T): Boolean = synchronized(this) {
        if (!entries.remove(node)) {
            return false
        }
        if (!replacementCache.isEmpty()) {
            val replacement = replacementCache.removeAt(replacementCache.lastIndex)
            entries.add(0, replacement)
        }
        return true
    }
}

package io.tonblocks.dht

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.indices

infix fun ByteString.xorDistance(other: ByteString): Int {
    require(size == other.size) { "ByteStrings must be of the same size" }
    var distance = size * 8
    for (i in indices) {
        val xor = (this[i].toInt() xor other[i].toInt()) and 0xff
        if (xor == 0) {
            distance -= 8
        } else {
            val d = (xor.countLeadingZeroBits() - 24)
            distance -= d
            break
        }
    }
    return distance
}

fun ByteString.xorDistCompare(a: ByteString, b: ByteString): Int {
    for (i in indices) {
        val xorA = (this[i].toInt() xor a[i].toInt()) and 0xff
        val xorB = (this[i].toInt() xor b[i].toInt()) and 0xff
        if (xorA > xorB) {
            return 1
        } else if (xorA < xorB) {
            return -1
        }
    }
    return 0
}

class KademliaRoutingTable<T : Comparable<T>>(
    private val selfId: ByteString,
    k: Int,
    private val nodeId: (T) -> ByteString,
    private val distanceToSelf: (T) -> Int = { nodeId(it) xorDistance selfId }
) : Set<T> {
    private val idBitSize = selfId.size * Byte.SIZE_BITS
    private val buckets_ = Array(idBitSize + 1) { KademliaBucket(k, nodeId) }
    val buckets get() = buckets_.asList()

    override val size: Int
        get() = buckets_.fold(0) { acc, bucket -> acc + bucket.size }

    override fun isEmpty(): Boolean = buckets_.all { it.isEmpty() }

    override fun iterator(): Iterator<T> = buckets_.asIterable().flatMap { bucket -> bucket.asSequence() }.iterator()

    override fun contains(element: T): Boolean = bucketFor(element).contains(element)

    override fun containsAll(elements: Collection<T>): Boolean {
        val peers = elements.toMutableSet()
        buckets_.forEach { bucket ->
            peers.removeAll(peers.filter { bucket.contains(it) }.toSet())
            if (peers.isEmpty()) {
                return true
            }
        }
        return false
    }

    fun add(node: T): T? = bucketFor(node).add(node)

    fun nearest(targetId: ByteString, count: Int): List<T> {
        val results = mutableListOf<T>()
        for (bucket in buckets_) {
            val bucketView = ArrayList(bucket)
            for (node in bucketView) {
                val nodeId = nodeId(node)
                results.orderedInsert(node) { a, _ -> targetId.xorDistCompare(nodeId(a), nodeId) }
                if (results.size > count) {
                    results.removeLast()
                }
            }
        }
        return results
    }

    fun logDistanceToSelf(node: T): Int {
        // TODO: cache distance
        return distanceToSelf(node)
    }

    private fun bucketFor(node: T) = buckets_[logDistanceToSelf(node)]
}

class KademliaBucket<T : Comparable<T>>(
    private val entries: MutableList<T>,
    private val k: Int,
    private val maxCandidates: Int = k,
    private val nodeId: (T) -> ByteString
) : Set<T> {
    constructor(k: Int, nodeId: (T) -> ByteString) : this(mutableListOf(), k, k, nodeId)

    private val candidates = mutableListOf<T>()

    override val size: Int
        get() = entries.size

    override fun isEmpty(): Boolean = entries.isEmpty()

    override fun contains(element: T): Boolean = entries.contains(element)

    override fun containsAll(elements: Collection<T>): Boolean = elements.containsAll(elements)

    override fun iterator(): Iterator<T> = entries.iterator()

    fun add(node: T): T? {
        candidates.remove(node)

        for (i in entries.indices) {
            if (nodeId(entries[i]) == nodeId(node)) {
                entries.sort()
                return node
            }
        }

        if (entries.size == k) {
            candidates.add(node)
            candidates.sort()
            if (candidates.size > maxCandidates) {
                candidates.removeLast()
            }
            return null
        }

        entries.add(node)
        entries.sort()
        return null
    }
}


fun <E : Comparable<E>> MutableList<E>.sortedAdd(element: E) {
    orderedInsert(element) { a, b -> a.compareTo(b) }
}

fun <E> MutableList<E>.orderedInsert(element: E, comparison: (E, E) -> Int) {
    var i = this.binarySearch { e -> comparison(e, element) }
    if (i < 0) {
        i = -i - 1
    }
    this.add(i, element)
}

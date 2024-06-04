package io.tonblocks.utils.concurrent

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class SynchronizedHashMap<K : Any, V : Any>(
    private val map: MutableMap<K, V> = HashMap()
) : ConcurrentHashMap<K, V>, SynchronizedObject() {
    constructor() : this(HashMap())
    constructor(initialCapacity: Int = 32) : this(HashMap(initialCapacity))

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = synchronized(this) { map.entries }
    override val keys: MutableSet<K>
        get() = synchronized(this) { map.keys }
    override val values: MutableCollection<V>
        get() = synchronized(this) { map.values }

    override val size: Int get() = synchronized(this) { map.size }

    override fun clear() = synchronized(this) { map.clear() }

    override fun put(key: K, value: V): V? = synchronized(this) {
        map.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) = synchronized(this) {
        map.putAll(from)
    }

    override fun remove(key: K): V? = synchronized(this) {
        map.remove(key)
    }

    override fun containsKey(key: K): Boolean = synchronized(this) {
        return map.containsKey(key)
    }

    override fun containsValue(value: V): Boolean = synchronized(this) {
        return map.containsValue(value)
    }

    override fun get(key: K): V? = synchronized(this) {
        return map[key]
    }

    override fun isEmpty(): Boolean = synchronized(this) {
        return map.isEmpty()
    }
}

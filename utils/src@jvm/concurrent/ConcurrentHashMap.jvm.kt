package io.tonblocks.utils.concurrent

actual fun <K : Any, V : Any> ConcurrentHashMap(initialSize: Int): ConcurrentHashMap<K, V> {
    return ConcurrentHashMapJvm(java.util.concurrent.ConcurrentHashMap<K, V>(initialSize))
}

private class ConcurrentHashMapJvm<K : Any, V : Any>(
    private val map: java.util.concurrent.ConcurrentHashMap<K, V>
) : ConcurrentHashMap<K, V> {
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = map.entries
    override val keys: MutableSet<K>
        get() = map.keys
    override val values: MutableCollection<V>
        get() = map.values
    override val size: Int
        get() = map.size

    override fun clear() {
        map.clear()
    }

    override fun put(key: K, value: V): V? {
        return map.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) {
        return map.putAll(from)
    }

    override fun remove(key: K): V? {
        return map.remove(key)
    }

    override fun containsKey(key: K): Boolean {
        return map.containsKey(key)
    }

    override fun containsValue(value: V): Boolean {
        return map.containsValue(value)
    }

    override fun get(key: K): V? {
        return map.get(key)
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }
}

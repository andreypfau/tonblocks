package io.tonblocks.kv

class MapKeyValueRepository<K, V>(
    private val map: MutableMap<K, V> = mutableMapOf()
) : KeyValueRepository<K, V> {
    override suspend fun containsKey(key: K): Boolean = map.containsKey(key)

    override suspend fun get(key: K): V? = map[key]

    override suspend fun put(key: K, value: V) {
        map[key] = value
    }

    override suspend fun remove(key: K) {
        map.remove(key)
    }

    override suspend fun keys(): Iterable<K> = map.keys

    override suspend fun clear() {
        map.clear()
    }

    override fun close() {}
}

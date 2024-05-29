package io.tonblocks.kv

/**
 * A key-value repository.
 */
@OptIn(ExperimentalStdlibApi::class)
interface KeyValueRepository<K, V> : AutoCloseable {
    /**
     * Returns true if the repository contains the key.
     *
     * @param key The key for the content.
     * @return true if an entry with the key exists in the store.
     */
    suspend fun containsKey(key: K): Boolean

    /**
     * Retrieves data from the repository.
     *
     * @param key The key for the content.
     * @return The stored data, or null if no data was stored under the specified key.
     */
    suspend fun get(key: K): V?

    /**
     * Puts data into the repository.
     *
     * @param key The key to associate with the data, for use when retrieving.
     * @param value The data to store.
     */
    suspend fun put(key: K, value: V)

    /**
     * Removes data from the repository.
     *
     * @param key The key to associate with the data, for use when retrieving.
     */
    suspend fun remove(key: K)

    /**
     * Returns the keys in the repository.
     *
     * @return An [Iterable] allowing iteration over the keys in the repository.
     */
    suspend fun keys(): Iterable<K>

    /**
     * Clears the repository.
     */
    suspend fun clear()
}

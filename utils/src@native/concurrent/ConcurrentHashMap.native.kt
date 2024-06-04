package io.tonblocks.utils.concurrent

actual fun <K : Any, V : Any> ConcurrentHashMap(initialCapacity: Int = 32): ConcurrentHashMap<K, V> {
    return SynchronizedHashMap(initialCapacity)
}

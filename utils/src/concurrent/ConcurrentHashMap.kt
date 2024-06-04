package io.tonblocks.utils.concurrent

interface ConcurrentHashMap<K : Any, V : Any> : MutableMap<K, V>

expect fun <K : Any, V : Any> ConcurrentHashMap(initialSize: Int = 32): ConcurrentHashMap<K, V>

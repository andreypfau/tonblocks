package io.tonblocks.utils.concurrent

interface ConcurrentHashMap<K : Any, V : Any> : MutableMap<K, V>

expect fun <K : Any, V : Any> ConcurrentHashMap(initialSize: Int = 32): ConcurrentHashMap<K, V>

interface ConcurrentHashSet<V : Any> : MutableSet<V>

fun <V : Any> ConcurrentHashMap(): ConcurrentHashSet<V> = ConcurrentHashSetImpl(ConcurrentHashMap<V, Unit>())

private class ConcurrentHashSetImpl<V : Any> private constructor(
    private val set: MutableSet<V>
) : ConcurrentHashSet<V>, MutableSet<V> by set {
    constructor(map: ConcurrentHashMap<V, Unit>) : this(map.keys)
}

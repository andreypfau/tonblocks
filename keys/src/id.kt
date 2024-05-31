package io.tonblocks.crypto

interface ShortId<T : Comparable<T>> : Comparable<T> {
    fun shortId(): T
}

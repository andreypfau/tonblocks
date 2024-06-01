package io.tonblocks.crypto

interface ShortId<T : Comparable<T>> : Comparable<ShortId<T>> {
    fun shortId(): T
}

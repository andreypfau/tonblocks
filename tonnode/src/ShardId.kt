package io.tonblocks.tonnode

import kotlin.jvm.JvmInline

typealias WorkchainId = Int

@JvmInline
value class ShardId(
    val value: ULong
) {
    constructor(shardPrefix: ULong, length: Int) : this((2uL * shardPrefix + 1uL) shl (63 - length)) {
        require(length <= 60) { "Invalid length, expected: 0..60, actual: $length" }
    }

    constructor(string: String) : this(string.toULong(16))

    fun length(): Int = if (value != 0uL) 63 - value.countTrailingZeroBits() else 0

    fun childRight(): ShardId = ShardId(value + (lowerBits64(value) shr 1))

    fun childLeft(): ShardId = ShardId(value - (lowerBits64(value) shr 1))

    fun parent(): ShardId {
        val x = lowerBits64(value)
        return ShardId((value - x) or (x shl 1))
    }

    operator fun contains(other: ShardId): Boolean {
        val x = lowerBits64(value)
        return ((value xor other.value) and (bitsNegative64(x) shl 1)) == 0uL
    }

    fun toLong(): Long = value.toLong()

    override fun toString(): String = value.toString(16).padStart(16, '0')

    companion object {
        val ROOT = ShardId(1uL shl 63)
    }
}

data class ShardIdFull(
    val workchain: WorkchainId,
    val shard: ShardId = ShardId.ROOT
) {
    init {
        check(workchain == BASECHAIN_ID || (workchain == MASTERCHAIN_ID && shard == ShardId.ROOT)) {
            "Invalid shard id: $this"
        }
    }

    fun parent(): ShardIdFull = ShardIdFull(workchain, shard.parent())

    fun childRight(): ShardIdFull = ShardIdFull(workchain, shard.childRight())

    fun childLeft(): ShardIdFull = ShardIdFull(workchain, shard.childLeft())

    operator fun contains(other: ShardId): Boolean = shard.contains(other)

    operator fun contains(other: ShardIdFull): Boolean {
        if (workchain != other.workchain) return false
        return shard.contains(other.shard)
    }

    override fun toString(): String = "$workchain:$shard"

    companion object {
        val MASTERCHAIN_ID: WorkchainId = -1
        val BASECHAIN_ID: WorkchainId = 0
    }
}

private fun bitsNegative64(x: ULong) = x.inv() + 1u
private fun lowerBits64(x: ULong) = x and bitsNegative64(x)

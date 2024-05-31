package io.tonblocks.tonnode

typealias WorkchainId = Int
typealias ShardId = ULong

data class ShardIdFull(
    val workchain: WorkchainId,
    val shard: ShardId = ROOT_SHARD_ID
) {
    init {
        check(workchain == BASECHAIN_ID || (workchain == MASTERCHAIN_ID && shard == ROOT_SHARD_ID)) {
            "Invalid shard id: $this"
        }
    }

    fun child(right: Boolean): ShardIdFull = ShardIdFull(workchain, childShard(shard, right))
    fun parent(): ShardIdFull = ShardIdFull(workchain, parentShard(shard))

    operator fun contains(child: ShardId): Boolean = containsShard(shard, child)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = "$workchain:${shard.toHexString().padStart(16, '0')}"

    companion object {
        val ROOT_SHARD_ID: ShardId = 1uL shl 63
        val MASTERCHAIN_ID: WorkchainId = -1
        val BASECHAIN_ID: WorkchainId = 0

        fun containsShard(parent: ShardId, child: ShardId): Boolean {
            val x = lowerBits64(parent)
            return ((parent xor child) and (bitsNegative64(x) shl 1)) == 0uL
        }

        fun childShard(shard: ShardId, right: Boolean): ShardId {
            val x = lowerBits64(shard) shr 1
            return if (right) shard + x else shard - x
        }

        fun parentShard(shard: ShardId): ShardId {
            val x = lowerBits64(shard)
            return (shard - x) or (x shl 1)
        }
    }
}

internal inline fun lowerBits64(x: ULong) = x and bitsNegative64(x)
internal inline fun bitsNegative64(x: ULong) = x.inv() + 1u

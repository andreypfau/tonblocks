package io.tonblocks.tonnode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShardTest {
    @Test
    fun testShardId() {
        val shard_4000000000000000 = ShardId(shardPrefix = 0u, length = 1)
        assertEquals(shard_4000000000000000, ShardId(0x4000000000000000uL))
        assertEquals(shard_4000000000000000, ShardId("4000000000000000"))
        assertEquals(shard_4000000000000000.toString(), "4000000000000000")
        assertEquals(shard_4000000000000000.length(), 1)
        assertEquals(shard_4000000000000000.parent(), ShardId.ROOT)

        val shard_c000000000000000 = ShardId(shardPrefix = 1u, length = 1)
        assertEquals(shard_c000000000000000, ShardId(0xc000000000000000uL))
        assertEquals(shard_c000000000000000, ShardId("c000000000000000"))
        assertEquals(shard_c000000000000000.toString(), "c000000000000000")
        assertEquals(shard_c000000000000000.length(), 1)
        assertEquals(shard_c000000000000000.parent(), ShardId.ROOT)

        assertEquals(ShardId.ROOT, ShardId("8000000000000000"))
        assertEquals(ShardId.ROOT, ShardId(0x8000000000000000uL))
        assertEquals(ShardId.ROOT.toString(), "8000000000000000")
        assertEquals(ShardId.ROOT.length(), 0)
        assertEquals(ShardId.ROOT.childLeft(), shard_4000000000000000)
        assertEquals(ShardId.ROOT.childRight(), shard_c000000000000000)
        assertTrue(ShardId.ROOT.contains(shard_4000000000000000))
        assertTrue(ShardId.ROOT.contains(shard_c000000000000000))
    }
}

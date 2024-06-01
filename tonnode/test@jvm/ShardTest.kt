package io.tonblocks.tonnode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShardTest {
    @Test
    fun testShardId() {
        val shard_4000000000000000 = ShardId(shardPrefix = 0u, length = 1)
        assertEquals(shard_4000000000000000, ShardId(0x4000000000000000u))
        assertEquals(shard_4000000000000000, ShardId("4000000000000000"))
        assertEquals("4000000000000000", shard_4000000000000000.toString())
        assertEquals(1, shard_4000000000000000.length())
        assertEquals(ShardId.ROOT, shard_4000000000000000.parent())

        val shard_c000000000000000 = ShardId(shardPrefix = 1u, length = 1)
        assertEquals(ShardId(0xc000000000000000u), shard_c000000000000000)
        assertEquals(shard_c000000000000000, ShardId("c000000000000000"))
        assertEquals("c000000000000000", shard_c000000000000000.toString())
        assertEquals(1, shard_c000000000000000.length())
        assertEquals(ShardId.ROOT, shard_c000000000000000.parent())

        assertEquals(ShardId.ROOT, ShardId("8000000000000000"))
        assertEquals(ShardId.ROOT, ShardId(0x8000000000000000u))
        assertEquals("8000000000000000", ShardId.ROOT.toString())
        assertEquals(0, ShardId.ROOT.length())
        assertEquals(shard_4000000000000000, ShardId.ROOT.childLeft())
        assertEquals(shard_c000000000000000, ShardId.ROOT.childRight())
        assertTrue(ShardId.ROOT.contains(shard_4000000000000000))
        assertTrue(ShardId.ROOT.contains(shard_c000000000000000))
    }
}

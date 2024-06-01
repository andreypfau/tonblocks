package io.tonblocks.tonnode

import kotlin.test.Test

class ShardTest {
    @Test
    fun testShard() {
        val shard = ShardId(shardPrefix = 0, length = 1)
        println(shard)
    }
}

package org.ton.catchain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CatChainOptions(
    val idleTimeout: Duration = 16.seconds,
    val maxDeps: Int = 4,
    val maxSerializedBlockSize: Int = 16 * 1024,
    val isBlockHashCoversData: Boolean = false,
    val maxBlockHeightCoefficient: Long = 0,
) {
    fun getMaxBlockHeight(sourcesCount: Int): Long {
        if (maxBlockHeightCoefficient == 0L) {
            return Long.MAX_VALUE
        }
        return maxBlockHeightCoefficient * (1 + (sourcesCount + maxDeps - 1) / maxDeps) / 1000
    }
}

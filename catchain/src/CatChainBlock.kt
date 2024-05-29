package org.ton.catchain

import kotlinx.io.bytestring.ByteString

class CatChainBlock<T>(
    val source: Int,
    val fork: Int,
    val sourceHash: ByteString,
    val height: CatChainBlockHeight,
    val hash: CatChainBlockHash,
    val payload: ByteString,
    val prev: CatChainBlock<T>?,
    val deps: List<CatChainBlock<T>>,
    val vt: List<CatChainBlockHeight>
) {
    var extra: T? = null
    var isProcessed = false
    var isPreprocessSent = false

    fun isDescendantOf(block: CatChainBlock<T>): Boolean {
        val fork = block.fork
        if (fork > vt.size) {
            return false
        }
        return block.height <= vt[fork]
    }

    fun blockSequence(filter: (CatChainBlock<T>)->Boolean): Sequence<CatChainBlock<T>> = sequence {
        if (!filter(this@CatChainBlock)) {
            return@sequence
        }
        prev?.blockSequence(filter)?.let {
            yieldAll(it)
        }
        deps.forEach {
            yieldAll(it.blockSequence(filter))
        }
        yield(this@CatChainBlock)
    }
}

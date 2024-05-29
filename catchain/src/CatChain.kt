package org.ton.catchain

import kotlinx.io.bytestring.ByteString
import org.ton.PublicKey
import org.ton.PublicKeyHash

class CatChain<T>(
    options: CatChainOptions,
    sourcesKeys: List<PublicKey>,
    val localId: PublicKeyHash
) : CatChainHandler {
    private val maxBlockHeight = options.getMaxBlockHeight(sourcesKeys.size)

    private val topBlocks = HashMap<CatChainBlockHash, CatChainBlock<T>>()
    private val blocks = HashMap<CatChainBlockHash, CatChainBlock<T>>()
    private val topSourceBlocks = arrayOfNulls<CatChainBlock<T>>(sourcesKeys.size)

    private val sourcesIds = Array(sourcesKeys.size) { sourcesKeys[it].hash() }
    private val localIdx = sourcesIds.indexOf(localId)
    private val blamedSources = BooleanArray(sourcesKeys.size)
    private val processDeps = ArrayList<CatChainBlockHash>()

    override fun onNewBlock(
        source: Int,
        fork: Int,
        hash: CatChainBlockHash,
        height: CatChainBlockHeight,
        prev: CatChainBlockHash?,
        deps: List<CatChainBlockHash>,
        vt: List<CatChainBlockHeight>,
        payload: ByteString
    ) {
        val p = prev?.let {
            topBlocks.remove(it)
            requireNotNull(blocks[it])
        }

        check(source < sourcesIds.size)
        val v = ArrayList<CatChainBlock<T>>(deps.size)
        for (i in deps.indices) {
            if (!blamedSources[source] && topBlocks.containsKey(deps[i])) {
                topBlocks.remove(deps[i])
            }
            v.add(requireNotNull(blocks[deps[i]]))
        }

        check(height <= maxBlockHeight)
        val srcHash = sourcesIds[source]
        val block = CatChainBlock(source, fork, srcHash, height, hash, payload, p, v, vt)
        blocks[hash] = block

        if (!blamedSources[source]) {
            block.blockSequence { !it.isPreprocessSent }
                .forEach { b ->
                    b.isPreprocessSent = true
                    preprocessBlock(b)
                }
            topSourceBlocks[source] = block
            topBlocks[hash] = block
        }
    }

    override fun onBlame(source: Int): Boolean {
        if (blamedSources[source]) {
            return false
        }
        blamedSources[source] = true
        topSourceBlocks[source] = null

        // recompute top blocks
        topBlocks.clear()
        for (i in sourcesIds.indices) {
            if (i == localIdx) continue
            if (blamedSources[i]) continue
            val block = topSourceBlocks[i] ?: continue
            if (block.isProcessed) continue
            var isTop = true
            for (j in sourcesIds.indices) {
                if (j == localIdx) continue
                if (blamedSources[j]) continue
                val b = topSourceBlocks[j] ?: continue
                if (b.isDescendantOf(block)) {
                    isTop = false
                    break
                }
            }
            if (isTop) {
                topBlocks[block.hash] = block
            }
        }

        var i = 0
        while (i < processDeps.size) {
            if (blocks[processDeps[i]]?.source == source) {
                processDeps[i] = processDeps.removeLast()
                i--
            }
            i++
        }

        return true
    }

    fun preprocessBlock(block: CatChainBlock<T>) {

    }

}

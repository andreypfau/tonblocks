package org.ton.catchain

import kotlinx.io.bytestring.ByteString

interface CatChainHandler {
    fun onNewBlock(
        source: Int,
        fork: Int,
        hash: CatChainBlockHash,
        height: CatChainBlockHeight,
        prev: CatChainBlockHash?,
        deps: List<CatChainBlockHash>,
        vt: List<CatChainBlockHeight>,
        payload: ByteString
    )

    fun onBlame(source: Int): Boolean
}

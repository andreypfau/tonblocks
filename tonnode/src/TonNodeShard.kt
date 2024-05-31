package io.tonblocks.tonnode

import io.github.andreypfau.kotlinx.crypto.sha256
import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.AdnlLocalNode
import io.tonblocks.overlay.Overlay
import io.tonblocks.overlay.OverlayIdFull
import io.tonblocks.overlay.OverlayImpl
import kotlinx.coroutines.*
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.encodeToByteArray
import tl.ton.tonnode.TonNodeShardPublicOverlayId
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

interface TonNodeShard : CoroutineScope {
    val id: ShardIdFull
    val zeroStateFileHash: ByteString
    val overlay: Overlay

    val maxNeighbours: Int get() = 16
}

fun OverlayIdFull(
    shardId: ShardIdFull,
    zeroStateFileHash: ByteString
): OverlayIdFull = OverlayIdFull(
    sha256(
        TL.Boxed.encodeToByteArray(
            TonNodeShardPublicOverlayId(
                workchain = shardId.workchain,
                shard = shardId.shard.toLong(),
                zeroStateFileHash = zeroStateFileHash
            )
        )
    )
)

class TonNodeShardImpl(
    val localNode: AdnlLocalNode,
    override val id: ShardIdFull,
    override val zeroStateFileHash: ByteString,
    coroutineContext: CoroutineContext = Dispatchers.Default
) : TonNodeShard {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = coroutineContext + job

    override val overlay = OverlayImpl(
        localNode = localNode,
        id = OverlayIdFull(id, zeroStateFileHash),
        coroutineContext = coroutineContext
    )

    private val reloadNeighboursJob = launch {
        while (true) {
            reloadNeighbours()
            delay(Random.nextInt(10_000, 30_000).milliseconds)
        }
    }

    private suspend fun reloadNeighbours() {
        overlay.randomPeers(maxNeighbours)
    }
}

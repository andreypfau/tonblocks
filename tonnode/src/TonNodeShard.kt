package io.tonblocks.tonnode

import io.github.andreypfau.kotlinx.crypto.sha256
import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.AdnlAddressResolver
import io.tonblocks.adnl.AdnlClient
import io.tonblocks.adnl.AdnlLocalNode
import io.tonblocks.overlay.AbstractOverlay
import io.tonblocks.overlay.Overlay
import io.tonblocks.overlay.OverlayIdFull
import io.tonblocks.overlay.OverlayNode
import kotlinx.coroutines.*
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.encodeToByteArray
import tl.ton.tonnode.TonNodeShardPublicOverlayId
import kotlin.coroutines.CoroutineContext

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
    val adnlAddressResolver: AdnlAddressResolver,
    override val id: ShardIdFull,
    override val zeroStateFileHash: ByteString,
    nodes: List<OverlayNode> = emptyList(),
    coroutineContext: CoroutineContext = Dispatchers.Default
) : TonNodeShard {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = coroutineContext + job

    override val overlay = object : AbstractOverlay(
        localNode = localNode,
        id = OverlayIdFull(id, zeroStateFileHash),
        isPublic = true,
        nodes = nodes,
        coroutineContext = coroutineContext
    ) {
        override val addressResolver: AdnlAddressResolver get() = this@TonNodeShardImpl.adnlAddressResolver

        override suspend fun receiveMessage(source: AdnlClient, data: Source) {

        }

        override suspend fun receiveQuery(
            source: AdnlClient,
            data: Source
        ): Buffer {
            TODO("Not yet implemented")
        }

        override suspend fun receiveBroadcast(source: ByteString, data: Source) {
        }
    }
}

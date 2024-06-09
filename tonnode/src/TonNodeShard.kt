package io.tonblocks.tonnode

import io.github.andreypfau.kotlinx.crypto.sha256
import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.overlay.Overlay
import io.tonblocks.overlay.OverlayIdFull
import io.tonblocks.overlay.OverlayLocalNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteString
import kotlinx.serialization.encodeToByteArray
import tl.ton.TonNodeShardPublicOverlayId
import kotlin.coroutines.CoroutineContext

interface TonNodeShard : CoroutineScope {
    val id: ShardIdFull
    val zeroStateFileHash: ByteString
    val overlay: Overlay
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
    val localNode: OverlayLocalNode,
    override val id: ShardIdFull,
    override val zeroStateFileHash: ByteString,
    coroutineContext: CoroutineContext = Dispatchers.Default
) : TonNodeShard {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = coroutineContext + job

    override val overlay = localNode.overlay(OverlayIdFull(id, zeroStateFileHash), true) {
        subscribeMessage { message ->
            println("$this message: ${message.readByteString()}")
        }
        subscribeQuery { query, answer ->
            println("$this query: ${query.readByteString()}")
        }
    }

    override fun toString(): String = "[$id]"
}

package io.tonblocks.overlay.broadcast

import io.github.andreypfau.kotlinx.crypto.sha256
import io.github.reactivecircus.cache4k.Cache
import io.tonblocks.adnl.AdnlIdFull
import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.ShortId
import io.tonblocks.overlay.Overlay
import io.tonblocks.overlay.OverlayConnection
import io.tonblocks.overlay.TlOverlayBroadcast
import io.tonblocks.overlay.TlOverlayBroadcastSimple
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface BroadcastHandler<B : Broadcast, T : TlOverlayBroadcast> {
    val incoming: ReceiveChannel<B>
    val outgoing: SendChannel<B>

    suspend fun handleBroadcast(connection: OverlayConnection, tl: T)

    operator fun get(id: ShortId<BroadcastIdShort>): B?
}

fun interface BroadcastDateValidator {
    fun check(date: Instant, now: Instant): BroadcastDateCheckResult

    companion object {
        fun keepAlive(duration: Duration): BroadcastDateValidator = object : BroadcastDateValidator {
            override fun check(date: Instant, now: Instant): BroadcastDateCheckResult {
                if (date < now - duration) {
                    return BroadcastDateCheckResult.FAILED_TOO_OLD
                }
                if (date > now + duration) {
                    return BroadcastDateCheckResult.FAILED_TOO_NEW
                }
                return BroadcastDateCheckResult.SUCCESS
            }
        }
    }
}

enum class BroadcastDateCheckResult {
    FAILED_TOO_OLD,
    FAILED_TOO_NEW,
    SUCCESS;

    fun isSuccess(): Boolean = this == SUCCESS
}

class SimpleBroadcastHandler(
    val overlay: Overlay,
    keepAliveDuration: Duration = 15.seconds,
) : BroadcastHandler<BroadcastSimple, TlOverlayBroadcastSimple> {
    private val dateValidator: BroadcastDateValidator = BroadcastDateValidator.keepAlive(keepAliveDuration)

    private val broadcasts = Cache.Builder<BroadcastIdShort, BroadcastSimple>()
        .expireAfterWrite(keepAliveDuration)
        .eventListener {
            println("$overlay broadcast $it")
        }
        .build()

    private val incomingChannel = Channel<BroadcastSimple>()
    override val incoming: ReceiveChannel<BroadcastSimple> get() = incomingChannel

    private val outgoingChannel = Channel<BroadcastSimple>()
    override val outgoing: SendChannel<BroadcastSimple> get() = outgoingChannel

    override fun get(id: ShortId<BroadcastIdShort>): BroadcastSimple? = broadcasts.get(id.shortId())

    override suspend fun handleBroadcast(
        sender: OverlayConnection,
        tl: TlOverlayBroadcastSimple
    ) {
        val date = Instant.fromEpochSeconds(tl.date.toLong())
        if (!dateValidator.check(date, Clock.System.now()).isSuccess()) {
            return
        }
        val dataHash = ByteString(*sha256(tl.data.toByteArray()))
        val source = AdnlIdFull(PublicKey(tl.src))
        val broadcastId = BroadcastIdFull(source, dataHash, tl.flags)
        val broadcast = broadcasts.get(broadcastId.shortId()) {
            BroadcastSimple(tl, broadcastId, date).also {
                it.addDelivered(sender.adnl.remotePeer.id)
                incomingChannel.send(it)
            }
        }
        broadcast.addDelivered(sender.adnl.remotePeer.id)
    }
}

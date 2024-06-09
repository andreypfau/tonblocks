package io.tonblocks.adnl.message

import io.tonblocks.adnl.query.AdnlQueryId
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString

sealed interface AdnlMessage {
    val serializedSize: Int

    fun tl(): tl.ton.AdnlMessage
}

interface AdnlMessageProcessor<T : AdnlMessage> {
    suspend fun processMessage(message: T)
}

fun AdnlMessage(tl: tl.ton.AdnlMessage): AdnlMessage {
    return when(tl) {
        is tl.ton.AdnlMessage.Query -> AdnlQueryMessage(
            queryId = tl.queryId,
            data = tl.query
        )

        is tl.ton.AdnlMessage.Answer -> AdnlMessageAnswer(
            queryId = tl.queryId,
            answer = tl.answer
        )

        is tl.ton.AdnlMessage.Custom -> AdnlCustomMessage(
            data = tl.data.toByteArray()
        )

        is tl.ton.AdnlMessage.ConfirmChannel -> AdnlConfirmChannelMessage(
            key = Ed25519.PublicKey(tl.key.toByteArray()),
            peerKey = Ed25519.PublicKey(tl.peerKey.toByteArray()),
            date = Instant.fromEpochSeconds(tl.date.toLong())
        )

        is tl.ton.AdnlMessage.CreateChannel -> AdnlCreateChannelMessage(
            key = Ed25519.PublicKey(tl.key.toByteArray()),
            date = Instant.fromEpochSeconds(tl.date.toLong())
        )

        is tl.ton.AdnlMessage.Nop -> AdnlNopMessage
        is tl.ton.AdnlMessage.Part -> AdnlPartMessage(
            hash = tl.hash,
            totalSize = tl.totalSize,
            offset = tl.offset,
            data = tl.data
        )

        is tl.ton.AdnlMessage.Reinit -> AdnlReinitMessage(
            date = Instant.fromEpochSeconds(tl.date.toLong())
        )
    }
}

class AdnlCreateChannelMessage(
    val key: Ed25519.PublicKey,
    val date: Instant
) : AdnlMessage {
    override val serializedSize: Int get() = 40

    override fun tl(): tl.ton.AdnlMessage = tl.ton.AdnlMessage.CreateChannel(
        key = ByteString(*key.toByteArray()),
        date = date.epochSeconds.toInt()
    )

    override fun toString(): String {
        return "AdnlMessageCreateChannel(key=$key, date=$date)"
    }
}

class AdnlConfirmChannelMessage(
    val key: Ed25519.PublicKey,
    val peerKey: Ed25519.PublicKey,
    val date: Instant
) : AdnlMessage {
    override val serializedSize: Int get() = 72

    override fun tl(): tl.ton.AdnlMessage {
        return tl.ton.AdnlMessage.ConfirmChannel(
            key = ByteString(*key.toByteArray()),
            peerKey = ByteString(*peerKey.toByteArray()),
            date = date.epochSeconds.toInt()
        )
    }

    override fun toString(): String = "AdnlMessageConfirmChannel(key=$key, peerKey=$peerKey, date=$date)"
}

class AdnlCustomMessage(
    data: ByteArray
) : AdnlMessage {
    private val _data = data
    val data: ByteArray get() = _data

    override val serializedSize: Int get() = data.size + 12

    override fun tl(): tl.ton.AdnlMessage {
        return tl.ton.AdnlMessage.Custom(
            data = ByteString(*data)
        )
    }
}

object AdnlNopMessage : AdnlMessage {
    override val serializedSize: Int get() = 4

    override fun tl(): tl.ton.AdnlMessage = tl.ton.AdnlMessage.Nop
}

class AdnlReinitMessage(
    val date: Instant
) : AdnlMessage {
    override val serializedSize: Int get() = 8

    override fun tl(): tl.ton.AdnlMessage = tl.ton.AdnlMessage.Reinit(
        date = date.epochSeconds.toInt()
    )
}

class AdnlQueryMessage(
    val queryId: AdnlQueryId,
    val data: ByteString
) : AdnlMessage {
    override val serializedSize: Int get() = data.size + 44

    override fun tl(): tl.ton.AdnlMessage = tl.ton.AdnlMessage.Query(
        queryId = queryId,
        query = data
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        return "AdnlMessageQuery(queryId=${queryId.toHexString()}, data=${data.toHexString()})"
    }
}

class AdnlMessageAnswer(
    val queryId: AdnlQueryId,
    val answer: ByteString
) : AdnlMessage {
    override val serializedSize: Int get() = answer.size + 44

    override fun tl(): tl.ton.AdnlMessage = tl.ton.AdnlMessage.Answer(
        queryId = queryId,
        answer = answer
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String =
        "AdnlMessageAnswer(queryId=${queryId.toHexString()}, answer=${answer.toHexString()})"
}

class AdnlPartMessage(
    val hash: ByteString,
    val totalSize: Int,
    val offset: Int,
    val data: ByteString
) : AdnlMessage {
    override val serializedSize: Int get() = data.size + 48

    override fun tl(): tl.ton.AdnlMessage {
        return tl.ton.AdnlMessage.Part(
            hash = hash,
            totalSize = totalSize,
            offset = offset,
            data = data
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String =
        "AdnlMessagePart(hash=$hash, totalSize=$totalSize, offset=$offset, data=${data.toHexString()})"
}

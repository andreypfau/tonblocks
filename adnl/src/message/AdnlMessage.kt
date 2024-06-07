package io.tonblocks.adnl.message

import io.tonblocks.adnl.query.AdnlQueryId
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString

sealed interface AdnlMessage {
    val size: Int

    fun tl(): tl.ton.AdnlMessage
}

fun AdnlMessage(tl: tl.ton.AdnlMessage): AdnlMessage {
    return when(tl) {
        is tl.ton.AdnlMessage.Query -> AdnlMessageQuery(
            queryId = tl.queryId,
            data = tl.query.toByteArray()
        )

        is tl.ton.AdnlMessage.Answer -> AdnlMessageAnswer(
            queryId = tl.queryId,
            answer = tl.answer.toByteArray()
        )

        is tl.ton.AdnlMessage.Custom -> AdnlMessageCustom(
            data = tl.data.toByteArray()
        )

        is tl.ton.AdnlMessage.ConfirmChannel -> AdnlMessageConfirmChannel(
            key = Ed25519.PublicKey(tl.key.toByteArray()),
            peerKey = Ed25519.PublicKey(tl.peerKey.toByteArray()),
            date = Instant.fromEpochSeconds(tl.date.toLong())
        )

        is tl.ton.AdnlMessage.CreateChannel -> AdnlMessageCreateChannel(
            key = Ed25519.PublicKey(tl.key.toByteArray()),
            date = Instant.fromEpochSeconds(tl.date.toLong())
        )

        is tl.ton.AdnlMessage.Nop -> AdnlMessageNop
        is tl.ton.AdnlMessage.Part -> AdnlMessagePart(
            hash = tl.hash,
            totalSize = tl.totalSize,
            offset = tl.offset,
            data = tl.data.toByteArray()
        )

        is tl.ton.AdnlMessage.Reinit -> AdnlMessageReinit(
            date = Instant.fromEpochSeconds(tl.date.toLong())
        )
    }
}

class AdnlMessageCreateChannel(
    val key: Ed25519.PublicKey,
    val date: Instant
) : AdnlMessage {
    override val size: Int get() = 40

    override fun tl(): tl.ton.AdnlMessage = tl.ton.AdnlMessage.CreateChannel(
        key = ByteString(*key.toByteArray()),
        date = date.epochSeconds.toInt()
    )

    override fun toString(): String {
        return "AdnlMessageCreateChannel(key=$key, date=$date)"
    }
}

class AdnlMessageConfirmChannel(
    val key: Ed25519.PublicKey,
    val peerKey: Ed25519.PublicKey,
    val date: Instant
) : AdnlMessage {
    override val size: Int get() = 72

    override fun tl(): tl.ton.AdnlMessage {
        return tl.ton.AdnlMessage.ConfirmChannel(
            key = ByteString(*key.toByteArray()),
            peerKey = ByteString(*peerKey.toByteArray()),
            date = date.epochSeconds.toInt()
        )
    }

    override fun toString(): String = "AdnlMessageConfirmChannel(key=$key, peerKey=$peerKey, date=$date)"
}

class AdnlMessageCustom(
    data: ByteArray
) : AdnlMessage {
    private val _data = data
    val data: ByteArray get() = _data

    override val size: Int get() = data.size + 12

    override fun tl(): tl.ton.AdnlMessage {
        return tl.ton.AdnlMessage.Custom(
            data = ByteString(*data)
        )
    }
}

object AdnlMessageNop : AdnlMessage {
    override val size: Int get() = 4

    override fun tl(): tl.ton.AdnlMessage = tl.ton.AdnlMessage.Nop
}

class AdnlMessageReinit(
    val date: Instant
) : AdnlMessage {
    override val size: Int get() = 8

    override fun tl(): tl.ton.AdnlMessage = tl.ton.AdnlMessage.Reinit(
        date = date.epochSeconds.toInt()
    )
}

class AdnlMessageQuery(
    val queryId: AdnlQueryId,
    val data: ByteArray
) : AdnlMessage {
    override val size: Int get() = data.size + 44

    override fun tl(): tl.ton.AdnlMessage = tl.ton.AdnlMessage.Query(
        queryId = queryId,
        query = ByteString(*data)
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        return "AdnlMessageQuery(queryId=${queryId.toHexString()}, data=${data.toHexString()})"
    }
}

class AdnlMessageAnswer(
    val queryId: AdnlQueryId,
    val answer: ByteArray
) : AdnlMessage {
    override val size: Int get() = answer.size + 44

    override fun tl(): tl.ton.AdnlMessage = tl.ton.AdnlMessage.Answer(
        queryId = queryId,
        answer = ByteString(*answer)
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String =
        "AdnlMessageAnswer(queryId=${queryId.toHexString()}, answer=${answer.toHexString()})"
}

class AdnlMessagePart(
    val hash: ByteString,
    val totalSize: Int,
    val offset: Int,
    val data: ByteArray
) : AdnlMessage {
    override val size: Int get() = data.size + 48

    override fun tl(): tl.ton.AdnlMessage {
        return tl.ton.AdnlMessage.Part(
            hash = hash,
            totalSize = totalSize,
            offset = offset,
            data = ByteString(*data)
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String =
        "AdnlMessagePart(hash=$hash, totalSize=$totalSize, offset=$offset, data=${data.toHexString()})"
}

package io.tonblocks.adnl

import io.tonblocks.adnl.channel.AdnlChannel
import io.tonblocks.adnl.message.*
import io.tonblocks.crypto.ed25519.Ed25519
import io.tonblocks.utils.internal.logging.CommonLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.CoroutineContext

class AdnlPeerPair(
    val peer: AdnlPeer,
    val node: AdnlNode,
    val transport: AdnlTransport,
    private val channelKey: Ed25519.PrivateKey = Ed25519.random()
) : CoroutineScope {
    private val logger = CommonLogger.logger("PeerPair ${peer.id}")

    private var reinitDate = Instant.DISTANT_PAST
    internal var channel: AdnlChannel? = null
    private val recvCounter = AdnlPacketCounter(peer.reinitDate)
    private val sendCounter = AdnlPacketCounter()

    private val partMessageProcessor = AdnlPartMessageProcessor()

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = peer.coroutineContext + job

    init {
        launch {
            while (true) {
                processPacket(transport.receive())
            }
        }
    }

    private suspend fun processPacket(packet: AdnlPacket) {
        val seqno = packet.seqno
        if (seqno != null) {
            recvCounter.seqno = seqno
            logger.trace { "recv packet, seqno S${sendCounter.seqno}, R${recvCounter.seqno} : $packet" }
        }
        val confirmSeqno = packet.confirmSeqno
        if (confirmSeqno != null) {
            val lastSeqno = sendCounter.seqno
            if (confirmSeqno > lastSeqno) {
                logger.warn { "too new ADNL packet seqno confirmation: $confirmSeqno, expected <= $lastSeqno" }
                return
            }
        }
        if (packet.messages.isEmpty()) {
            return
        }

        packet.messages.forEach { message ->
            runCatching {
                processMessage(message)
            }.exceptionOrNull()?.let {
                logger.error(it) { "Error processing message: $message" }
            }
        }
    }

    private suspend fun processMessage(message: AdnlMessage) {
        when (message) {
            is AdnlCreateChannelMessage -> processCreateChannelMessage(message)
            is AdnlConfirmChannelMessage -> processConfirmChannelMessage(message)
            is AdnlCustomMessage -> processCustomMessage(message)
            is AdnlNopMessage -> {}
            is AdnlReinitMessage -> reinit(message.date)
            is AdnlQueryMessage -> processQueryMessage(message)
            is AdnlMessageAnswer -> processAnswerMessage(message)
            is AdnlPartMessage -> partMessageProcessor.processMessage(message)
        }
    }

    private suspend fun processCreateChannelMessage(message: AdnlCreateChannelMessage) {
        channel = AdnlChannel.create(null, this, channelKey, message.key, message.date)
        sendPacket(null, AdnlConfirmChannelMessage(channelKey.publicKey(), message.key, message.date))
    }

    private fun processConfirmChannelMessage(message: AdnlConfirmChannelMessage) {
        if (message.peerKey != channelKey.publicKey()) {
            logger.warn { "Bad peer key in confirm channel message: ${message}, expected: ${channelKey.publicKey()}" }
            return
        }
        if (message.date != channel?.date) {
            channel = AdnlChannel.create(null, this, channelKey, message.key, message.date)
        }
    }

    private fun processCustomMessage(message: AdnlCustomMessage) {

    }

    private fun processQueryMessage(message: AdnlQueryMessage) {

    }

    private fun processAnswerMessage(message: AdnlMessageAnswer) {

    }

    suspend fun sendMessage(message: AdnlMessage) {
        val channel = channel
        val createChannelMsg = if (channel == null) {
            val pubKey = channelKey.publicKey()
            AdnlCreateChannelMessage(
                key = pubKey,
                date = Clock.System.now()
            )
        } else {
            null
        }
        var size = createChannelMsg?.serializedSize ?: 0
        size += message.serializedSize
        if (size <= MAX_ADNL_MESSAGE) {
            if (createChannelMsg != null) {
                logger.trace { "Send with message: $createChannelMsg" }
                sendPacket(channel, createChannelMsg, message)
            } else {
                sendPacket(channel, message)
            }
        } else {
            TODO("Too big message: ${message.serializedSize}")
        }
    }

    private suspend fun sendPacket(channel: AdnlChannel?, vararg messages: AdnlMessage) {
        val packet = AdnlPacket()
        packet.addMessages(*messages)
        packet.seqno = sendCounter.nextSeqno()
        packet.confirmSeqno = recvCounter.seqno
        if (channel == null) {
            packet.addressList = node.addressList
            packet.reinitDate(recvCounter.reinitDate, sendCounter.reinitDate)
            packet.sign(node.key)
        }
        transport.send(packet)
    }

    fun reinit(reinitDate: Instant) {

    }
}

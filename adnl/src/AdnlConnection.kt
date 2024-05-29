package io.tonblocks.adnl

import io.github.andreypfau.tl.serialization.TL
import io.ktor.utils.io.core.*
import io.tonblocks.adnl.channel.AdnlChannel
import io.tonblocks.adnl.message.*
import io.tonblocks.adnl.query.AdnlQueryId
import io.tonblocks.adnl.transport.AdnlTransport
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteArray
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import tl.ton.adnl.AdnlPacketContents
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

const val PACKET_HEADER_BYTES = 272
const val CHANNEL_PACKET_HEADER_BYTES = 128
const val MAX_ADNL_MESSAGE = 1024

abstract class AdnlConnection(
    val transport: AdnlTransport,
    addressList: AdnlAddressList,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob(transport.coroutineContext.job)

    abstract val localNode: AdnlLocalNode
    abstract val remotePeer: AdnlPeer
    private val channelKey: Ed25519.PrivateKey = Ed25519.random()

    val localId get() = localNode.id.shortId()
    val remoteId get() = remotePeer.id.shortId()
    val recvCounter = AdnlPacketCounter(transport.reinitDate)
    val sendCounter = AdnlPacketCounter()
    private var addressList = addressList

    var channel: AdnlChannel? by atomic(null)
        private set

    private val queries = mutableMapOf<AdnlQueryId, CompletableDeferred<Buffer>>()

    internal suspend fun handlePacket(data: ByteReadPacket) {
        println("[$localId] Handling packet from $remoteId")
        val channel = channel
        val decryptor = channel?.input?.decryptor ?: localNode.key.createDecryptor()
        val decrypted = decryptor.decryptToByteArray(data.readBytes())
        val packet = AdnlPacket(tl = TL.Boxed.decodeFromByteArray<AdnlPacketContents>(decrypted))

        if (channel == null) {
            if (!packet.checkSignature()) {
                println("$remoteId: bad ADNL packet signature")
                return
            }
        }

        val seqno = packet.seqno
        if (seqno != null) {
            recvCounter.seqno = seqno
            println("recv packet $remoteId -> $localId, seqno S${sendCounter.seqno}, R${recvCounter.seqno}")
        }

        val confirmSeqno = packet.confirmSeqno
        if (confirmSeqno != null) {
            val lastSeqno = sendCounter.seqno
            if (confirmSeqno > lastSeqno) {
                println("$remoteId: too new ADNL packet seqno confirmation: $confirmSeqno, expected <= $lastSeqno")
                return
            }
        }

        if (packet.messages.isEmpty()) {
            println("$remoteId: empty ADNL packet")
            return
        }

        packet.messages.forEach { message ->
            handleMessage(message)
        }
    }

    suspend fun handleMessage(message: AdnlMessage) {
        println("Process message: $message")
        when (message) {
            is AdnlMessagePart -> {
                println("skip big messages")
            }

            is AdnlMessageCreateChannel -> {
                channel = AdnlChannel.create(channelKey, message.key, message.date)
                sendPacket(null, AdnlMessageConfirmChannel(channelKey.publicKey(), message.key, message.date))
            }

            is AdnlMessageConfirmChannel -> {
                if (message.peerKey != channelKey.publicKey()) {
                    println("Bad peer key in confirm channel message: ${message.peerKey}, expected: ${channelKey.publicKey()}")
                    return
                }
                channel = AdnlChannel.create(channelKey, message.peerKey, message.date)
            }

            is AdnlMessageAnswer -> {
                val queryId = message.queryId
                val deferred = queries.remove(queryId) ?: run {
                    println("Unknown query id: $queryId")
                    return
                }
                deferred.complete(Buffer().apply {
                    write(message.answer)
                })
            }

            is AdnlMessageCustom -> {
                handleCustom(Buffer().apply {
                    write(message.data)
                })
            }

            is AdnlMessageQuery -> {
                handleQuery(message.queryId, Buffer().apply {
                    write(message.data)
                })
            }

            is AdnlMessageReinit -> reinit(message.date)
            AdnlMessageNop -> {}
        }
    }

    suspend fun sendMessage(message: AdnlMessage) {
        val channel = channel
        val createChannelMsg = if (channel == null) {
            println("Create channel $localId -> $remoteId")
            val pubKey = channelKey.publicKey()
            AdnlMessageCreateChannel(
                key = pubKey,
                date = Clock.System.now()
            )
        } else {
            null
        }
        var size = createChannelMsg?.size ?: 0
        size += message.size
        if (size <= MAX_ADNL_MESSAGE) {
            if (createChannelMsg != null) {
                println("Send with message: $createChannelMsg")
                sendPacket(channel, createChannelMsg, message)
            } else {
                sendPacket(channel, message)
            }
        } else {
            TODO("Too big message: ${message.size}")
        }
    }

    open suspend fun sendQuery(query: Source): Source {
        val queryId = ByteString(*Random.nextBytes(32))
        val deferred = CompletableDeferred<Buffer>()
        deferred.invokeOnCompletion { queries.remove(queryId) }
        queries[queryId] = deferred
        sendMessage(AdnlMessageQuery(queryId, query.readByteArray()))
        return deferred.await()
    }

    open suspend fun sendCustom(data: Source) {
        sendMessage(AdnlMessageCustom(data.readByteArray()))
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun sendPacket(channel: AdnlChannel?, vararg messages: AdnlMessage) {
        val packet = AdnlPacket()
        packet.addMessages(*messages)
        packet.seqno = sendCounter.nextSeqno()
        packet.confirmSeqno = recvCounter.seqno
        if (channel == null) {
            packet.addressList = localNode.addressList
            packet.reinitDate(recvCounter.reinitDate, sendCounter.reinitDate)
            packet.sign(localNode.key)
        }
        val data = TL.Boxed.encodeToByteArray(packet.tl())
        val encryptor = channel?.output?.encryptor ?: remotePeer.id.publicKey.createEncryptor()
        val encrypted = encryptor.encryptToByteArray(data)
        println("send packet $localId -> $remoteId, seqno S${sendCounter.seqno}, R${recvCounter.seqno}")
        transport.sendDatagram(remoteId, addressList.random(), ByteReadPacket(encrypted))
    }

    abstract suspend fun handleCustom(data: Source)

    abstract suspend fun handleQuery(queryId: AdnlQueryId, data: Source)

    private fun updateAddrList(addressList: AdnlAddressList) {
        if (addressList.isEmpty()) return
        if (addressList.reinitDate > Clock.System.now() + 60.seconds) return
        if (addressList.reinitDate < this.addressList.reinitDate) return
        this.addressList = addressList
    }

    private fun reinit(date: Instant) {
        val oldReinitDate = sendCounter.reinitDate
        if (oldReinitDate < date) {
            sendCounter.reinitDate = date
            sendCounter.seqno = 0
            recvCounter.seqno = 0
            channel = null
        }
    }
}

fun CoroutineScope.AdnlConnection(
    transport: AdnlTransport,
    localKey: Ed25519.PrivateKey,
    remoteKey: Ed25519.PublicKey,
    addressList: AdnlAddressList,
    handleCustom: suspend (data: Source) -> Unit = {},
    handleQuery: suspend (queryId: AdnlQueryId, data: Source) -> Unit = { _, _ -> },
): AdnlConnection {
    return object : AdnlConnection(transport, addressList) {
        override val localNode: AdnlLocalNode = AdnlLocalNode(
            localKey, AdnlAddressList(
                reinitDate = transport.reinitDate,
                version = Clock.System.now().epochSeconds.toInt()
            )
        )
        override val remotePeer: AdnlPeer = AdnlPeer(remoteKey)

        init {
            transport.handle { _, _, datagram ->
                handlePacket(datagram)
            }
        }

        override suspend fun handleCustom(data: Source) {
            handleCustom(data)
        }

        override suspend fun handleQuery(queryId: AdnlQueryId, data: Source) {
            handleQuery(queryId, data)
        }
    }
}

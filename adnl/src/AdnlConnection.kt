package io.tonblocks.adnl

import io.github.andreypfau.tl.serialization.TL
import io.github.reactivecircus.cache4k.Cache
import io.ktor.utils.io.core.*
import io.tonblocks.adnl.channel.AdnlChannel
import io.tonblocks.adnl.message.*
import io.tonblocks.adnl.query.AdnlQueryId
import io.tonblocks.adnl.transport.AdnlTransport
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteArray
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

const val PACKET_HEADER_BYTES = 272
const val CHANNEL_PACKET_HEADER_BYTES = 128
const val MAX_ADNL_MESSAGE = 1024

abstract class AdnlConnection(
    val transport: AdnlTransport,
    addressList: AdnlAddressList,
    coroutineContext: CoroutineContext
) : CoroutineScope, AdnlClient {
    private val job = Job()
    override val coroutineContext: CoroutineContext = coroutineContext + job

    abstract val localNode: AdnlLocalNode
    abstract val remotePeer: AdnlPeer
    private val channelKey: Ed25519.PrivateKey = Ed25519.random()

    val recvCounter = AdnlPacketCounter(transport.reinitDate)
    val sendCounter = AdnlPacketCounter()
    private var addressList = addressList
    private val transfers = Cache.Builder<ByteString, AdnlPartMessageTransfer>()
        .expireAfterWrite(5.seconds)
        .maximumCacheSize(10)
        .build()

    internal var channel = atomic<AdnlChannel?>(null)

    private val queries = mutableMapOf<AdnlQueryId, CompletableDeferred<Buffer>>()

    suspend fun handlePacket(packet: AdnlPacket) {
        val seqno = packet.seqno
        if (seqno != null) {
            recvCounter.seqno = seqno
//            println("$this recv packet, seqno S${sendCounter.seqno}, R${recvCounter.seqno} : ${packet}")
        }
        val confirmSeqno = packet.confirmSeqno
        if (confirmSeqno != null) {
            val lastSeqno = sendCounter.seqno
            if (confirmSeqno > lastSeqno) {
                println("$this too new ADNL packet seqno confirmation: $confirmSeqno, expected <= $lastSeqno")
                return
            }
        }

        packet.messages.forEach { message ->
            handleMessage(message)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun handleMessage(message: AdnlMessage) {
//        println("$this Process message: $message")
        when (message) {
            is AdnlMessagePart -> {
                val transferId = message.hash
                val transfer = transfers.get(transferId) {
                    AdnlPartMessageTransfer(
                        hash = transferId,
                        totalSize = message.totalSize,
                        updatedAt = Clock.System.now()
                    )
                }
                if (transfer.update(message)) {
//                    println("$this added part: ${message.offset}@${message.hash}")
                }
                if (transfer.isComplete() && transfer.isValid() && transfer.delivered.compareAndSet(false, true)) {
//                    println("$this Got data: ${ByteString(transfer.data)}")
                    val newMessage = AdnlMessage(TL.decodeFromByteArray<tl.ton.AdnlMessage>(transfer.data))
//                    println("$this Got new reassembled message: $newMessage")
                    handleMessage(newMessage)
                }
            }

            is AdnlMessageCreateChannel -> {
                channel.update {
                    AdnlChannel.create(this, channelKey, message.key, message.date)
                }
                sendPacket(null, AdnlMessageConfirmChannel(channelKey.publicKey(), message.key, message.date))
            }

            is AdnlMessageConfirmChannel -> {
                if (message.peerKey != channelKey.publicKey()) {
                    println("$this Bad peer key in confirm channel message: ${message}, expected: ${channelKey.publicKey()}")
                    return
                }
                if (message.date != channel.value?.date) {
                    channel.update {
                        AdnlChannel.create(this, channelKey, message.key, message.date)
                    }
//                    println("$this Setup channel: ${channel.value}")
                }
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
                val data = Buffer().apply {
                    write(message.data)
                }
                localNode.handleMessage(this, data)
            }

            is AdnlMessageQuery -> {
                val query = Buffer().apply {
                    write(message.data)
                }
                val answer = Buffer()
                localNode.handleQuery(this, query, answer)
                sendMessage(AdnlMessageAnswer(message.queryId, answer.readByteArray()))
            }
            is AdnlMessageReinit -> reinit(message.date)
            AdnlMessageNop -> {}
        }
    }

    suspend fun sendMessage(message: AdnlMessage) {
        val channel = channel.value
        val createChannelMsg = if (channel == null) {
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
//                println("$this Send with message: $createChannelMsg")
                sendPacket(channel, createChannelMsg, message)
            } else {
                sendPacket(channel, message)
            }
        } else {
            TODO("Too big message: ${message.size}")
        }
    }

    override suspend fun sendQuery(data: Source): Source {
        val queryId = ByteString(*Random.nextBytes(32))
        val deferred = CompletableDeferred<Buffer>()
        deferred.invokeOnCompletion { queries.remove(queryId) }
        queries[queryId] = deferred
        sendMessage(AdnlMessageQuery(queryId, data.readByteArray()))
        return deferred.await()
    }

    override suspend fun sendMessage(data: Source) {
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
//        println("$this send packet, seqno S${sendCounter.seqno}, R${recvCounter.seqno} | $channel")
        val destinationId = channel?.output?.id ?: remotePeer.id.shortId()
        transport.sendDatagram(destinationId, addressList.random(), ByteReadPacket(encrypted))
    }

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
            channel.update {
                null
            }
        }
    }

    override fun toString(): String = "[->${remotePeer.id.shortId()}]"
}

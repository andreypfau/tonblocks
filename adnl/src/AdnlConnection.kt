package io.tonblocks.adnl

import io.github.andreypfau.tl.serialization.TL
import io.github.reactivecircus.cache4k.Cache
import io.ktor.utils.io.core.*
import io.tonblocks.adnl.channel.AdnlChannel
import io.tonblocks.adnl.message.*
import io.tonblocks.adnl.query.AdnlQueryId
import io.tonblocks.adnl.transport.AdnlTransport
import io.tonblocks.crypto.Decryptor
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
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
    private val transfers = Cache.Builder<ByteString, AdnlPartMessageTransfer>()
        .expireAfterWrite(5.seconds)
        .maximumCacheSize(10)
        .build()

    internal var channel = atomic<AdnlChannel?>(null)

    private val queries = mutableMapOf<AdnlQueryId, CompletableDeferred<Buffer>>()

    internal suspend fun handlePacket(dest: AdnlNodeIdShort, address: AdnlAddress, data: ByteReadPacket) {
        if (data.remaining < 32) {
            return
        }
        val channel = channel.value
        val decryptor: Decryptor
        if (channel != null) {
            println("$this w  channel checking $data - $dest | ${channel.input.id}")
            if (channel.input.id != dest.publicKeyHash) {
                return
            }
            decryptor = channel.input.decryptor
        } else {
            println("$this w/o channel checking $data - $dest | $localId")
            if (localId != dest) {
                return
            }
            decryptor = localNode.key.createDecryptor()
        }

        println("$this Handling packet (${data} bytes) for $localId decryptor:$decryptor ${if (channel != null) "using channel: $channel" else ""}")
        val readData = data.copy().readBytes()
        var decrypted: ByteArray
        try {
            decrypted = decryptor.decryptToByteArray(readData)
        } catch (e: Exception) {
            return
        }
        data.discard(readData.size)

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
            println("$this recv packet, seqno S${sendCounter.seqno}, R${recvCounter.seqno} : ${packet.tl()}")
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

        return
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun handleMessage(message: AdnlMessage) {
        println("Process message: $message")
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
                    println("$this added part: ${message.offset}@${message.hash}")
                }
                if (transfer.isComplete() && transfer.isValid() && transfer.delivered.compareAndSet(false, true)) {
                    println("Got data: ${ByteString(transfer.data)}")
                    val newMessage = AdnlMessage(TL.decodeFromByteArray<tl.ton.adnl.AdnlMessage>(transfer.data))
                    println("Got new reassembled message: $newMessage")
                    handleMessage(newMessage)
                }
            }

            is AdnlMessageCreateChannel -> {
                channel.update {
                    AdnlChannel.create(channelKey, message.key, message.date)
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
                        AdnlChannel.create(channelKey, message.key, message.date)
                    }
                    println("Setup channel: ${channel.value}")
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
                println("$this Send with message: $createChannelMsg")
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
        println("$this send packet, seqno S${sendCounter.seqno}, R${recvCounter.seqno} | $channel")
        val destinationId = channel?.output?.id?.let { AdnlNodeIdShort(it) } ?: remoteId
        transport.sendDatagram(destinationId, addressList.random(), ByteReadPacket(encrypted))
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
            channel.update {
                null
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = "[$remoteId@${hashCode().toHexString()}]"
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
            transport.handle { dst, address, datagram ->
                try {
                    handlePacket(dst, address, datagram)
                } catch (e: Exception) {
                    println("Failed process $this datagram: ${e.stackTraceToString()}")
                }
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

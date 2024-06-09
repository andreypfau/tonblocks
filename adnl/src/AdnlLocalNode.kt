package io.tonblocks.adnl

import io.github.andreypfau.tl.serialization.TL
import io.github.reactivecircus.cache4k.Cache
import io.ktor.utils.io.core.*
import io.tonblocks.adnl.channel.AdnlChannel
import io.tonblocks.adnl.transport.AdnlNetworkTransport
import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.datetime.Clock
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.serialization.decodeFromByteArray
import tl.ton.AdnlNode
import tl.ton.AdnlPacketContents
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

typealias AdnlMessageCallback = suspend (AdnlConnection).(message: Source) -> Unit
typealias AdnlQueryCallback = suspend (AdnlConnection).(query: Source, answer: Sink) -> Unit

class AdnlLocalNode(
    val transport: AdnlNetworkTransport,
    val key: Ed25519.PrivateKey,
    addressList: AdnlAddressList = AdnlAddressList(
        reinitDate = transport.reinitDate,
        version = Clock.System.now().epochSeconds.toInt()
    )
) : CoroutineScope {
    val id = AdnlIdFull(key.publicKey())

    private val job = SupervisorJob(transport.coroutineContext.job)
    private val connections = Cache.Builder<AdnlIdShort, AdnlConnection>()
        .expireAfterAccess(15.minutes)
        .build()
    private val channels = Cache.Builder<AdnlIdShort, AdnlChannel>()
        .expireAfterAccess(15.minutes)
        .build()
    private val addressList_ = atomic(addressList)
    private val messageCallbacks = ArrayList<AdnlMessageCallback>()
    private val queryCallbacks = ArrayList<AdnlQueryCallback>()

    val addressList by addressList_

    override val coroutineContext: CoroutineContext = job

    init {
        updateAddressList(addressList)
        transport.handle { dest, address, datagram ->
            val channel = channel(dest)
            if (channel != null) {
                channel.handleDatagram(datagram)
                return@handle
            }
            if (dest == id.shortId()) {
                handleDatagram(address, datagram)
                return@handle
            }
            println("Unknown destination: $dest")
        }
    }

    fun updateAddressList(addressList: AdnlAddressList) {
        val newAddressList = addressList.copy(
            reinitDate = transport.reinitDate,
            version = Clock.System.now().epochSeconds.toInt()
        )
        addressList_.update {
            newAddressList
        }
        println("$this updated addr list. New version set to ${newAddressList.version}")
    }

    fun tl(): AdnlNode {
        return AdnlNode(
            id = key.publicKey().tl(),
            addrList = addressList.tl()
        )
    }

    suspend fun handleDatagram(
        address: AdnlAddress,
        datagram: ByteReadPacket,
    ) {
        val decrypted = key.createDecryptor().decryptToByteArray(datagram.readBytes())
        val packet = AdnlPacket(tl = TL.Boxed.decodeFromByteArray<AdnlPacketContents>(decrypted))

        val sourceId = packet.sourceShort ?: packet.source?.shortId()
        if (sourceId == null) {
            println("$this Bad packet, unknown source")
            return
        }
        var connection = connection(sourceId)
        if (connection == null) {
            println("$this New connection: $sourceId")
            val source = packet.source
            val publicKey = source?.publicKey as? Ed25519.PublicKey
            if (publicKey == null) {
                println("$this Bad packet, invalid public key from $sourceId: $source")
                return
            }
            if (!packet.checkSignature()) {
                println("$this Bad packet, invalid signature from $sourceId")
                return
            }
            var addressList = packet.addressList
            if (addressList.isNullOrEmpty()) {
                addressList = AdnlAddressList(address)
            }
            connection = connection(source, addressList)
        }
//        println("$this process packet (S${packet.confirmSeqno}|R${packet.seqno}) for $connection")
        connection.handlePacket(packet)
    }

    fun channel(id: AdnlIdShort): AdnlChannel? {
        var channel = channels.get(id)
        if (channel != null) {
            return channel
        }
        channel = connections.asMap().values.find {
            it.channel.value?.input?.id == id
        }?.channel?.value
        if (channel != null) {
            channels.put(id, channel)
        }
        return channel
    }

    fun connection(id: AdnlIdShort): AdnlConnection? {
        return connections.get(id)
    }

    suspend fun connection(id: AdnlIdFull, resolver: AdnlAddressResolver): AdnlConnection? {
        val shortId = id.shortId()
        var connection = connections.get(shortId)
        if (connection != null) {
            return connection
        }
        val addresses = resolver.resolveAddress(shortId) ?: return null
        connection = createConnection(id, addresses)
        connections.put(shortId, connection)
        return connection
    }

    suspend fun connection(publicKey: PublicKey, addressList: AdnlAddressList): AdnlConnection =
        connection(AdnlIdFull(publicKey), addressList)

    suspend fun connection(id: AdnlIdFull, addressList: AdnlAddressList): AdnlConnection {
        return connections.get(id.shortId()) {
            createConnection(id, addressList)
        }
    }

    suspend fun handleMessage(connection: AdnlConnection, data: Source) {
        val iterator = messageCallbacks.iterator()
        while (!data.exhausted() && iterator.hasNext()) {
            iterator.next().invoke(connection, data)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun handleQuery(connection: AdnlConnection, query: Source, answer: Sink) {
        val iterator = queryCallbacks.iterator()
        while (!query.exhausted() && iterator.hasNext()) {
            iterator.next().invoke(connection, query, answer)
        }
        if (!query.exhausted()) {
            val firstInt = query.readInt()
            throw IllegalStateException(
                "Dropping query $connection, first int: $firstInt (${
                    firstInt.toUInt().toHexString()
                })"
            )
        }
    }

    fun subscribeMessage(messageCallback: AdnlMessageCallback) {
        messageCallbacks.add(messageCallback)
    }

    fun subscribeQuery(queryCallback: AdnlQueryCallback) {
        queryCallbacks.add(queryCallback)
    }

    fun removeConnection(id: AdnlIdShort) {
        connection(id)?.channel?.value?.input?.id?.let {
            channels.invalidate(it)
        }
        connections.invalidate(id)
    }

    private fun createConnection(
        id: AdnlIdFull,
        addressList: AdnlAddressList = AdnlAddressList()
    ): AdnlConnection {
        val connection = object : AdnlConnection(transport, addressList, coroutineContext) {
            override val localNode: AdnlLocalNode = this@AdnlLocalNode
            override val remotePeerId = id
        }
        connection.coroutineContext.job.invokeOnCompletion {
            connections.invalidate(id.shortId())
            connection.channel.value?.input?.id?.let {
                channels.invalidate(it)
            }
        }
        return connection
    }

    override fun toString(): String {
        return "[LocalNode ${id.shortId()}]"
    }


}

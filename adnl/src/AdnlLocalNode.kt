package io.tonblocks.adnl

import io.github.andreypfau.tl.serialization.TL
import io.ktor.utils.io.core.*
import io.tonblocks.adnl.query.AdnlQueryId
import io.tonblocks.adnl.transport.AdnlTransport
import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.io.Source
import kotlinx.serialization.decodeFromByteArray
import tl.ton.adnl.AdnlPacketContents
import kotlin.coroutines.CoroutineContext

class AdnlLocalNode(
    val transport: AdnlTransport,
    val key: Ed25519.PrivateKey,
    addressList: AdnlAddressList = AdnlAddressList(
        reinitDate = transport.reinitDate,
        version = Clock.System.now().epochSeconds.toInt()
    )
) : CoroutineScope {
    val id = AdnlIdFull(key.publicKey())

    private val job = SupervisorJob(transport.coroutineContext.job)
    private val connections = HashMap<AdnlIdShort, AdnlConnection>()
    private val addressList_ = atomic(addressList)
    private val mx = Mutex()

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

    fun tl(): tl.ton.adnl.AdnlNode {
        return tl.ton.adnl.AdnlNode(
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

    suspend fun channel(id: AdnlIdShort) = mx.withLock {
        connections.values.find {
            it.channel.value?.input?.id == id
        }?.channel?.value
    }

    suspend fun connection(id: AdnlIdShort): AdnlConnection? {
        return mx.withLock {
            connections[id]
        }
    }

    suspend fun connection(publicKey: PublicKey, addressList: AdnlAddressList): AdnlConnection =
        connection(AdnlIdFull(publicKey), addressList)

    suspend fun connection(id: AdnlIdFull, addressList: AdnlAddressList): AdnlConnection {
        return mx.withLock {
            connections.getOrPut(id.shortId()) {
                createConnection(id, addressList)
            }
        }
    }

    suspend fun removeConnection(id: AdnlIdShort) {
        mx.withLock {
            connections.remove(id)
        }
    }

    private fun createConnection(
        id: AdnlIdFull,
        addressList: AdnlAddressList = AdnlAddressList()
    ): AdnlConnection {
        val connection = object : AdnlConnection(transport, addressList, coroutineContext) {
            override val localNode: AdnlLocalNode = this@AdnlLocalNode
            override val remotePeer: AdnlPeer = AdnlPeer(id)

            override suspend fun handleCustom(data: Source) {
            }

            override suspend fun handleQuery(queryId: AdnlQueryId, data: Source) {
            }
        }
        connection.coroutineContext.job.invokeOnCompletion {
            connections.remove(id.shortId())
        }
        return connection
    }

    override fun toString(): String {
        return "[LocalNode ${id.shortId()}]"
    }
}

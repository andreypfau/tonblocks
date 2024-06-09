package io.tonblocks.adnl

import io.ktor.util.collections.*
import io.tonblocks.adnl.channel.AdnlChannel
import io.tonblocks.crypto.ShortId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.coroutines.CoroutineContext

class AdnlPeerTable(
    val addressResolver: AdnlAddressResolver,
    val addressRepository: AdnlAddressRepository
) : CoroutineScope, AdnlAddressRepository by addressRepository {
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    private val channels = ConcurrentMap<AdnlIdShort, AdnlChannel>()
    private val peers = ConcurrentMap<AdnlIdShort, AdnlPeer>()

    fun addPeer(id: AdnlIdFull, addressList: AdnlAddressList): AdnlPeer {
        val shortId = id.shortId()
        val peer = peers.getOrPut(shortId) {
            AdnlPeer(shortId, this, addressList)
        }
        peer.updatePublicKey(id.publicKey)
        return peer
    }

    fun addPeer(id: ShortId<AdnlIdShort>, addressList: AdnlAddressList? = null): AdnlPeer {
        val shortId = id.shortId()
        return peers.getOrPut(shortId) {
            AdnlPeer(shortId, this, addressList)
        }
    }

    fun unregisterChannel(id: AdnlIdShort) {
        channels.remove(id)
    }

    fun registerChannel(channel: AdnlChannel) {
        channels[channel.input.id] = channel
    }

    fun getChannel(destination: AdnlIdShort): AdnlChannel? {
        return channels[destination]
    }
}

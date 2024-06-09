package io.tonblocks.adnl

import io.github.reactivecircus.cache4k.Cache
import io.tonblocks.crypto.PublicKey
import io.tonblocks.crypto.ShortId
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class AdnlPeer(
    val id: AdnlIdShort,
    val peerTable: AdnlPeerTable,
    addressList: AdnlAddressList?
) : ShortId<AdnlIdShort> by id, CoroutineScope {
    val reinitDate = Clock.System.now()
    private val deferred = CompletableDeferred<PublicKey>()
    private val peerPairs = Cache.Builder<AdnlIdShort, AdnlPeerPair>()
        .expireAfterAccess(5.minutes)
        .build()

    var addressList = addressList ?: AdnlAddressList()
        private set

    private val job = Job()
    override val coroutineContext: CoroutineContext = peerTable.coroutineContext + job

    private val discoverJob = launch {
        while (true) {
            try {
                if (this@AdnlPeer.addressList.isEmpty()) {
                    discover()
                }
            } catch (e: Exception) {
                println("$this ${e.message}")
            }
            delay(Random.nextInt(60_000, 120_000).milliseconds)
        }
    }

    fun updatePublicKey(publicKey: PublicKey) {
        if (deferred.isCompleted) {
            return
        }
        check(publicKey.hash() == id.publicKeyHash) {
            "Invalid public key hash, expected: ${id.publicKeyHash}, actual: ${publicKey.hash()}"
        }
        deferred.complete(publicKey)
    }

    suspend fun peerPair(localNode: AdnlNode): AdnlPeerPair {
        val peerPair = peerPairs.get(localNode.id.shortId()) {
            AdnlPeerPair(this, localNode)
        }
        return peerPair
    }

    suspend fun awaitPublicKey(): PublicKey = deferred.await()

    override fun toString(): String = "[peer $id]"

    private suspend fun discover() {
        println("$this discover address...")
        val addressList = requireNotNull(peerTable.addressResolver.resolveAddress(id)) {
            "$this Failed to resolve address"
        }
        updateAddressList(addressList)
    }

    private fun updateAddressList(addressList: AdnlAddressList) {
        this.addressList = addressList
        peerPairs.asMap().values.forEach { peerPair ->
            peerPair.reinit(addressList.reinitDate)
        }
    }
}

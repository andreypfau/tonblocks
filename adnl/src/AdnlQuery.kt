package io.tonblocks.adnl

import io.tonblocks.adnl.query.AdnlQueryId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.io.bytestring.ByteString

class AdnlQuery(
    val peerPair: AdnlPeerPair,
    val queryId: AdnlQueryId,
    val request: ByteString
) {
    private val deferred = CompletableDeferred<ByteString>()

    fun response(data: ByteString) {
        deferred.complete(data)
    }

    suspend fun awaitResponse(): ByteString = deferred.await()
}

package io.tonblocks.adnl

import io.github.andreypfau.tl.serialization.TL
import io.github.andreypfau.tl.serialization.decodeFromSource
import io.github.andreypfau.tl.serialization.encodeToSink
import kotlinx.io.Buffer
import kotlinx.io.Source

interface AdnlClient {
    suspend fun sendQuery(data: Source): Source

    suspend fun sendMessage(data: Source)
}

suspend inline fun <reified Q : Any, reified A : Any> AdnlClient.sendTlQuery(
    query: Q
): A {
    val buffer = Buffer()
    TL.Boxed.encodeToSink(buffer, query)
    val answer = sendQuery(buffer)
    return TL.Boxed.decodeFromSource(answer)
}

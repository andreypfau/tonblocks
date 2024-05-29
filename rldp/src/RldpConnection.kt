package org.ton.rldp

import kotlinx.coroutines.*
import org.ton.adnl.AdnlNodeIdShort
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

class RldpConnection(
    val rldp: Rldp,
    val source: AdnlNodeIdShort,
    val destination: AdnlNodeIdShort,
) : DisposableHandle {
    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                try {
                    process()
                } catch (e: CancellationException) {
                    // ignore
                } catch (cause: Exception) {
                    cause.printStackTrace()
                }
            }
        }
    }

    override fun dispose() {

    }

    private suspend fun process() {

    }
}

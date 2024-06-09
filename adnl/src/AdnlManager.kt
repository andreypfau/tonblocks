package io.tonblocks.adnl

import io.tonblocks.crypto.ShortId
import kotlinx.io.Source

class AdnlManager {

    suspend fun receiveMessage(src: ShortId<AdnlIdShort>, dest: ShortId<AdnlIdShort>, data: Source) {

    }

    fun subscribe(dest: ShortId<AdnlIdShort>, messageCallback: AdnlMessageCallback) {

    }
}

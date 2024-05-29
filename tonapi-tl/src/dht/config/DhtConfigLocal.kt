// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton.dht.config

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlin.jvm.JvmName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import tl.ton.adnl.id.AdnlIdShort

@Serializable
@JsonClassDiscriminator("@type")
public sealed interface DhtConfigLocal {
    @Serializable
    @SerialName("dht.config.local")
    @TLCombinatorId(0x76204A6F)
    public data class Local(
        @get:JvmName("id")
        public val id: AdnlIdShort,
    ) : DhtConfigLocal {
        public companion object
    }

    @Serializable
    @SerialName("dht.config.random.local")
    @TLCombinatorId(0x9BEB2577)
    public data class RandomLocal(
        @get:JvmName("cnt")
        public val cnt: Int,
    ) : DhtConfigLocal {
        public companion object
    }

    public companion object
}

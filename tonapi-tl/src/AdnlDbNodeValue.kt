// This file is generated by TLGenerator.kt
// Do not edit manually!
package tl.ton

import io.github.andreypfau.tl.serialization.TLCombinatorId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

@Serializable
@SerialName("adnl.db.node.value")
@TLCombinatorId(0x545D2707)
public data class AdnlDbNodeValue(
    @get:JvmName("date")
    public val date: Int,
    @get:JvmName("id")
    public val id: PublicKey,
    @SerialName("addr_list")
    @get:JvmName("addrList")
    public val addrList: AdnlAddressList,
    @SerialName("priority_addr_list")
    @get:JvmName("priorityAddrList")
    public val priorityAddrList: AdnlAddressList,
) {
    public companion object
}
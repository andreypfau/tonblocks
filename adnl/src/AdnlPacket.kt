package io.tonblocks.adnl

import io.github.andreypfau.tl.serialization.TL
import io.tonblocks.adnl.message.AdnlMessage
import io.tonblocks.crypto.ed25519.Ed25519
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString
import tl.ton.AdnlPacketContents
import kotlin.random.Random

class AdnlPacket internal constructor(
    var rand1: ByteArray = Random.nextBytes(Random.nextInt(7, 15)),
    var rand2: ByteArray = Random.nextBytes(Random.nextInt(7, 15)),
    flags: Int = 0,
    signature: ByteArray? = null,
    sourceShort: AdnlIdShort? = null,
    source: AdnlIdFull? = null,
    addressList: AdnlAddressList? = null,
    priorityAddressList: AdnlAddressList? = null,
    seqno: Long? = null,
    confirmSeqno: Long? = null,
    recvAddressListVersion: Int? = null,
    recvPriorityAddressListVersion: Int? = null,
    reinitDate: Instant? = null,
    dstReinitDate: Instant? = null,
) {
    constructor() : this(flags = 0)

    constructor(tl: AdnlPacketContents) : this(
        rand1 = tl.rand1.toByteArray(),
        rand2 = tl.rand2.toByteArray(),
        flags = tl.flags,
        signature = tl.signature?.toByteArray() ?: ByteArray(0),
        sourceShort = tl.fromShort?.let { AdnlIdShort(it) },
        source = tl.from?.let { AdnlIdFull(it) },
        addressList = tl.address?.let { AdnlAddressList(it) },
        priorityAddressList = tl.address?.let { AdnlAddressList(it) },
        seqno = tl.seqno,
        confirmSeqno = tl.confirmSeqno,
        recvAddressListVersion = tl.recvAddrListVersion,
        recvPriorityAddressListVersion = tl.recvPriorityAddrListVersion,
        reinitDate = tl.reinitDate?.let { Instant.fromEpochSeconds(it.toLong()) },
        dstReinitDate = tl.dstReinitDate?.let { Instant.fromEpochSeconds(it.toLong()) },
    ) {
        val tlMessage = tl.message
        if (tlMessage != null) {
            addMessages(AdnlMessage(tlMessage))
        }
        val tlMessages = tl.messages
        if (tlMessages != null) {
            addMessages(*tlMessages.map { AdnlMessage(it) }.toTypedArray())
        }
    }

    private var _messages: MutableList<AdnlMessage> = ArrayList()

    var addressList: AdnlAddressList? = addressList
        set(value) {
            field = value
            flags = if (value != null) {
                flags or FLAG_ADDRESS
            } else {
                flags and FLAG_ADDRESS.inv()
            }
        }

    var priorityAddressList: AdnlAddressList? = priorityAddressList
        set(value) {
            field = value
            flags = if (value != null) {
                flags or FLAG_PRIORITY_ADDRESS
            } else {
                flags and FLAG_PRIORITY_ADDRESS.inv()
            }
        }

    var seqno: Long? = seqno
        set(value) {
            field = value
            flags = if (value != null) {
                flags or FLAG_SEQNO
            } else {
                flags and FLAG_SEQNO.inv()
            }
        }

    var confirmSeqno: Long? = confirmSeqno
        set(value) {
            field = value
            flags = if (value != null) {
                flags or FLAG_CONFIRM_SEQNO
            } else {
                flags and FLAG_CONFIRM_SEQNO.inv()
            }
        }

    var recvAddressListVersion: Int? = recvAddressListVersion
        set(value) {
            field = value
            flags = if (value != null) {
                flags or FLAG_RECV_ADDR_VERSION
            } else {
                flags and FLAG_RECV_ADDR_VERSION.inv()
            }
        }

    var recvPriorityAddressListVersion: Int? = recvPriorityAddressListVersion
        set(value) {
            field = value
            flags = if (value != null) {
                flags or FLAG_RECV_PRIORITY_ADDR_VERSION
            } else {
                flags and FLAG_RECV_PRIORITY_ADDR_VERSION.inv()
            }
        }

    val messages: List<AdnlMessage> get() = _messages

    var flags: Int = flags
        private set

    var signature: ByteArray? = signature
        private set(value) {
            field = value
            if (value != null) {
                flags = flags or FLAG_SIGNATURE
            } else {
                flags = flags and FLAG_SIGNATURE.inv()
            }
        }

    var sourceShort: AdnlIdShort? = sourceShort
        set(value) {
            field = value
            flags = if (value != null) {
                flags or FLAG_FROM_SHORT
            } else {
                flags and FLAG_FROM_SHORT.inv()
            }
        }

    var source: AdnlIdFull? = source
        set(value) {
            field = value
            flags = if (value != null) {
                sourceShort = value.shortId()
                flags or FLAG_FROM
            } else {
                sourceShort = null
                flags and FLAG_FROM.inv()
            }
        }

    var reinitDate: Instant? = reinitDate
        set(value) {
            field = value
            flags = flags or FLAG_REINIT_DATE
        }

    private var dstReinitDate: Instant? = dstReinitDate
        set(value) {
            field = value
        }

    fun reinitDate(date: Instant, destinationReinitDate: Instant) {
        reinitDate = date
        dstReinitDate = destinationReinitDate
        flags = flags or FLAG_REINIT_DATE
    }

    fun addMessages(vararg messages: AdnlMessage) {
        _messages.addAll(messages)
        flags = if (_messages.size == 1) {
            (flags or FLAG_ONE_MESSAGE) and FLAG_MULTIPLE_MESSAGES.inv()
        } else {
            (flags or FLAG_MULTIPLE_MESSAGES) and FLAG_ONE_MESSAGE.inv()
        }
    }

    operator fun plusAssign(message: AdnlMessage) {
        this.addMessages(message)
    }

    fun tl(): AdnlPacketContents = AdnlPacketContents(
        rand1 = ByteString(*rand1),
        flags = flags,
        from = source?.publicKey?.tl(),
        fromShort = sourceShort?.tl(),
        message = if (_messages.size == 1) _messages.firstOrNull()?.tl() else null,
        messages = if (_messages.size > 1) _messages.map { it.tl() } else null,
        address = addressList?.tl(),
        priorityAddress = priorityAddressList?.tl(),
        seqno = seqno,
        confirmSeqno = confirmSeqno,
        recvAddrListVersion = recvAddressListVersion,
        recvPriorityAddrListVersion = recvPriorityAddressListVersion,
        reinitDate = reinitDate?.epochSeconds?.toInt(),
        dstReinitDate = dstReinitDate?.epochSeconds?.toInt(),
        signature = signature?.let { ByteString(*it) },
        rand2 = ByteString(*rand2)
    )

    fun sign(key: Ed25519.PrivateKey) {
        signature = null
        source = AdnlIdFull(key.publicKey())
        val raw = TL.Boxed.encodeToByteArray(AdnlPacketContents.serializer(), tl())
        signature = key.createDecryptor().sign(raw)
    }

    // TODO: fix check
    fun checkSignature(): Boolean {
//        val signature = signature ?: return false
//        val raw = TL.Boxed.encodeToByteArray(
//            AdnlPacketContents.serializer(),
//            tl().copy(signature = null, flags = flags and FLAG_SIGNATURE.inv())
//        )
//        return source?.publicKey?.createEncryptor()?.checkSignature(raw, signature) ?: false
        return true
    }

    override fun toString(): String {
        return buildString {
            append("AdnlPacket(")
            append("seqno=")
            append(seqno)
            append(", confirmSeqno=")
            append(confirmSeqno)
            append(", messages=")
            append(_messages)
            append(", addressList=")
            append(addressList)
            append(", priorityAddressList=")
            append(priorityAddressList)
            append(", recvAddressListVersion=")
            append(recvAddressListVersion)
            append(", recvPriorityAddressListVersion=")
            append(recvPriorityAddressListVersion)
            append(", flags=")
            append(flags)
            append(", reinitDate=")
            append(reinitDate)
            append(", dstReinitDate=")
            append(dstReinitDate)
            append(", sourceShort=")
            append(sourceShort)
            append(", source=")
            append(source)
            append(", signature=")
            append(signature?.contentToString())
            append(")")
        }
    }


    companion object {
        const val FLAG_FROM = 0x1
        const val FLAG_FROM_SHORT = 0x2
        const val FLAG_ONE_MESSAGE = 0x4
        const val FLAG_MULTIPLE_MESSAGES = 0x8
        const val FLAG_ADDRESS = 0x10
        const val FLAG_PRIORITY_ADDRESS = 0x20
        const val FLAG_SEQNO = 0x40
        const val FLAG_CONFIRM_SEQNO = 0x80
        const val FLAG_RECV_ADDR_VERSION = 0x100
        const val FLAG_RECV_PRIORITY_ADDR_VERSION = 0x200
        const val FLAG_REINIT_DATE = 0x400
        const val FLAG_SIGNATURE = 0x800
        const val FLAG_PRIORITY = 0x1000
        const val FLAG_ALL = 0x1fff
    }
}

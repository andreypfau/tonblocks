package io.tonblocks.overlay

import io.tonblocks.crypto.PublicKey
import kotlinx.datetime.Instant
import kotlinx.io.bytestring.ByteString

sealed class OverlayCertificate {
    abstract fun tl(): TlOverlayCertificate

    data class V1(
        val issuedBy: PublicKey,
        val expireAt: Instant,
        val maxSize: Int,
        val signature: ByteString
    ) : OverlayCertificate() {
        constructor(tl: TlOverlayCertificateV1) : this(
            PublicKey(tl.issuedBy),
            Instant.fromEpochSeconds(tl.expireAt.toLong()),
            tl.maxSize,
            tl.signature
        )

        override fun tl(): TlOverlayCertificateV1 = TlOverlayCertificateV1(
            issuedBy = issuedBy.tl(),
            expireAt = expireAt.epochSeconds.toInt(),
            maxSize = maxSize,
            signature = signature
        )
    }

    data class V2(
        val issuedBy: PublicKey,
        val expireAt: Instant,
        val maxSize: Int,
        val flags: Int,
        val signature: ByteString
    ) : OverlayCertificate() {
        constructor(tl: TlOverlayCertificateV2) : this(
            PublicKey(tl.issuedBy),
            Instant.fromEpochSeconds(tl.expireAt.toLong()),
            tl.maxSize,
            tl.flags,
            tl.signature
        )

        override fun tl(): TlOverlayCertificateV2 = TlOverlayCertificateV2(
            issuedBy = issuedBy.tl(),
            expireAt = expireAt.epochSeconds.toInt(),
            maxSize = maxSize,
            flags = flags,
            signature = signature
        )
    }

    data object Empty : OverlayCertificate() {
        override fun tl(): TlOverlayCertificateEmpty = TlOverlayCertificateEmpty
    }

    companion object {
        operator fun invoke(tl: TlOverlayCertificate): OverlayCertificate = when (tl) {
            is TlOverlayCertificateV1 -> invoke(tl)
            is TlOverlayCertificateV2 -> invoke(tl)
            is TlOverlayCertificateEmpty -> invoke(tl)
        }

        operator fun invoke(tl: TlOverlayCertificateV1): V1 = V1(tl)

        operator fun invoke(tl: TlOverlayCertificateV2): V2 = V2(tl)

        operator fun invoke(tl: TlOverlayCertificateEmpty): Empty = Empty
    }
}

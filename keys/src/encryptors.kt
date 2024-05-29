package io.tonblocks.crypto

interface Encryptor {
    fun encryptToByteArray(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): ByteArray

    fun encryptIntoByteArray(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = source.size
    )

    fun checkSignature(message: ByteArray, signature: ByteArray): Boolean
}



interface Decryptor {
    fun decryptToByteArray(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): ByteArray

    fun decryptIntoByteArray(
        source: ByteArray,
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = source.size
    )

    fun sign(message: ByteArray): ByteArray
}

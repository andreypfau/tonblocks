package io.github.andreypfau.kton.cell

interface Cell {
    suspend fun load(): DataCell

    fun hash(): ByteArray
}

data class CellDescriptor(
    val d1: Byte,
    val d2: Byte
) {
    val refsCount get() = d1.toInt() and REFERENCE_COUNT_MASK
    val isExotic get() = d1.toInt() and IS_EXOTIC_MASK != 0
    val hasHashes get() = d1.toInt() and HAS_HASHES_MASK != 0
    val levelMask get() = d1.toInt() and LEVEL_MASK shr 5
    val isAligned: Boolean
        get() = d2.toInt() and 1 == 0
    val dataLength: Int
        get() {
            val d2 = d2.toInt() and 0xFF
            return (d2 and 1) + (d2 ushr 1)
        }

    companion object {
        public const val LEVEL_MASK: Int = 0b1110_0000
        public const val HAS_HASHES_MASK: Int = 0b0001_0000
        public const val IS_EXOTIC_MASK: Int = 0b0000_1000
        public const val REFERENCE_COUNT_MASK: Int = 0b0000_0111
    }
}

class DataCell(
    val descriptor: CellDescriptor,
    val data: ByteArray,
    val dataOffset: Int,
    val refs: List<Cell>
) : Cell {
    override suspend fun load() = this

    fun encodeIntoByteArray(
        destination: ByteArray,
        destinationOffset: Int = 0
    ): Int {
        var pos = destinationOffset
        destination[pos++] = descriptor.d1
        destination[pos++] = descriptor.d2
        data.copyInto(destination, pos, dataOffset, dataOffset+descriptor.dataLength)
        pos += descriptor.dataLength
        for (ref in refs) {
            val hash = ref.hash()
            hash.copyInto(destination, pos)
            pos += hash.size
        }
        return pos + data.size
    }

    fun encodeIntoByteArray(): ByteArray {
        val size = descriptor.dataLength + refs.size
        val result = ByteArray(size)
        encodeIntoByteArray(result)
        return result
    }

    override fun hash(): ByteArray {
        TODO()
    }
}

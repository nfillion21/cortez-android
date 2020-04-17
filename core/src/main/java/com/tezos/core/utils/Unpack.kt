package com.tezos.core.utils

import java.io.InputStream
import java.nio.charset.Charset

data class Unpacker(val input: InputStream, var byteReadCount: Int = 0) {

    fun unpack(): Visitable {
        return when (val tag = read()) {
            0x00 -> VisitableLong(parseLong())
            0x01 -> VisitableString(parseString())
            0x02 -> VisitableSequence(parseSequence())
            in 0x03..0x09 -> parsePrimitive(tag)
            0x0a -> VisitableBytes(parseBytes())
            else -> throw DataFormatException("Unknown data type")
        }
    }

    private fun parsePrimitive(tag: Int): Visitable {
        return Primitive(
            Primitive.Name.valueOf(read()),
            when (tag) {
                0x05, 0x06 -> arrayOf(unpack()) // 1 argument
                0x07, 0x08 -> arrayOf(unpack(), unpack()) // 2 arguments
                0x9 -> parseSequence() // > 2 arguments
                else -> null
            },
            when (tag) {
                0x04, 0x06, 0x08 -> parseString()
                0x09 -> tryToParseString()
                else -> null
            }
        )
    }

    private fun parseLong(): Long {
        var byte = read()
        val sign = byte and 0x40 != 0 // true if negative

        var magnitude: Long = (byte and 0x3f).toLong()
        var shift = 6
        while ((byte and 0x80) != 0) {
            if (shift >= Long.SIZE_BITS) {
                throw IntegerFormatException("Integer overflow")
            }
            byte = read()
            magnitude = ((byte and 0x7f).toLong() shl shift) or magnitude
            shift += 7
        }

        return if (sign) -magnitude else magnitude
    }

    private fun tryToParseString(): String? {
        val size = parseSize()
        return if (size == 0) null else parseString(size)
    }

    private fun parseString(): String {
        return parseString(parseSize())
    }

    private fun parseString(size: Int): String {
        val bytes = ByteArray(size)
        read(bytes)
        return bytes.toString(Charset.defaultCharset())
    }

    private fun parseSequence(): Array<out Visitable> {
        val size = parseSize()
        val mark = byteReadCount
        val sequence = mutableListOf<Visitable>()
        while (size > (byteReadCount - mark)) {
            sequence += unpack()
        }
        return sequence.toTypedArray()
    }

    private fun parseBytes(): ByteArray {
        val bytes = ByteArray(parseSize())
        read(bytes)
        return bytes
    }

    private fun parseSize(): Int {
        return (read() shl 24) or (read() shl 16) or (read() shl 8) or read()
    }

    private fun read(): Int {
        byteReadCount += 1
        return input.read()
    }

    private fun read(bytes: ByteArray): Int {
        val count = input.read(bytes)
        byteReadCount += count
        return count
    }
}

class DataFormatException : IllegalArgumentException {
    constructor(message: String?) : super(message)
}

class IntegerFormatException : java.lang.IllegalArgumentException {
    constructor(message: String?) : super(message)
}
package com.tezos.core

import com.tezos.core.crypto.Base58
import org.bitcoinj.core.AddressFormatException
import kotlin.ByteArray
import kotlin.math.absoluteValue

data class Pack(val data: ByteArray) {
    companion object {}
}

fun Pack.Companion.prim(value: Pack): Pack {
    return Pack(byteArrayOf(0x05) + value.data)
}

fun Int.toByteArray(): ByteArray {
    return byteArrayOf(
        (this shr 24).toByte(), (this shr 16).toByte(), (this shr 8).toByte(), this.toByte()
    )
}

fun Pack.Companion.long(value: Long): Pack {
    if (value == 0L) {
        return Pack(byteArrayOf(0x00, 0x00))
    }

    var v = value.absoluteValue

    var byte = v and 0x3f
    v = v shr 6
    if (value < 0) {
        byte = byte or 0x40
    }
    if (v > 0) {
        byte = byte or 0x80
    }

    var data = byteArrayOf(0x00, byte.toByte())

    while (v > 0) {
        byte = v and 0x7f
        v = v shr 7
        if (v > 0) {
            byte = byte or 0x80
        }
        data += byte.toByte()
    }

    return Pack(data)
}
fun Pack.Companion.int(value: Int): Pack {
    return long(value.toLong())
}
fun Pack.Companion.mutez(value: Long): Pack {
    return long(value)
}

fun Pack.Companion.string(value: String): Pack {
    val data = value.toByteArray(Charsets.UTF_8)
    return Pack(byteArrayOf(0x01) + data.size.toByteArray() + data)
}

@Throws(AddressFormatException::class)
private fun Pack.Companion.address(string: String, prefix: ByteArray = byteArrayOf()): Pack {
    var data = Base58.decode(string).slice(3 until (27 - 4)).toByteArray() // remove prefix & checksum
    data = when(string.slice(0 until 3)) {
        "tz1" -> byteArrayOf(0x00) + data
        "tz2" -> byteArrayOf(0x01) + data
        "tz3" -> byteArrayOf(0x02) + data
        else -> throw AddressFormatException("Unknown address prefix")
    }
    data = prefix + data
    return Pack(byteArrayOf(0x0a) + data.size.toByteArray() + data)
}
fun Pack.Companion.keyHash(string: String): Pack {
    return address(string)
}
fun Pack.Companion.contract(string: String): Pack {
    return address(string, byteArrayOf(0x00))
}

fun Pack.Companion.pair(first: Pack, second: Pack): Pack {
    return Pack(byteArrayOf(0x07, 0x07) + first.data + second.data)
}

fun Pack.Companion.list(elements: Array<out Pack>): Pack {
    val data = elements.fold(byteArrayOf(), { data, pack -> data + pack.data })
    return Pack(byteArrayOf(0x02) + data.size.toByteArray() + data)
}
fun Pack.Companion.listOf(vararg elements: Pack): Pack {
    return list(elements)
}
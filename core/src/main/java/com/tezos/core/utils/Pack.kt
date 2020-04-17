package com.tezos.core.utils

import com.tezos.core.crypto.Base58
import org.bitcoinj.core.AddressFormatException
import java.io.OutputStream
import kotlin.math.absoluteValue
import kotlin.math.sign

interface Visitor {
    fun visit(integer: Long)
    fun visit(string: String)
    fun visit(sequence: Array<out Visitable>)
    fun visit(primitive: Primitive)
    fun visit(bytes: ByteArray)
}

interface Visitable {

    companion object

    fun accept(visitor: Visitor)
}

data class VisitableLong(val value: Long): Visitable {
    override fun accept(visitor: Visitor) {
        visitor.visit(value)
    }
}

fun Visitable.Companion.integer(value: Long): Visitable {
    return VisitableLong(value)
}

data class VisitableString(val value: String): Visitable {
    override fun accept(visitor: Visitor) {
        visitor.visit(value)
    }
}

fun Visitable.Companion.string(value: String): Visitable {
    return VisitableString(value)
}


@Suppress("ArrayInDataClass")
data class VisitableSequence(val elements: Array<out Visitable>): Visitable {
    override fun accept(visitor: Visitor) {
        visitor.visit(elements)
    }
}

fun Visitable.Companion.sequenceOf(vararg visitables: Visitable): Visitable {
    return VisitableSequence(visitables)
}

@Suppress("ArrayInDataClass")
data class Primitive(val name: Name, val arguments: Array<out Visitable>? = null, val annotations: String? = null): Visitable {

    companion object

    @Suppress("unused", "EnumEntryName", "SpellCheckingInspection")
    enum class Name {
        parameter,
        storage,
        code,
        False,
        Elt,
        Left,
        None,
        Pair,
        Right,
        Some,
        True,
        Unit,
        PACK,
        UNPACK,
        BLAKE2B,
        SHA256,
        SHA512,
        ABS,
        ADD,
        AMOUNT,
        AND,
        BALANCE,
        CAR,
        CDR,
        CHECK_SIGNATURE,
        COMPARE,
        CONCAT,
        CONS,
        CREATE_ACCOUNT,
        CREATE_CONTRACT,
        IMPLICIT_ACCOUNT,
        DIP,
        DROP,
        DUP,
        EDIV,
        EMPTY_MAP,
        EMPTY_SET,
        EQ,
        EXEC,
        FAILWITH,
        GE,
        GET,
        GT,
        HASH_KEY,
        IF,
        IF_CONS,
        IF_LEFT,
        IF_NONE,
        INT,
        LAMBDA,
        LE,
        LEFT,
        LOOP,
        LSL,
        LSR,
        LT,
        MAP,
        MEM,
        MUL,
        NEG,
        NEQ,
        NIL,
        NONE,
        NOT,
        NOW,
        OR,
        PAIR,
        PUSH,
        RIGHT,
        SIZE,
        SOME,
        SOURCE,
        SENDER,
        SELF,
        STEPS_TO_QUOTA,
        SUB,
        SWAP,
        TRANSFER_TOKENS,
        SET_DELEGATE,
        UNIT,
        UPDATE,
        XOR,
        ITER,
        LOOP_LEFT,
        ADDRESS,
        CONTRACT,
        ISNAT,
        CAST,
        RENAME,
        bool,
        contract,
        int,
        key,
        key_hash,
        lambda,
        list,
        map,
        big_map,
        nat,
        option,
        or,
        pair,
        set,
        signature,
        string,
        bytes,
        mutez,
        timestamp,
        unit,
        operation,
        address,
        SLICE,
        DIG,
        DUG,
        EMPTY_BIG_MAP,
        APPLY,
        chain_id,
        CHAIN_ID;

        companion object {
            fun valueOf(value: Int) = values().first { it.ordinal == value }
        }
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

@Suppress("ArrayInDataClass")
data class VisitableBytes(val value: ByteArray): Visitable {
    override fun accept(visitor: Visitor) {
        visitor.visit(value)
    }
}

fun Visitable.Companion.byteArrayOfKeyHash(keyHash: String): ByteArray {
    var decodedValue = Base58.decode(keyHash.slice(0 until 36))
    println("pikatos: " + decodedValue.size)
    var bytes = decodedValue.slice(3 until (decodedValue.size - 4)).toByteArray()
    bytes = when(keyHash.slice(0 until 3)) {
        "tz1" -> byteArrayOf(0x00) + bytes
        "tz2" -> byteArrayOf(0x01) + bytes
        "tz3" -> byteArrayOf(0x02) + bytes
        else -> throw AddressFormatException("Unknown address prefix")
    }
    return bytes
}

fun Visitable.Companion.keyHash(value: String): Visitable {
    return VisitableBytes(byteArrayOfKeyHash(value))
}

fun Visitable.Companion.byteArrayOfPublicKey(publicKey: String): ByteArray {
    var decodedValue = Base58.decode(publicKey)
    var bytes = decodedValue.slice(4 until (decodedValue.size - 4)).toByteArray()
    bytes = when(publicKey.slice(0 until 4)) {
        "edpk" -> byteArrayOf(0x00) + bytes
        "sppk" -> byteArrayOf(0x01) + bytes
        "p2pk" -> byteArrayOf(0x02) + bytes
        else -> throw PublicKeyFormatException("Unknown public key prefix")
    }
    return bytes
}

fun Visitable.Companion.publicKey(value: String): Visitable {
    return VisitableBytes(byteArrayOfPublicKey(value))
}

fun Visitable.Companion.byteArrayOfSignature(signature: String): ByteArray {
    var decodedValue = Base58.decode(signature)
    var bytes = decodedValue.slice(5 until (decodedValue.size - 4)).toByteArray()
    bytes = when(signature.slice(0 until 5)) {
        "edsig" -> byteArrayOf(0x00) + bytes
        "spsig" -> byteArrayOf(0x01) + bytes // spsig1 <- 1? (lib_crypto/base58.ml)
        "p2sig" -> byteArrayOf(0x02) + bytes
        else -> throw SignatureFormatException("Unknown signature prefix")
    }
    return bytes
}

fun Visitable.Companion.signature(value: String): Visitable {
    return VisitableBytes(byteArrayOfSignature(value))
}

fun Visitable.Companion.address(value: String): Visitable {
    var decodedValue = Base58.decode(value.slice(0 until 36))
    var bytes = decodedValue.slice(3 until (decodedValue.size - 4)).toByteArray()
    bytes = when(value.slice(0 until 3)) {
        "KT1" -> if (value.length > 36) { // contract with specific entrypoint
            byteArrayOf(0x01) + bytes + byteArrayOf(0x00) + value.slice(37 until value.length).toByteArray() // 37 to skip entrypoint symbol '%'
        }
        else {
            byteArrayOf(0x01) + bytes + byteArrayOf(0x00)
        }
        else -> byteArrayOf(0x00) + byteArrayOfKeyHash(value)
    }
    return VisitableBytes(bytes)
}

fun Visitable.Companion.chainID(value: String): Visitable {
    var bytes = Base58.decode(value).slice(3 until 7).toByteArray()
    return VisitableBytes(bytes)
}

data class FakeOutputStream(var size: Int = 0): OutputStream() {
    override fun write(b: Int) {
        ++size
    }
}

data class Packer(val output: OutputStream): Visitor {
    override fun visit(integer: Long) {
        output.write(0x00)

        var magnitude: Long = integer.absoluteValue

        // first byte handle sign flag
        var byte: Int = (magnitude and 0x3f).toInt() or (if (integer.sign == -1) 0x40 else 0) // make room for sign & next flag and set it
        magnitude = magnitude shr 6

        while (magnitude != 0L) {
            byte = byte or 0x80
            output.write(byte)
            byte = (magnitude and 0x7f).toInt()
            magnitude = magnitude shr 7
        }

        output.write(byte)
    }
    override fun visit(string: String) {
        output.write(0x01)
        pack(string.length)
        output.write(string.toByteArray())
    }
    override fun visit(sequence: Array<out Visitable>) {
        output.write(0x02)
        packSizeOf(sequence)
        sequence.forEach { it.accept(this) }
    }
    override fun visit(primitive: Primitive) {
        val arguments: Array<out Visitable> = primitive.arguments ?: arrayOf()

        val tag: Int =  if (arguments.size > 2) {
            0x09
        } else {
            2 * arguments.size + 3 + if (primitive.annotations == null) 0 else 1
        }
        output.write(tag)

        output.write(primitive.name.ordinal)

        if (tag == 9) {
            packSizeOf(arguments)
        }
        arguments.forEach { it.accept(this) }

        primitive.annotations?.apply {
            pack(length)
            output.write(toByteArray())
        }
    }
    override fun visit(bytes: ByteArray) {
        output.write(0x0a)
        pack(bytes.size)
        output.write(bytes)
    }

    private fun packSizeOf(sequence: Array<out Visitable>) {
        val fake = Packer(FakeOutputStream())
        sequence.forEach { it.accept(fake) }
        pack((fake.output as FakeOutputStream).size)
    }

    private fun pack(value: Int) {
        output.write((value shr 24) and 0xff)
        output.write((value shr 16) and 0xff)
        output.write((value shr 8) and 0xff)
        output.write(value and 0xff)
    }
}

fun Primitive.Companion.pair(left: Visitable, right: Visitable): Visitable {
    return Primitive(
            Primitive.Name.Pair,
            arrayOf(
                    left, right
            )
    )
}

fun Primitive.Companion.left(value: Visitable): Visitable {
    return Primitive(
            Primitive.Name.Pair,
            arrayOf(
                    value
            )
    )
}

class PublicKeyFormatException: IllegalArgumentException {
    constructor(message: String?): super(message)
}

class SignatureFormatException: IllegalArgumentException {
    constructor(message: String?): super(message)
}

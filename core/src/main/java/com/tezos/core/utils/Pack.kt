package com.tezos.core.utils

import java.io.OutputStream
import kotlin.math.absoluteValue
import kotlin.math.sign

interface Visitor {
    fun visit(integer: Long)
    fun visit(string: String)
    fun visit(sequence: Array<out Visitable>)
    fun visit(primitive: Primitive)
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
        CHAIN_ID
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
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

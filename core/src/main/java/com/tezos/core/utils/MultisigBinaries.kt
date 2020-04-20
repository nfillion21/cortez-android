package com.tezos.core.utils

import com.tezos.core.crypto.CryptoUtils
import com.tezos.ui.utils.hexToByteArray
import java.io.ByteArrayInputStream

class MultisigBinaries(val hexaInput: String)
{
    companion object
    {
        enum class MULTISIG_BINARY_TYPE
        {
            UPDATE_SIGNATORIES, DELEGATE, TRANSFER
        }
    }

    fun getType():MULTISIG_BINARY_TYPE?
    {
        val inputStream = ByteArrayInputStream(hexaInput?.hexToByteArray())
        inputStream.read() // 0x05
        val visitable = Unpacker(inputStream).unpack()

        // check for update signatories first.


        val vChainId = visitable.primitive?.arguments?.get(0)?.primitive?.arguments?.get(0)?.bytes

        val vContract = visitable.primitive?.arguments?.get(0)?.primitive?.arguments?.get(1)?.bytes
        val hashContract = vContract?.slice(1 until 21)?.toByteArray()

        val pkhh = if (hashContract != null) CryptoUtils.genericHashToKT(hashContract) else null


        val counterVisitable = visitable.primitive?.arguments?.get(1)?.primitive?.arguments?.get(0)
        val counterV = counterVisitable?.integer

        val signatoriesss = visitable.primitive?.arguments?.get(1)?.primitive?.arguments?.get(1)?.primitive?.arguments?.get(0)?.primitive?.arguments?.get(0)?.primitive?.arguments
        val threshold = signatoriesss?.get(0)?.integer

        val sequence = signatoriesss?.get(1)?.sequence

        if (!sequence.isNullOrEmpty())
        {
            val signatoriesArray = ArrayList<String>(sequence.size)

            for (entry in sequence)
            {
                val hashPublicKey = entry?.bytes?.slice(1 until entry!!.bytes!!.size)?.toByteArray()

                signatoriesArray.add(CryptoUtils.genericHashToPk(hashPublicKey))
            }
        }

        if (
            vChainId != null && //maybe check if it's the good network id
                !pkhh.isNullOrEmpty() &&
                counterV == null &&
                        threshold == null &&
                        !sequence.isNullOrEmpty()
        )
        {
            //fail
            return MULTISIG_BINARY_TYPE.UPDATE_SIGNATORIES
        }
        else // check for delegation
        {








        }

        return null
    }

    private fun parsePrimitive(tag: Int): Visitable?
    {
        /*
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
         */
        return null
    }
}

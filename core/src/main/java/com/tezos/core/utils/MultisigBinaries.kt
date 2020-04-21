package com.tezos.core.utils

import com.tezos.core.crypto.CryptoUtils
import com.tezos.ui.utils.hexToByteArray
import java.io.ByteArrayInputStream
import kotlin.math.sign

class MultisigBinaries(val hexaInput: String)
{
    companion object
    {
        enum class MULTISIG_BINARY_TYPE
        {
            UPDATE_SIGNATORIES, DELEGATE, UNDELEGATE, TRANSFER
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

        val signatories = visitable.primitive?.arguments?.get(1)?.primitive?.arguments?.get(1)?.primitive?.arguments?.get(0)?.primitive?.arguments?.get(0)?.primitive?.arguments

        var threshold:Long? = null
        var baker:String? = null
        val signatoriesArray = ArrayList<String>()

        if (!signatories.isNullOrEmpty())
        {
            val prethreshold = signatories?.get(0)
            if (prethreshold != null)
            {
                if (prethreshold is VisitableBytes)
                {
                    val bytes = prethreshold.bytes

                    val hashBaker = bytes?.slice(1 until bytes?.size)?.toByteArray()
                    baker = CryptoUtils.genericHashToPkh(hashBaker)
                }
                else if (prethreshold is VisitableLong)
                {
                    threshold = signatories?.get(0)?.integer
                }
            }

            if (signatories.size > 1)
            {
                val sequence = signatories?.get(1)?.sequence
                if (!sequence.isNullOrEmpty())
                {
                    for (entry in sequence)
                    {
                        val hashPublicKey = entry?.bytes?.slice(1 until entry!!.bytes!!.size)?.toByteArray()

                        signatoriesArray.add(CryptoUtils.genericHashToPk(hashPublicKey))
                    }
                }
            }
        }


        val delegate = visitable.primitive?.arguments?.get(1)?.primitive?.arguments?.get(1)?.primitive?.arguments?.get(0)?.primitive?.arguments?.get(0)?.primitive
        val delegateNone = delegate?.name == Primitive.Name.None

        if (
            vChainId != null && //maybe check if it's the good network id
                !pkhh.isNullOrEmpty() &&
                counterV != null )
        {
            if (
                    threshold != null &&
                    !signatoriesArray.isNullOrEmpty()
            )
            {
                return MULTISIG_BINARY_TYPE.UPDATE_SIGNATORIES
            }
            else if (delegateNone)
            {
                return MULTISIG_BINARY_TYPE.DELEGATE
            }
            else if (!baker.isNullOrEmpty())
            {
                return MULTISIG_BINARY_TYPE.UNDELEGATE
            }
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
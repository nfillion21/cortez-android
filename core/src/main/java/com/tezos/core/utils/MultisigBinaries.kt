package com.tezos.core.utils

import com.tezos.core.crypto.CryptoUtils
import com.tezos.ui.utils.hexToByteArray
import java.io.ByteArrayInputStream

class MultisigBinaries(val hexaInput: String)
{
    private var mThreshold: Long? = null
    private var mContractAddress: String? = null
    private var mSignatoriesArray = ArrayList<String>()
    private var mOperationTypeString: String? = null

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

        mContractAddress = if (hashContract != null) CryptoUtils.genericHashToKT(hashContract) else null


        val counterVisitable = visitable.primitive?.arguments?.get(1)?.primitive?.arguments?.get(0)
        val counterV = counterVisitable?.integer

        val signatories = visitable.primitive?.arguments?.get(1)?.primitive?.arguments?.get(1)?.primitive?.arguments?.get(0)?.primitive?.arguments?.get(0)?.primitive?.arguments

        var baker:String? = null

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
                    mThreshold = signatories?.get(0)?.integer
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

                        mSignatoriesArray.add(CryptoUtils.genericHashToPk(hashPublicKey))
                    }
                }
            }
        }


        val delegate = visitable.primitive?.arguments?.get(1)?.primitive?.arguments?.get(1)?.primitive?.arguments?.get(0)?.primitive?.arguments?.get(0)?.primitive
        val delegateNone = delegate?.name == Primitive.Name.None

        if (
            vChainId != null && //maybe check if it's the good network id
                !mContractAddress.isNullOrEmpty() &&
                counterV != null )
        {
            if (
                    mThreshold != null &&
                    !mSignatoriesArray.isNullOrEmpty()
            )
            {
                mOperationTypeString = "Update threshold and/or signatories"
                return MULTISIG_BINARY_TYPE.UPDATE_SIGNATORIES
            }
            else if (delegateNone)
            {
                mOperationTypeString = "Delegate"
                return MULTISIG_BINARY_TYPE.DELEGATE
            }
            else if (!baker.isNullOrEmpty())
            {
                mOperationTypeString = "Undelegate"
                return MULTISIG_BINARY_TYPE.UNDELEGATE
            }
        }

        return null
    }

    fun getThreshold():Long?
    {
        return mThreshold
    }

    fun getOperationTypeString():String?
    {
        return mOperationTypeString
    }

    fun getContractAddress():String?
    {
        return mContractAddress
    }

    fun getSignatories():ArrayList<String>?
    {
        return mSignatoriesArray
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

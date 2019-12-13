package com.tezos.ui.utils

import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.utils.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.interfaces.ECPublicKey
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToLong

/*
(*****************************************************************************)
(*                                                                           *)
(* Open Source License                                                       *)
(* Copyright (c) 2018 Nomadic Development, Inc. <contact@tezcore.com>        *)
(*                                                                           *)
(* Permission is hereby granted, free of charge, to any person obtaining a   *)
(* copy of this software and associated documentation files (the "Software"),*)
(* to deal in the Software without restriction, including without limitation *)
(* the rights to use, copy, modify, merge, publish, distribute, sublicense,  *)
(* and/or sell copies of the Software, and to permit persons to whom the     *)
(* Software is furnished to do so, subject to the following conditions:      *)
(*                                                                           *)
(* The above copyright notice and this permission notice shall be included   *)
(* in all copies or substantial portions of the Software.                    *)
(*                                                                           *)
(* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR*)
(* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  *)
(* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL   *)
(* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER*)
(* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING   *)
(* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER       *)
(* DEALINGS IN THE SOFTWARE.                                                 *)
(*                                                                           *)
(*****************************************************************************)
*/

fun isTransferPayloadValid(payload:String, params: JSONObject):Boolean
{
    var isValid = false
    if (params != null)
    {
        val data = payload.hexToByteArray()

        val obj = params["dsts"] as JSONArray
        val dstObj = obj[0] as JSONObject

        val dataField = data.slice(32 until data.size).toByteArray()

        val revealFees = isRevealTagCorrect(dataField, params["src"] as String, params["src_pk"] as String)
        if (revealFees.first != -1L)
        {
            val transactionByteArray = revealFees.second

            val srcParam = params["src"] as String
            val dstParam = dstObj["dst"] as String

            val dstAccount =
                    if (dstObj.has("dst_account"))
                    {
                        dstObj["dst_account"] as String?
                    }
                    else
                    {
                        null
                    }

            val amountTransfer =
                    if (dstObj.has("transfer_amount"))
                    {
                        dstObj["transfer_amount"] as Long?
                    }
                    else
                    {
                        null
                    }

            val contractType =
                    if (dstObj.has("contract_type"))
                    {
                        dstObj["contract_type"] as String?
                    }
                    else
                    {
                        null
                    }

            val edsig =
                    if (dstObj.has("edsig"))
                    {
                        dstObj["edsig"] as String?
                    }
                    else
                    {
                        null
                    }

            val transactionFees = isTransactionTagCorrect(transactionByteArray!!, srcParam, dstParam, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType, params["src_pk"] as String, edsig)
            if (transactionFees != -1L)
            {
                val totalFees = revealFees.first + transactionFees
                if (totalFees == dstObj["fee"])
                {
                    return true
                }
            }
        }
        else
        {
            val dstAccount =
                    if (dstObj.has("dst_account"))
                    {
                        dstObj["dst_account"] as String?
                    }
                    else
                    {
                        null
                    }

            val amountTransfer =
                    if (dstObj.has("transfer_amount"))
                    {
                        dstObj["transfer_amount"] as Long?
                    }
                    else
                    {
                        null
                    }

            val contractType =
                    if (dstObj.has("contract_type"))
                    {
                        dstObj["contract_type"] as String?
                    }
                    else
                    {
                        null
                    }

            val edsig =
                    if (dstObj.has("edsig"))
                    {
                        dstObj["edsig"] as String?
                    }
                    else
                    {
                        null
                    }

            val transactionFees = isTransactionTagCorrect(dataField, params["src"] as String, dstObj["dst"] as String, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType, params["src_pk"] as String, edsig)
            if (transactionFees != -1L)
            {
                if (transactionFees == dstObj["fee"])
                {
                    return true
                }
            }
        }
    }
    return isValid
}

/*
fun isChangeDelegatePayloadValid(payload:String, params: JSONObject):Boolean
{
    var isValid = false
    if (payload != null && params != null)
    {
        val data = payload.hexToByteArray()

        // 32 first bytes are the block hash
        var i = 32

        //Reveal Tag 10
        val firstTag = data[i++]
        if (firstTag.compareTo(10) == 0)
        {
            val contract = data.slice(i until data.size).toByteArray()

            i = 22
            val contractParse = contract.slice(1 until i).toByteArray()

            val contractKT = params["src"]

            val isContractValid = contractKT == CryptoUtils.genericHashToKT(contractParse)
            if (!isContractValid)
            {
                return false
            }

            val fee = contract.slice(i until contract.size).toByteArray()

            val feeList = ArrayList<Int>()
            i = 0
            do
            {
                val bytePos = Utils.byteToUnsignedInt(fee[i])

                feeList.add(bytePos)
                i++

            } while (bytePos > 128)

            val dstFees = params["fee"] as Long

            val fees = addBytesLittleEndian(feeList)
            val isFeesValid = fees == dstFees

            if (!isFeesValid)
            {
                return false
            }

            val counter = fee.slice(i until fee.size).toByteArray()
            i = 0
            do
            {
                val bytePos = Utils.byteToUnsignedInt(counter[i])
                i++

            } while (bytePos >= 128)

            val gasLimit = counter.slice(i until counter.size).toByteArray()
            i = 0
            do
            {
                val bytePos = Utils.byteToUnsignedInt(gasLimit[i])
                i++

            } while (bytePos >= 128)


            val storageLimit = gasLimit.slice(i until gasLimit.size).toByteArray()
            i = 0
            do
            {
                val bytePos = Utils.byteToUnsignedInt(storageLimit[i])
                i++

            } while (bytePos >= 128)

            val delegatableField = storageLimit.slice(i until storageLimit.size).toByteArray()
            i = 0
            val isDelegatableFieldValid = Utils.byteToUnsignedInt(delegatableField[i++]).compareTo(255) == 0
            if (!isDelegatableFieldValid)
            {
                return false
            }

            val delegate = delegatableField.slice(i until delegatableField.size).toByteArray()
            val delegateParse = delegate.slice(1 .. 20).toByteArray()
            val delegatePkh = params["delegate"] as String

            //val cryptoDelegate = CryptoUtils.genericHashToPkh(delegateParse)

            val beginsWith = delegatePkh.slice(0 until 3)

            val cryptoDelegate = when (beginsWith.toLowerCase())
            {
                "tz1" -> CryptoUtils.genericHashToPkh(delegateParse)
                "tz2" -> CryptoUtils.genericHashToPkhTz2(delegateParse)
                "tz3" -> CryptoUtils.genericHashToPkhTz3(delegateParse)
                else -> null
            }

            val isDelegateValid = delegatePkh == cryptoDelegate

            if (!isDelegateValid)
            {
                return false
            }

            return true
        }
    }
    return isValid
}
*/

/*
fun isRemoveDelegatePayloadValid(payload:String, params: JSONObject):Boolean
{
    if (payload != null && params != null)
    {
        // 32 first bytes are the block hash

        val data = payload.hexToByteArray()
        val dataField = data.slice(32 until data.size).toByteArray()

        val revealFees = isRevealTagCorrect(dataField, params["src"] as String, params["src_pk"] as String)
        if (revealFees.first != -1L)
        {
            val delegationByteArray = revealFees.second

            val delegationFees = isDelegationTagCorrect(delegationByteArray!!, params["src"] as String)
            if (delegationFees != -1L)
            {
                val totalFees = revealFees.first + delegationFees
                if (totalFees == params["fee"])
                {
                    return true
                }
            }
        }
        else
        {
            val delegationFees = isDelegationTagCorrect(dataField, params["src"] as String)
            if (delegationFees != -1L)
            {
                if (delegationFees == params["fee"])
                {
                    return true
                }
            }
        }

        return false
    }
    return false
}
*/

fun isRemoveDelegatePayloadValid(payload:String, params: JSONObject):Boolean
{
    var isValid = false
    val data = payload.hexToByteArray()

    val obj = params["dsts"] as JSONArray
    val dstObj = obj[0] as JSONObject

    val dataField = data.slice(32 until data.size).toByteArray()

    val revealFees = isRevealTagCorrect(dataField, params["src"] as String, params["src_pk"] as String)
    if (revealFees.first != -1L)
    {
        val transactionByteArray = revealFees.second

        val srcParam = params["src"] as String
        val dstParam = dstObj["dst"] as String

        val dstAccount =
                if (dstObj.has("dst_account"))
                {
                    dstObj["dst_account"] as String?
                }
                else
                {
                    null
                }

        val amountTransfer =
                if (dstObj.has("transfer_amount"))
                {
                    dstObj["transfer_amount"] as Long?
                }
                else
                {
                    null
                }

        val contractType =
                if (dstObj.has("contract_type"))
                {
                    dstObj["contract_type"] as String?
                }
                else
                {
                    null
                }

        val edsig =
                if (dstObj.has("edsig"))
                {
                    dstObj["edsig"] as String?
                }
                else
                {
                    null
                }

        val transactionFees = isTransactionTagCorrect(transactionByteArray!!, srcParam, dstParam, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType, params["src_pk"] as String, edsig)
        if (transactionFees != -1L)
        {
            val totalFees = revealFees.first + transactionFees
            if (totalFees == dstObj["fee"])
            {
                return true
            }
        }
    }
    else
    {
        val dstAccount =
                if (dstObj.has("dst_account"))
                {
                    dstObj["dst_account"] as String?
                }
                else
                {
                    null
                }

        val amountTransfer =
                if (dstObj.has("transfer_amount"))
                {
                    dstObj["transfer_amount"] as Long?
                }
                else
                {
                    null
                }

        val contractType =
                if (dstObj.has("contract_type"))
                {
                    dstObj["contract_type"] as String?
                }
                else
                {
                    null
                }

        val edsig =
                if (dstObj.has("edsig"))
                {
                    dstObj["edsig"] as String?
                }
                else
                {
                    null
                }

        val transactionFees = isTransactionTagCorrect(dataField, params["src"] as String, dstObj["dst"] as String, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType, params["src_pk"] as String, edsig)
        if (transactionFees != -1L)
        {
            if (transactionFees == dstObj["fee"])
            {
                return true
            }
        }
    }
    return isValid
}




private fun isRevealTagCorrect(payload: ByteArray, src:String, srcPk:String):Pair<Long, ByteArray?>
{
    var i = 0

    //Reveal Tag 7
    val firstTag = payload[i++]
    if (firstTag.compareTo(107) == 0)
    {
        val contract = payload.slice(i until payload.size).toByteArray()

        i = 21
        val contractParse = contract.slice(1 until i).toByteArray()

        val hash = if (src.startsWith("KT1", true))
        {
            CryptoUtils.genericHashToKT(contractParse)
        }
        else
        {
            if (src.startsWith("tz3", ignoreCase = true))
            {
                CryptoUtils.genericHashToPkhTz3(contractParse)
            }
            else
            {
                CryptoUtils.genericHashToPkh(contractParse)
            }
        }

        val isContractValid = src == hash
        if (!isContractValid)
        {
            return Pair(-1L, null)
        }

        val fee = contract.slice(i until contract.size).toByteArray()

        val feeList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(fee[i])

            feeList.add(bytePos)
            i++

        } while (bytePos > 128)

        val fees = addBytesLittleEndian(feeList)

        val counter = fee.slice(i until fee.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(counter[i])
            i++

        } while (bytePos >= 128)

        val gasLimit = counter.slice(i until counter.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(gasLimit[i])
            i++

        } while (bytePos >= 128)

        val storageLimit = gasLimit.slice(i until gasLimit.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(storageLimit[i])
            i++

        } while (bytePos >= 128)

        // we don't read the first byte (0)
        // let's read the first byte to handle tz3
        var publicKey = storageLimit.slice(i until storageLimit.size).toByteArray()

        val binaryToPk = if (publicKey[0].compareTo(2) == 0)
        {
            i = 33
            val publicKeyParse = publicKey.slice(1 .. i).toByteArray()
            CryptoUtils.genericHashToP2pk(publicKeyParse)
        }
        else
        {
            i = 32
            val publicKeyParse = publicKey.slice(1 .. i).toByteArray()
            CryptoUtils.genericHashToPk(publicKeyParse)
        }
        //publicKey = publicKey.dropWhile { it == "0".toByte() }.toByteArray()

        val isPublicKeyValid = srcPk == binaryToPk

        //TODO verify this one
        /*
        if (!isPublicKeyValid)
        {
            return Pair(-1L, null)
        }
         */

        i++
        val nextField = publicKey.slice(i until publicKey.size).toByteArray()

        return Pair(fees, nextField)
    }

    return Pair(-1L, null)
}

private fun isTransactionTagCorrect(payload: ByteArray, srcParam:String, dstParam:String, amountParam:Long, amountDstParam:Long?, dstAccountParam:String?, contractType:String?, pk:String?, sig:String?):Long
{
    //TODO handle if delegation is correct, return the fees, or do something to add the fees with reveal.

    var i = 0

    val firstTag = payload[i++]
    if (firstTag.compareTo(108) == 0)
    {

        var src = payload.slice((i+1)..(21)).toByteArray()

        val srcCompare = if (srcParam.startsWith("KT1", true))
        {
            CryptoUtils.genericHashToKT(src)
        }
        else
        {
            if (srcParam.startsWith("tz3"))
            {
                CryptoUtils.genericHashToPkhTz3(src)
            }
            else
            {
                CryptoUtils.genericHashToPkh(src)
            }
        }

        val isSrcValid = srcParam == srcCompare

        if (!isSrcValid)
        {
            return -1L
        }

        i = 22

        val size = payload.size
        val fee = payload.slice((i) until size).toByteArray()

        val feeList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(fee[i])

            feeList.add(bytePos)
            i++

        } while (bytePos >= 128)

        val retFee = addBytesLittleEndian(feeList)

        val counter = fee.slice(i until fee.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(counter[i])
            i++

        } while (bytePos >= 128)

        val gasLimit = counter.slice(i until counter.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(gasLimit[i])
            i++

        } while (bytePos >= 128)

        val storageLimit = gasLimit.slice(i until gasLimit.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(storageLimit[i])
            i++

        } while (bytePos >= 128)

        val amount = storageLimit.slice(i until storageLimit.size).toByteArray()

        val amountList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(amount[i])

            amountList.add(bytePos)
            i++

        } while (bytePos >= 128)

        val isAmountValid = addBytesLittleEndian(amountList) == amountParam
        if (!isAmountValid)
        {
            return -1L
        }

        val dstOrigin = amount.slice(i until amount.size).toByteArray()

        val beginsWith = dstParam.slice(0 until 3)

        val begins = when (beginsWith.toLowerCase(Locale.US))
        {
            "kt1" -> {
                var dst2 = dstOrigin.slice(1..20).toByteArray()
                CryptoUtils.genericHashToKT(dst2)
            }

            "tz1" -> {
                var dst = dstOrigin.slice(2..21).toByteArray()
                CryptoUtils.genericHashToPkh(dst)
            }
            "tz2" -> {
                var dst = dstOrigin.slice(2..21).toByteArray()
                CryptoUtils.genericHashToPkhTz2(dst)
            }
            "tz3" -> {
                var dst = dstOrigin.slice(2..21).toByteArray()
                CryptoUtils.genericHashToPkhTz3(dst)
            }
            else -> null
        }


        val isDstValid = dstParam == begins

        if (!isDstValid)
        {
            return -1L
        }

        val parametersField = dstOrigin.slice(22 until dstOrigin.size).toByteArray()

        i = 0
        val isParametersFieldValid = Utils.byteToUnsignedInt(parametersField[i++]).compareTo(255) == 0
        if (!isParametersFieldValid)
        {
            return retFee
        }

        val parametersDataField = parametersField.slice(i until parametersField.size).toByteArray()

        var parameters:Visitable

        when (contractType) {
            "remove_delegate" -> parameters =

                    Visitable.sequenceOf(
                            Visitable.sequenceOf(

                                    Primitive(Primitive.Name.DROP),
                                    Primitive(Primitive.Name.NIL, arrayOf(Primitive(Primitive.Name.operation))),
                                    Primitive(
                                            Primitive.Name.NONE,
                                            arrayOf(Primitive(Primitive.Name.key_hash))

                                    ),
                                    Primitive(Primitive.Name.SET_DELEGATE),
                                    Primitive(Primitive.Name.CONS)
                            )
                    )

            "remove_delegate_slc" -> parameters =

                    Primitive(Primitive.Name.Pair,
                            arrayOf(
                                    Primitive(Primitive.Name.Pair,
                                            arrayOf(
                                                    Visitable.string(pk!!),
                                                    Visitable.string(sig!!)
                                            )
                                    ),

                                    Primitive(Primitive.Name.Right,
                                            arrayOf(
                                                    Primitive(Primitive.Name.Pair,
                                                            arrayOf(
                                                                    Visitable.sequenceOf(
                                                                            Primitive(Primitive.Name.DROP),
                                                                            Primitive(Primitive.Name.NIL,
                                                                                    arrayOf(
                                                                                            Primitive(Primitive.Name.operation)
                                                                                    )
                                                                            ),
                                                                            Primitive(Primitive.Name.NONE,
                                                                                    arrayOf(
                                                                                            Primitive(Primitive.Name.key_hash)
                                                                                    )
                                                                            ),
                                                                            Primitive(Primitive.Name.SET_DELEGATE),
                                                                            Primitive(Primitive.Name.CONS)
                                                                    ),
                                                                    Visitable.string(srcParam)
                                                            )
                                                    )
                                            )
                                    )

                            )
                    )

            "add_delegate" -> parameters =

                    Visitable.sequenceOf(
                            Visitable.sequenceOf(

                                    Primitive(Primitive.Name.DROP),
                                    Primitive(Primitive.Name.NIL, arrayOf(Primitive(Primitive.Name.operation))),
                                    Primitive(
                                            Primitive.Name.PUSH,
                                            arrayOf(Primitive(Primitive.Name.key_hash),
                                                    Visitable.string(dstAccountParam!!))
                                    ),
                                    Primitive(Primitive.Name.SOME),
                                    Primitive(Primitive.Name.SET_DELEGATE),
                                    Primitive(Primitive.Name.CONS)
                            )
                    )

            "add_delegate_slc" -> parameters =

                    Primitive(Primitive.Name.Pair,
                            arrayOf(
                                    Primitive(Primitive.Name.Pair,
                                            arrayOf(
                                                    Visitable.string(pk!!),
                                                    Visitable.string(sig!!)
                                            )
                                    ),
                                    Primitive(Primitive.Name.Right,
                                            arrayOf(
                                                    Primitive(Primitive.Name.Pair,
                                                            arrayOf(
                                                                    Visitable.sequenceOf(
                                                                            Primitive(Primitive.Name.DROP),
                                                                            Primitive(Primitive.Name.NIL,
                                                                                    arrayOf(
                                                                                            Primitive(Primitive.Name.operation)
                                                                                    )
                                                                            ),
                                                                            Primitive(Primitive.Name.PUSH,
                                                                                    arrayOf(
                                                                                            Primitive(Primitive.Name.key_hash),
                                                                                            Visitable.string(dstAccountParam!!)
                                                                                    )
                                                                            ),
                                                                            Primitive(Primitive.Name.SOME),
                                                                            Primitive(Primitive.Name.SET_DELEGATE),
                                                                            Primitive(Primitive.Name.CONS)
                                                                    ),
                                                                    Visitable.string(srcParam)
                                                            )
                                                    )
                                            )
                                    )
                            )
                    )

            "kt1_to_kt1" -> parameters =

                    Visitable.sequenceOf(
                            Visitable.sequenceOf(
                                    Primitive(Primitive.Name.DROP),
                                    Primitive(Primitive.Name.NIL, arrayOf(Primitive(Primitive.Name.operation))),
                                    Primitive(
                                            Primitive.Name.PUSH,
                                            arrayOf(
                                                    Primitive(Primitive.Name.address),
                                                    Visitable.string(dstAccountParam!!)
                                            )
                                    ),

                                    Primitive(Primitive.Name.CONTRACT, arrayOf(Primitive(Primitive.Name.unit))),
                                    Visitable.sequenceOf(
                                            Primitive(
                                                    Primitive.Name.IF_NONE,
                                                    arrayOf(

                                                            Visitable.sequenceOf(

                                                                    Visitable.sequenceOf(

                                                                            Primitive(Primitive.Name.UNIT),
                                                                            Primitive(Primitive.Name.FAILWITH)
                                                                    )
                                                            ),
                                                            Visitable.sequenceOf()

                                                    )
                                            )
                                    ),
                                    Primitive(
                                            Primitive.Name.PUSH,
                                            arrayOf(
                                                    Primitive(Primitive.Name.mutez),
                                                    Visitable.integer(amountDstParam!!)
                                            )
                                    ),
                                    Primitive(Primitive.Name.UNIT),
                                    Primitive(Primitive.Name.TRANSFER_TOKENS),
                                    Primitive(Primitive.Name.CONS)
                            )
                    )

            "kt1_to_tz" -> parameters =

                    Visitable.sequenceOf(
                            Visitable.sequenceOf(
                                    Primitive(Primitive.Name.DROP),
                                    Primitive(Primitive.Name.NIL, arrayOf(Primitive(Primitive.Name.operation))),
                                    Primitive(
                                            Primitive.Name.PUSH,
                                            arrayOf(
                                                    Primitive(Primitive.Name.key_hash),
                                                    Visitable.string(dstAccountParam!!)
                                            )
                                    ),
                                    Primitive(Primitive.Name.IMPLICIT_ACCOUNT),
                                    Primitive(
                                            Primitive.Name.PUSH,
                                            arrayOf(
                                                    Primitive(Primitive.Name.mutez),
                                                    Visitable.integer(amountDstParam!!)
                                            )
                                    ),
                                    Primitive(Primitive.Name.UNIT),
                                    Primitive(Primitive.Name.TRANSFER_TOKENS),
                                    Primitive(Primitive.Name.CONS)
                            )
                    )

            "slc_master_to_tz" -> parameters =

                    Primitive(
                            Primitive.Name.Pair,
                            arrayOf(
                                    Primitive(
                                            Primitive.Name.Pair,
                                            arrayOf(
                                                    Visitable.string(pk!!),
                                                    Visitable.string(sig!!)
                                            )
                                    ),
                                    Primitive(
                                            Primitive.Name.Right,
                                            arrayOf(
                                                    Primitive(
                                                            Primitive.Name.Pair,
                                                            arrayOf(
                                                                    Visitable.sequenceOf(
                                                                            Primitive(
                                                                                    Primitive.Name.DIP,
                                                                                    arrayOf(
                                                                                            Visitable.sequenceOf(
                                                                                                    Primitive(Primitive.Name.NIL, arrayOf(Primitive(Primitive.Name.operation))),
                                                                                                    Primitive(
                                                                                                            Primitive.Name.PUSH,
                                                                                                            arrayOf(
                                                                                                                    Primitive(Primitive.Name.key_hash),
                                                                                                                    Visitable.string(dstAccountParam!!)
                                                                                                            )
                                                                                                    ),
                                                                                                    Primitive(Primitive.Name.IMPLICIT_ACCOUNT),
                                                                                                    Primitive(
                                                                                                            Primitive.Name.PUSH,
                                                                                                            arrayOf(
                                                                                                                    Primitive(Primitive.Name.mutez),
                                                                                                                    Visitable.integer(amountDstParam!!)
                                                                                                            )
                                                                                                    )
                                                                                            )
                                                                                    )
                                                                            ),
                                                                            Primitive(Primitive.Name.TRANSFER_TOKENS),
                                                                            Primitive(Primitive.Name.CONS)
                                                                    ),
                                                                    Visitable.string(srcParam)
                                                            )
                                                    )
                                            )
                                    )
                            )
                    )

            "slc_master_to_kt1" -> parameters =

                    Primitive(Primitive.Name.Pair,
                            arrayOf(
                                    Primitive
                                    (Primitive.Name.Pair,
                                            arrayOf(
                                                    Visitable.string(pk!!),
                                                    Visitable.string(sig!!)
                                            )
                                    ),
                                    Primitive
                                    (Primitive.Name.Right,

                                            arrayOf(
                                                    Primitive(Primitive.Name.Pair,
                                                            arrayOf(
                                                                    Visitable.sequenceOf
                                                                    (
                                                                            Primitive(Primitive.Name.DIP,
                                                                                    arrayOf(
                                                                                            Visitable.sequenceOf(
                                                                                                    Primitive(Primitive.Name.NIL,
                                                                                                            arrayOf(Primitive(Primitive.Name.operation))
                                                                                                    ),
                                                                                                    Primitive(Primitive.Name.PUSH,
                                                                                                            arrayOf(
                                                                                                                    Primitive(Primitive.Name.address),
                                                                                                                    Visitable.string(dstAccountParam!!)
                                                                                                            )),
                                                                                                    Primitive(Primitive.Name.CONTRACT,
                                                                                                            arrayOf(
                                                                                                                    Primitive(Primitive.Name.unit)
                                                                                                            )),
                                                                                                    Visitable.sequenceOf(
                                                                                                            Primitive(Primitive.Name.IF_NONE,
                                                                                                                    arrayOf(
                                                                                                                            Visitable.sequenceOf(
                                                                                                                                    Visitable.sequenceOf(
                                                                                                                                            Primitive(Primitive.Name.UNIT),
                                                                                                                                            Primitive(Primitive.Name.FAILWITH)
                                                                                                                                    )
                                                                                                                            ),
                                                                                                                            Visitable.sequenceOf()
                                                                                                                    ))
                                                                                                    ),
                                                                                                    Primitive(Primitive.Name.PUSH,
                                                                                                            arrayOf(
                                                                                                                    Primitive(Primitive.Name.mutez),
                                                                                                                    Visitable.integer(amountDstParam!!)
                                                                                                            ))
                                                                                            )
                                                                                    )
                                                                            ),
                                                                            Primitive(Primitive.Name.TRANSFER_TOKENS),
                                                                            Primitive(Primitive.Name.CONS)
                                                                    ),
                                                                    Visitable.string(srcParam)
                                                            )
                                                    )
                                            )
                                    )
                            )
                    )

            "slc_enclave_transfer" -> parameters =

                    Primitive(Primitive.Name.Pair,
                            arrayOf(
                                    Primitive(Primitive.Name.Pair,
                                            arrayOf(

                                                    Visitable.sequenceOf(
                                                            Primitive(
                                                                    Primitive.Name.Pair,
                                                                    arrayOf(
                                                                            Visitable.integer(amountDstParam!!),
                                                                            Visitable.string(dstAccountParam!!)
                                                                    )
                                                            )
                                                    ),
                                                    Visitable.string(srcParam)
                                            )
                                    ),
                                    Primitive(Primitive.Name.Pair,
                                            arrayOf(
                                                    Visitable.string(pk!!),
                                                    Visitable.string(sig!!)
                                            )
                                    )
                            )
                    )

            else -> {

                //no-op
                parameters = Visitable.sequenceOf()
            }
        }

        val packer = Packer(ByteArrayOutputStream())
        parameters.accept(packer)

        val binary = (packer.output as ByteArrayOutputStream).toByteArray()

        //TODO parametersDataField here
        // ok, need to work a bit on the entrypoint first.

        if (contractType.equals("slc_master_to_tz", ignoreCase = true) ||
                contractType.equals("slc_master_to_kt1", ignoreCase = true) ||
                contractType.equals("slc_enclave_transfer", ignoreCase = true) ||
                contractType.equals("add_delegate_slc", ignoreCase = true) ||
                contractType.equals("remove_delegate_slc", ignoreCase = true)
        )
        {

            //entrypoint named (tag 255)
            if ( Utils.byteToUnsignedInt(parametersDataField[0]) != 255)
            {
                return -1L
            }

            //val entryPointSize = parametersDataField.slice(1 until 5).toByteArray()
            //val entryPointSizeInt = ByteBuffer.wrap(entryPointSize, 0, 1).int

            val entryPointSizeInt = Utils.byteToUnsignedInt(parametersDataField[1])

            val entryPointField = parametersDataField.slice(5 until parametersDataField.size)
            val entryPoint = entryPointField.slice(0 until entryPointSizeInt).toByteArray()
            //TODO check the entrypoint "appel_clef_maitresse"
            val str = String(entryPoint)

            val scriptField = parametersDataField.slice(2 + entryPointSizeInt until parametersDataField.size).toByteArray()


            val scriptSize = scriptField.slice(0 until 4).toByteArray()
            val scriptSizeInt = ByteBuffer.wrap(scriptSize,0,4).int

            val scriptCodeField = scriptField.slice(4 until scriptField.size).toByteArray()

            val scriptCode = scriptCodeField.slice(0 until scriptSizeInt).toByteArray()


            if (!scriptCode.contentEquals(binary))
            {
                return -1L
            }
        }
        else
        {
            //entrypoint tag 2?
            /*
            if ( Utils.byteToUnsignedInt(parametersDataField[0]) != 2)
            {
                return -1L
            }
            */

            if (!parametersDataField.contentEquals(binary))
            {
                return -1L
            }
        }

        return retFee
    }

    return -1L
}

private fun isDelegationTagCorrect(payload: ByteArray, src:String):Long
{
    //TODO handle if delegation is correct, return the fees, or do something to add the fees with reveal.

    var i = 0

    val firstTag = payload[i++]
    if (firstTag.compareTo(110) == 0)
    {
        val contract = payload.slice(i until payload.size).toByteArray()

        i = 21
        val contractParse = contract.slice(1 until i).toByteArray()

        val isContractValid = src == CryptoUtils.genericHashToKT(contractParse)
        if (!isContractValid)
        {
            return -1L
        }

        val fee = contract.slice(i until contract.size).toByteArray()

        val feeList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(fee[i])

            feeList.add(bytePos)
            i++

        } while (bytePos > 128)

        val fees = addBytesLittleEndian(feeList)

        val counter = fee.slice(i until fee.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(counter[i])
            i++

        } while (bytePos >= 128)

        val gasLimit = counter.slice(i until counter.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(gasLimit[i])
            i++

        } while (bytePos >= 128)

        val storageLimit = gasLimit.slice(i until gasLimit.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(storageLimit[i])
            i++

        } while (bytePos >= 128)

        val delegatableField = storageLimit.slice(i until storageLimit.size).toByteArray()
        i = 0
        val isDelegatableFieldValid = Utils.byteToUnsignedInt(delegatableField[i++]).compareTo(0) == 0
        if (!isDelegatableFieldValid)
        {
            return -1L
        }

        return fees
    }

    return -1L
}


private fun isOriginationTagCorrect(data: ByteArray, srcParam:String, balanceParam:Long, delegateParam:String):Long
{
    var i = 0

    val isOriginationTag = data[i++]

    if (isOriginationTag.compareTo(109) == 0)
    {
        var contract= data.slice(i+1 until i+21).toByteArray()

        val isContractValid = srcParam == CryptoUtils.genericHashToPkh(contract)
        if (!isContractValid)
        {
            return -1L
        }

        i+=21

        val size = data.size
        val fee = data.slice((i) until size).toByteArray()

        val feeList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(fee[i])

            feeList.add(bytePos)
            i++

        } while (bytePos > 128)

        //val dstFees = dstObj["fee"] as String

        val retFee = addBytesLittleEndian(feeList)

        val counter = fee.slice(i until fee.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(counter[i])
            i++

        } while (bytePos >= 128)

        val gasLimit = counter.slice(i until counter.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(gasLimit[i])
            i++

        } while (bytePos >= 128)


        val storageLimit = gasLimit.slice(i until gasLimit.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(storageLimit[i])
            i++

        } while (bytePos >= 128)

        val balance = storageLimit.slice(i until storageLimit.size).toByteArray()

        val balanceList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(balance[i])

            balanceList.add(bytePos)
            i++

        } while (bytePos >= 128)

        val isBalanceValid =  addBytesLittleEndian(balanceList) == balanceParam
        if (!isBalanceValid)
        {
            return -1L
        }

        val delegatableField = balance.slice(i until balance.size).toByteArray()
        i = 0
        val isDelegatableFieldValid = Utils.byteToUnsignedInt(delegatableField[i++]).compareTo(255) == 0
        if (!isDelegatableFieldValid)
        {
            return -1L
        }

        val delegate = delegatableField.slice(i until delegatableField.size).toByteArray()
        var delegateParse = delegate.slice(1 until 21).toByteArray()
        //delegateParse = delegateParse.dropWhile { it == "0".toByte() }.toByteArray()

        val beginsWith = delegateParam.slice(0 until 3)

        val cryptoDelegate = when (beginsWith.toLowerCase(Locale.US))
        {
            "tz1" -> CryptoUtils.genericHashToPkh(delegateParse)
            "tz2" -> CryptoUtils.genericHashToPkhTz2(delegateParse)
            "tz3" -> CryptoUtils.genericHashToPkhTz3(delegateParse)
            else -> null
        }

        val isDelegateValid = delegateParam == cryptoDelegate

        if (!isDelegateValid)
        {
            return -1L
        }

        val script = delegate.slice(21 until delegate.size).toByteArray()

        //TODO verify the script
        val scriptSize = script.slice(0 until 4).toByteArray()
        val scriptSizeInt = ByteBuffer.wrap(scriptSize,0,4).int

        val scriptCodeField = script.slice(4 until script.size).toByteArray()

        val scriptCode = scriptCodeField.slice(0 until scriptSizeInt).toByteArray()

        val binary = byteArrayOfInts(2, 0, 0, 0, 193, 5, 0, 7, 100, 8, 94, 3, 108, 5, 95, 3, 109, 0, 0, 0, 3, 37, 100, 111, 4, 108, 0, 0, 0, 8, 37, 100, 101,
                102, 97, 117, 108, 116, 5, 1, 3, 93, 5, 2, 2, 0, 0, 0, 149, 2, 0, 0, 0, 18, 2, 0, 0, 0, 13, 3, 33, 3, 22, 5, 31, 2,
                0, 0, 0, 2, 3, 23, 7, 46, 2, 0, 0, 0, 106, 7, 67, 3, 106, 0, 0, 3, 19, 2, 0, 0, 0, 30, 2, 0, 0, 0, 4, 3, 25,
                3, 37, 7, 44, 2, 0, 0, 0, 0, 2, 0, 0, 0, 9, 2, 0, 0, 0, 4, 3, 79, 3, 39, 2, 0, 0, 0, 11, 5, 31, 2, 0, 0,
                0, 2, 3, 33, 3, 76, 3, 30, 3, 84, 3, 72, 2, 0, 0, 0, 30, 2, 0, 0, 0, 4, 3, 25, 3, 37, 7, 44, 2, 0, 0, 0, 0,
                2, 0, 0, 0, 9, 2, 0, 0, 0, 4, 3, 79, 3, 39, 3, 79, 3, 38, 3, 66, 2, 0, 0, 0, 8, 3, 32, 5, 61, 3, 109, 3, 66)

        if (!scriptCode.contentEquals(binary))
        {
            return -1L
        }

        val storageField = scriptCodeField.slice(scriptSizeInt until scriptCodeField.size).toByteArray()

        val storageSize = storageField.slice(0 until 4).toByteArray()
        val storageSizeInt = ByteBuffer.wrap(storageSize, 0, 4).int

        val storageBinary = storageField.slice(4 until storageField.size).toByteArray()

        if (storageBinary.size != storageSizeInt)
        {
            return -1L
        }

        if (storageBinary[0].toInt() != 1)
        {
            return -1L
        }

        val hashSize = storageBinary.slice(1 until 5).toByteArray()
        val hashSizeInt = ByteBuffer.wrap(hashSize, 0, 4).int

        var hashField = storageBinary.slice(5 until storageBinary.size).toByteArray()

        if (hashField.size != hashSizeInt)
        {
            return -1L
        }

        if (String(hashField) != srcParam)
        {
            return -1L
        }

        return  retFee
    }

    return -1L
}

private fun isOriginationSlcTagCorrect(data: ByteArray, srcParam:String, balanceParam:Long, tz3:String, limit:Long):Long
{
    var i = 0

    val isOriginationTag = data[i++]

    if (isOriginationTag.compareTo(109) == 0)
    {
        var contract= data.slice(i+1 until i+21).toByteArray()
        //contract = contract.dropWhile { it == "0".toByte() }.toByteArray()

        val isContractValid = srcParam == CryptoUtils.genericHashToPkh(contract)
        if (!isContractValid)
        {
            return -1L
        }

        i+=21

        val size = data.size
        val fee = data.slice((i) until size).toByteArray()

        val feeList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(fee[i])

            feeList.add(bytePos)
            i++

        } while (bytePos > 128)

        //val dstFees = dstObj["fee"] as String

        val retFee = addBytesLittleEndian(feeList)

        val counter = fee.slice(i until fee.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(counter[i])
            i++

        } while (bytePos >= 128)

        val gasLimit = counter.slice(i until counter.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(gasLimit[i])
            i++

        } while (bytePos >= 128)


        val storageLimit = gasLimit.slice(i until gasLimit.size).toByteArray()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(storageLimit[i])
            i++

        } while (bytePos >= 128)

        val balance = storageLimit.slice(i until storageLimit.size).toByteArray()

        val balanceList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(balance[i])

            balanceList.add(bytePos)
            i++

        } while (bytePos >= 128)

        val isBalanceValid =  addBytesLittleEndian(balanceList) == balanceParam
        if (!isBalanceValid)
        {
            return -1L
        }

        val delegatableField = balance.slice(i until balance.size).toByteArray()
        i = 0
        val isDelegatableFieldValid = Utils.byteToUnsignedInt(delegatableField[i++]).compareTo(255) == 0
        if (!isDelegatableFieldValid)
        {
            //return -1L
        }

        val script = delegatableField.slice(i until delegatableField.size).toByteArray()

        //TODO verify the script
        val scriptSize = script.slice(0 until 4).toByteArray()
        val scriptSizeInt = ByteBuffer.wrap(scriptSize,0,4).int

        val scriptCodeField = script.slice(4 until script.size).toByteArray()

        val scriptCode = scriptCodeField.slice(0 until scriptSizeInt).toByteArray()

        val slcBinary = byteArrayOfInts(

                2,
                0,
                0,
                6,
                126,
                5,
                1,
                7,
                101,
                7,
                101,
                4,
                93,
                0,
                0,
                0,
                12,
                37,
                106,
                111,
                117,
                114,
                110,
                97,
                108,
                105,
                101,
                114,
                101,
                7,
                101,
                7,
                101,
                4,
                106,
                0,
                0,
                0,
                14,
                37,
                102,
                111,
                110,
                100,
                115,
                95,
                114,
                101,
                115,
                116,
                97,
                110,
                116,
                4,
                91,
                0,
                0,
                0,
                17,
                37,
                100,
                117,
                114,
                101,
                101,
                95,
                100,
                101,
                95,
                98,
                108,
                111,
                99,
                97,
                103,
                101,
                8,
                101,
                5,
                95,
                7,
                101,
                3,
                107,
                3,
                106,
                5,
                95,
                7,
                101,
                3,
                107,
                3,
                106,
                0,
                0,
                0,
                5,
                37,
                102,
                105,
                108,
                101,
                7,
                101,
                4,
                93,
                0,
                0,
                0,
                10,
                37,
                109,
                97,
                105,
                116,
                114,
                101,
                115,
                115,
                101,
                8,
                101,
                3,
                98,
                3,
                98,
                0,
                0,
                0,
                4,
                37,
                115,
                101,
                108,
                5,
                0,
                7,
                100,
                4,
                108,
                0,
                0,
                0,
                8,
                37,
                100,
                101,
                102,
                97,
                117,
                108,
                116,
                7,
                100,
                8,
                101,
                7,
                101,
                4,
                92,
                0,
                0,
                0,
                14,
                37,
                99,
                108,
                101,
                102,
                95,
                112,
                117,
                98,
                108,
                105,
                113,
                117,
                101,
                3,
                103,
                7,
                100,
                7,
                101,
                7,
                101,
                4,
                93,
                0,
                0,
                0,
                12,
                37,
                106,
                111,
                117,
                114,
                110,
                97,
                108,
                105,
                101,
                114,
                101,
                7,
                101,
                7,
                101,
                4,
                106,
                0,
                0,
                0,
                14,
                37,
                102,
                111,
                110,
                100,
                115,
                95,
                114,
                101,
                115,
                116,
                97,
                110,
                116,
                4,
                91,
                0,
                0,
                0,
                17,
                37,
                100,
                117,
                114,
                101,
                101,
                95,
                100,
                101,
                95,
                98,
                108,
                111,
                99,
                97,
                103,
                101,
                8,
                101,
                5,
                95,
                7,
                101,
                3,
                107,
                3,
                106,
                5,
                95,
                7,
                101,
                3,
                107,
                3,
                106,
                0,
                0,
                0,
                5,
                37,
                102,
                105,
                108,
                101,
                4,
                93,
                0,
                0,
                0,
                24,
                37,
                110,
                111,
                117,
                118,
                101,
                108,
                108,
                101,
                95,
                99,
                108,
                101,
                102,
                95,
                109,
                97,
                105,
                116,
                114,
                101,
                115,
                115,
                101,
                7,
                101,
                7,
                94,
                3,
                108,
                5,
                95,
                3,
                109,
                4,
                93,
                0,
                0,
                0,
                23,
                37,
                110,
                111,
                117,
                118,
                101,
                108,
                108,
                101,
                95,
                99,
                108,
                101,
                102,
                95,
                112,
                117,
                98,
                108,
                105,
                113,
                117,
                101,
                0,
                0,
                0,
                21,
                37,
                97,
                112,
                112,
                101,
                108,
                95,
                99,
                108,
                101,
                102,
                95,
                109,
                97,
                105,
                116,
                114,
                101,
                115,
                115,
                101,
                8,
                101,
                7,
                101,
                6,
                95,
                7,
                101,
                4,
                106,
                0,
                0,
                0,
                8,
                37,
                109,
                111,
                110,
                116,
                97,
                110,
                116,
                6,
                90,
                3,
                108,
                0,
                0,
                0,
                13,
                37,
                98,
                101,
                110,
                101,
                102,
                105,
                99,
                105,
                97,
                105,
                114,
                101,
                0,
                0,
                0,
                14,
                37,
                98,
                101,
                110,
                101,
                102,
                105,
                99,
                105,
                97,
                105,
                114,
                101,
                115,
                4,
                93,
                0,
                0,
                0,
                25,
                37,
                110,
                111,
                118,
                101,
                108,
                108,
                101,
                95,
                99,
                108,
                101,
                102,
                95,
                106,
                111,
                117,
                114,
                110,
                97,
                108,
                105,
                101,
                114,
                101,
                7,
                101,
                4,
                92,
                0,
                0,
                0,
                14,
                37,
                99,
                108,
                101,
                102,
                95,
                112,
                117,
                98,
                108,
                105,
                113,
                117,
                101,
                3,
                103,
                0,
                0,
                0,
                9,
                37,
                116,
                114,
                97,
                110,
                115,
                102,
                101,
                114,
                5,
                2,
                2,
                0,
                0,
                4,
                -122,
                2,
                0,
                0,
                0,
                43,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                5,
                31,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                2,
                0,
                0,
                4,
                65,
                7,
                46,
                2,
                0,
                0,
                0,
                6,
                3,
                32,
                5,
                61,
                3,
                109,
                2,
                0,
                0,
                4,
                47,
                7,
                46,
                2,
                0,
                0,
                1,
                65,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                76,
                3,
                76,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                5,
                31,
                2,
                0,
                0,
                0,
                -119,
                3,
                76,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                5,
                31,
                2,
                0,
                0,
                0,
                78,
                3,
                33,
                3,
                12,
                5,
                31,
                2,
                0,
                0,
                0,
                63,
                5,
                31,
                2,
                0,
                0,
                0,
                54,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                3,
                33,
                5,
                31,
                2,
                0,
                0,
                0,
                10,
                7,
                67,
                3,
                98,
                0,
                2,
                3,
                18,
                3,
                66,
                3,
                12,
                3,
                117,
                3,
                73,
                3,
                66,
                3,
                12,
                3,
                26,
                3,
                76,
                3,
                26,
                3,
                33,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                3,
                33,
                3,
                43,
                2,
                0,
                0,
                0,
                30,
                2,
                0,
                0,
                0,
                4,
                3,
                25,
                3,
                37,
                7,
                44,
                2,
                0,
                0,
                0,
                0,
                2,
                0,
                0,
                0,
                9,
                2,
                0,
                0,
                0,
                4,
                3,
                79,
                3,
                39,
                3,
                24,
                7,
                44,
                2,
                0,
                0,
                0,
                2,
                3,
                32,
                2,
                0,
                0,
                0,
                2,
                3,
                39,
                7,
                46,
                2,
                0,
                0,
                0,
                40,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                5,
                31,
                2,
                0,
                0,
                0,
                6,
                3,
                66,
                3,
                76,
                3,
                32,
                5,
                61,
                3,
                109,
                2,
                0,
                0,
                0,
                38,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                5,
                31,
                2,
                0,
                0,
                0,
                4,
                3,
                66,
                3,
                76,
                3,
                79,
                3,
                38,
                2,
                0,
                0,
                2,
                -30,
                5,
                31,
                2,
                0,
                0,
                0,
                48,
                2,
                0,
                0,
                0,
                43,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                5,
                31,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                3,
                33,
                3,
                12,
                5,
                31,
                2,
                0,
                0,
                0,
                32,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                76,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                1,
                -51,
                5,
                31,
                2,
                0,
                0,
                0,
                -82,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                76,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                27,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                3,
                33,
                3,
                43,
                2,
                0,
                0,
                0,
                30,
                2,
                0,
                0,
                0,
                4,
                3,
                25,
                3,
                37,
                7,
                44,
                2,
                0,
                0,
                0,
                0,
                2,
                0,
                0,
                0,
                9,
                2,
                0,
                0,
                0,
                4,
                3,
                79,
                3,
                39,
                5,
                112,
                0,
                5,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                5,
                31,
                2,
                0,
                0,
                0,
                42,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                5,
                31,
                2,
                0,
                0,
                0,
                10,
                3,
                33,
                7,
                67,
                3,
                98,
                0,
                2,
                3,
                18,
                3,
                66,
                3,
                66,
                5,
                113,
                0,
                6,
                3,
                12,
                3,
                117,
                3,
                73,
                3,
                66,
                3,
                12,
                3,
                26,
                3,
                26,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                11,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                33,
                3,
                24,
                7,
                44,
                2,
                0,
                0,
                0,
                2,
                3,
                32,
                2,
                0,
                0,
                0,
                2,
                3,
                39,
                5,
                31,
                2,
                0,
                0,
                0,
                -33,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                -65,
                3,
                76,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                7,
                67,
                3,
                89,
                3,
                10,
                5,
                52,
                2,
                0,
                0,
                0,
                -114,
                7,
                45,
                2,
                0,
                0,
                0,
                63,
                3,
                33,
                3,
                22,
                3,
                64,
                2,
                0,
                0,
                0,
                52,
                3,
                25,
                3,
                40,
                7,
                44,
                2,
                0,
                0,
                0,
                28,
                3,
                23,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                11,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                18,
                7,
                67,
                3,
                89,
                3,
                10,
                2,
                0,
                0,
                0,
                8,
                3,
                27,
                7,
                67,
                3,
                89,
                3,
                3,
                2,
                0,
                0,
                0,
                67,
                7,
                45,
                2,
                0,
                0,
                0,
                39,
                5,
                61,
                7,
                101,
                3,
                107,
                3,
                106,
                3,
                76,
                3,
                27,
                3,
                76,
                5,
                82,
                2,
                0,
                0,
                0,
                2,
                3,
                27,
                5,
                61,
                7,
                101,
                3,
                107,
                3,
                106,
                3,
                76,
                7,
                67,
                3,
                89,
                3,
                10,
                2,
                0,
                0,
                0,
                16,
                5,
                61,
                7,
                101,
                3,
                107,
                3,
                106,
                3,
                33,
                7,
                67,
                3,
                89,
                3,
                3,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                76,
                3,
                76,
                7,
                67,
                3,
                106,
                0,
                0,
                5,
                61,
                3,
                109,
                5,
                82,
                2,
                0,
                0,
                0,
                51,
                2,
                0,
                0,
                0,
                18,
                2,
                0,
                0,
                0,
                13,
                3,
                33,
                3,
                22,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                23,
                3,
                33,
                5,
                31,
                2,
                0,
                0,
                0,
                6,
                3,
                79,
                3,
                77,
                3,
                27,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                4,
                3,
                76,
                3,
                18,
                5,
                31,
                2,
                0,
                0,
                0,
                82,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                71,
                3,
                76,
                3,
                33,
                3,
                64,
                3,
                18,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                48,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                76,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                28,
                3,
                76,
                3,
                33,
                5,
                31,
                2,
                0,
                0,
                0,
                17,
                3,
                76,
                3,
                66,
                3,
                76,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                27,
                3,
                66,
                3,
                75,
                3,
                76,
                3,
                66,
                3,
                66,
                3,
                66,
                2,
                0,
                0,
                0,
                11,
                5,
                31,
                2,
                0,
                0,
                0,
                2,
                3,
                66,
                3,
                66

        )

        if (!scriptCode.contentEquals(slcBinary))
        {
            return -1L
        }

        val storageField = scriptCodeField.slice(scriptSizeInt until scriptCodeField.size).toByteArray()

        val storageSize = storageField.slice(0 until 4).toByteArray()
        val storageSizeInt = ByteBuffer.wrap(storageSize, 0, 4).int

        val storageBinary = storageField.slice(4 until storageField.size).toByteArray()

        if (storageBinary.size != storageSizeInt)
        {
            return -1L
        }

        val storage =

        Primitive(Primitive.Name.Pair,
                arrayOf(
                        Primitive(Primitive.Name.Pair,
                                arrayOf(
                                        Visitable.string(tz3),
                                        Primitive(Primitive.Name.Pair,
                                                arrayOf(
                                                        Primitive(Primitive.Name.Pair,
                                                                arrayOf(
                                                                        Visitable.integer(limit),
                                                                        Visitable.integer(86400)
                                                                )
                                                        ),
                                                        Primitive(Primitive.Name.Pair,
                                                                arrayOf(
                                                                        Visitable.sequenceOf(),
                                                                        Visitable.sequenceOf()
                                                                )
                                                        )
                                                )
                                        )
                                )
                        ),

                        Primitive(Primitive.Name.Pair,
                                arrayOf(
                                        Visitable.string(srcParam),
                                        Primitive(Primitive.Name.Pair,
                                                arrayOf(
                                                        Visitable.integer(0),
                                                        Visitable.integer(1)
                                                )
                                        )
                                )
                        )
                )
        )

        val packer = Packer(ByteArrayOutputStream())
        storage.accept(packer)

        val binaryStorage = (packer.output as ByteArrayOutputStream).toByteArray()
        if (!storageBinary.contentEquals(binaryStorage))
        {
            return -1L
        }

        return  retFee
    }

    return -1L
}

fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

fun isOriginatePayloadValid(payload:String, params: JSONObject):Boolean
{
    var isValid = false
    val data = payload.hexToByteArray()

    val obj = params["dsts"] as JSONArray
    val dstObj = obj[0] as JSONObject

    val dataField = data.slice(32 until data.size).toByteArray()

    val revealFees = isRevealTagCorrect(dataField, params["src"] as String, params["src_pk"] as String)
    if (revealFees.first != -1L)
    {
        val transactionByteArray = revealFees.second

        val srcParam = params["src"] as String

        val originationFees = isOriginationTagCorrect(transactionByteArray!!, srcParam, dstObj["balance"] as Long, dstObj["delegate"] as String)
        if (originationFees != -1L)
        {
            val totalFees = revealFees.first + originationFees
            if (totalFees == dstObj["fee"])
            {
                return true
            }
        }
    }
    else
    {
        val originationFees = isOriginationTagCorrect(dataField, params["src"] as String, dstObj["balance"] as Long, dstObj["delegate"] as String)
        if (originationFees != -1L)
        {
            if (originationFees == dstObj["fee"])
            {
                return true
            }
        }
    }
    return isValid
}

fun isOriginateSlcPayloadValid(payload:String, params: JSONObject):Boolean
{
    var isValid = false
    val data = payload.hexToByteArray()

    val obj = params["dsts"] as JSONArray
    val dstObj = obj[0] as JSONObject

    val dataField = data.slice(32 until data.size).toByteArray()

    val revealFees = isRevealTagCorrect(dataField, params["src"] as String, params["src_pk"] as String)
    if (revealFees.first != -1L)
    {
        val transactionByteArray = revealFees.second

        val srcParam = params["src"] as String

        val originationFees = isOriginationSlcTagCorrect(transactionByteArray!!, srcParam, dstObj["balance"] as Long, dstObj["tz3"] as String, dstObj["limit"] as Long)
        if (originationFees != -1L)
        {
            val totalFees = revealFees.first + originationFees
            if (totalFees == dstObj["fee"])
            {
                return true
            }
        }
    }
    else
    {
        val originationFees = isOriginationSlcTagCorrect(dataField, params["src"] as String, dstObj["balance"] as Long, dstObj["tz3"] as String, dstObj["limit"] as Long)
        if (originationFees != -1L)
        {
            if (originationFees == dstObj["fee"])
            {
                return true
            }
        }
    }
    return isValid
}

fun isAddDelegatePayloadValid(payload:String, params: JSONObject):Boolean
{
    var isValid = false
    if (payload != null && params != null)
    {
        val data = payload.hexToByteArray()

        val obj = params["dsts"] as JSONArray
        val dstObj = obj[0] as JSONObject

        val dataField = data.slice(32 until data.size).toByteArray()

        val revealFees = isRevealTagCorrect(dataField, params["src"] as String, params["src_pk"] as String)
        if (revealFees.first != -1L)
        {
            val transactionByteArray = revealFees.second

            val srcParam = params["src"] as String
            val dstParam = dstObj["dst"] as String

            val dstAccount =
                    if (dstObj.has("dst_account"))
                    {
                        dstObj["dst_account"] as String?
                    }
                    else
                    {
                        null
                    }

            val amountTransfer =
                    if (dstObj.has("transfer_amount"))
                    {
                        dstObj["transfer_amount"] as Long?
                    }
                    else
                    {
                        null
                    }

            val contractType =
                    if (dstObj.has("contract_type"))
                    {
                        dstObj["contract_type"] as String?
                    }
                    else
                    {
                        null
                    }

            val edsig =
                    if (dstObj.has("edsig"))
                    {
                        dstObj["edsig"] as String?
                    }
                    else
                    {
                        null
                    }

            val transactionFees = isTransactionTagCorrect(transactionByteArray!!, srcParam, dstParam, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType, params["src_pk"] as String, edsig)
            if (transactionFees != -1L)
            {
                val totalFees = revealFees.first + transactionFees
                if (totalFees == dstObj["fee"])
                {
                    return true
                }
            }
        }
        else
        {
            val dstAccount =
                    if (dstObj.has("dst_account"))
                    {
                        dstObj["dst_account"] as String?
                    }
                    else
                    {
                        null
                    }

            val amountTransfer =
                    if (dstObj.has("transfer_amount"))
                    {
                        dstObj["transfer_amount"] as Long?
                    }
                    else
                    {
                        null
                    }

            val contractType =
                    if (dstObj.has("contract_type"))
                    {
                        dstObj["contract_type"] as String?
                    }
                    else
                    {
                        null
                    }

            val edsig =
                    if (dstObj.has("edsig"))
                    {
                        dstObj["edsig"] as String?
                    }
                    else
                    {
                        null
                    }

            val transactionFees = isTransactionTagCorrect(dataField, params["src"] as String, dstObj["dst"] as String, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType, params["src_pk"] as String, edsig)
            if (transactionFees != -1L)
            {
                if (transactionFees == dstObj["fee"])
                {
                    return true
                }
            }
        }
    }
    return isValid
}

private fun addBytesLittleEndian(bytes:ArrayList<Int>):Long
{
    val reversed = bytes.reversed()

    var accum = 0L

    for (i in reversed.indices)
    {
        val bytePos = reversed[i]

        if (bytePos < 128L)
        {
            accum += bytePos
            if (i != reversed.size - 1)
            {
                accum *= 128
            }
        }
        else
        {
            accum += bytePos - 128
            if (i != reversed.size - 1)
            {
                accum *= 128
            }
        }
    }

    return accum
}

fun ecKeyFormat(ecKey:ECPublicKey):ByteArray
{
    var x = ecKey.w.affineX.toByteArray()
    if (x[0].toInt() == 0)
    {
        val tmp = ByteArray(x.size - 1)
        System.arraycopy(x, 1, tmp, 0, tmp.size)
        x = tmp
    }

    var y = ecKey.w.affineY

    var yEvenOdd = if (y.rem(BigInteger.valueOf(2L)) == BigInteger.ZERO)
    {
        "0x02".hexToByteArray()
    }
    else
    {
        "0x03".hexToByteArray()
    }

    val xLen = x.size

    val yLen = yEvenOdd.size
    val result = ByteArray(yLen + xLen)

    System.arraycopy(yEvenOdd, 0, result, 0, yLen)
    System.arraycopy(x, 0, result, yLen, xLen)

    return result
}

fun compressFormat(data: ByteArray):ByteArray
{
    /*

    When encoded in DER, this (signature) becomes the following sequence of bytes:

0x30 b1 0x02 b2 (vr) 0x02 b3 (vs)

where:

b1 is a single byte value, equal to the length, in bytes, of the remaining list of bytes (from the first 0x02 to the end of the encoding);
b2 is a single byte value, equal to the length, in bytes, of (vr);
b3 is a single byte value, equal to the length, in bytes, of (vs);
(vr) is the signed big-endian encoding of the value "r", of minimal length;
(vs) is the signed big-endian encoding of the value "s", of minimal length.

     */

    // I take the fourth byte to take the next vr.

    val vLengthData = data[3]

    val rest = data.slice(4 until data.size).toByteArray()
    //val rest2 = data.slice(4 until data.size).toByteArray()

    val rLength = Utils.byteToUnsignedInt(vLengthData)

    var v = rest.slice(0 until rLength).toByteArray()
    if (v[0].toInt() == 0)
    {
        val tmp = ByteArray(v.size - 1)
        System.arraycopy(v, 1, tmp, 0, tmp.size)
        v = tmp
    }

    //after that, there is a 0x02.

    val restR = rest.slice(rLength until rest.size).toByteArray()

    var r = restR.slice(2 until restR.size).toByteArray()
    if (r[0].toInt() == 0)
    {
        val tmp = ByteArray(r.size - 1)
        System.arraycopy(r, 1, tmp, 0, tmp.size)
        r = tmp
    }

    return v + r
}


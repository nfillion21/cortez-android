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

            val transactionFees = isTransactionTagCorrect(transactionByteArray!!, srcParam, dstParam, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType)
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

            val transactionFees = isTransactionTagCorrect(dataField, params["src"] as String, dstObj["dst"] as String, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType)
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

            val transactionFees = isTransactionTagCorrect(transactionByteArray!!, srcParam, dstParam, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType)
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

            val transactionFees = isTransactionTagCorrect(dataField, params["src"] as String, dstObj["dst"] as String, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType)
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
            CryptoUtils.genericHashToPkh(contractParse)
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
        var publicKey = storageLimit.slice(i+1 until storageLimit.size).toByteArray()
        publicKey = publicKey.dropWhile { it == "0".toByte() }.toByteArray()

        val publicKeyParse = publicKey.slice(0 .. 31).toByteArray()

        val binaryToPk = CryptoUtils.genericHashToPk(publicKeyParse)
        val isPublicKeyValid = srcPk == binaryToPk

        if (!isPublicKeyValid)
        {
            return Pair(-1L, null)
        }

        i = 32
        val nextField = publicKey.slice(i until publicKey.size).toByteArray()

        return Pair(fees, nextField)
    }

    return Pair(-1L, null)
}

private fun isTransactionTagCorrect(payload: ByteArray, srcParam:String, dstParam:String, amountParam:Long, amountDstParam:Long?, dstAccountParam:String?, contractType:String?):Long
{
    //TODO handle if delegation is correct, return the fees, or do something to add the fees with reveal.

    var i = 0

    val firstTag = payload[i++]
    if (firstTag.compareTo(108) == 0)
    {

        var src = payload.slice((i+1)..(21)).toByteArray()
        src = src.dropWhile { it == "0".toByte() }.toByteArray()

        val isSrcValid = srcParam == if (srcParam.startsWith("KT1", true)) CryptoUtils.genericHashToKT(src) else CryptoUtils.genericHashToPkh(src)

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

        val k = addBytesLittleEndian(amountList)

        val isAmountValid = addBytesLittleEndian(amountList) == amountParam
        if (!isAmountValid)
        {
            return -1L
        }

        val dstOrigin = amount.slice(i until amount.size).toByteArray()

        var dst = dstOrigin.slice(1..21).toByteArray()
        dst = dst.dropWhile { it == "0".toByte() }.toByteArray()

        val beginsWith = dstParam.slice(0 until 3)

        val begins = when (beginsWith.toLowerCase(Locale.US))
        {
            "kt1" -> CryptoUtils.genericHashToKT(dst)
            "tz1" -> CryptoUtils.genericHashToPkh(dst)
            "tz2" -> CryptoUtils.genericHashToPkhTz2(dst)
            "tz3" -> CryptoUtils.genericHashToPkhTz3(dst)
            else -> null
        }
        val isDstValid = dstParam == begins

        if (!isDstValid)
        {
            return -1L
        }

        val parametersField = dstOrigin.slice(22 until dstOrigin.size).toByteArray()

        i = 0
        val isDelegatableFieldValid = Utils.byteToUnsignedInt(parametersField[i++]).compareTo(255) == 0
        if (!isDelegatableFieldValid)
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
            /*

[
{"prim": "DROP"},
{"prim": "NIL", "args": [{"prim": "operation"}]},
{
    "prim": "NONE",
    "args":[
    {"prim": "key_hash"}
    ]
},
{"prim": "SET_DELEGATE"},
{"prim": "CONS"}
]
            */
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
            /*

    [
                {"prim": "DROP"},
                {"prim": "NIL", "args": [{"prim": "operation"}]},
                {
                    "prim": "PUSH",
                    "args":[
                        {"prim": "key_hash"},
                        {"string": "%1$s"}
                    ]
                },
                {"prim": "SOME"},
                {"prim": "SET_DELEGATE"},
                {"prim": "CONS"}
            ]
            */
            //TODO add some more contract types
            else -> {
                val sendToBegins = dstAccountParam!!.slice(0 until 3)
                parameters = when (sendToBegins.toLowerCase(Locale.US)) {
                    "kt1" -> {

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

                                        /*
                            {"prim": "CONTRACT", "args": [{"prim": "unit"}]},

                            [{ "prim": "IF_NONE",
                                "args": [

                                [
                                    [
                                        {"prim": "UNIT"},
                                        {"prim": "FAILWITH"}
                                    ]
                                ],
                                []
                                ]
                            }
                            ],
                                    */


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
                                ))

                    }
                    else -> {
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
                                ))
                    }
                }

            }
        }

        val packer = Packer(ByteArrayOutputStream())
        parameters.accept(packer)

        val binary = (packer.output as ByteArrayOutputStream).toByteArray()

        if (!parametersDataField.contentEquals(binary))
        {
            return -1L
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

fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

fun isOriginatePayloadValid(payload:String, params: JSONObject):Boolean
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

            val transactionFees = isTransactionTagCorrect(transactionByteArray!!, srcParam, dstParam, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType)
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

            val transactionFees = isTransactionTagCorrect(dataField, params["src"] as String, dstObj["dst"] as String, dstObj["amount"] as Long, amountTransfer, dstAccount, contractType)
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

    val thirty = "0x30".hexToByteArray()

    val two = "0x02".hexToByteArray()

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


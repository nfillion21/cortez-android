package com.tezos.ui.utils

import com.tezos.core.crypto.CryptoUtils
import com.tezos.core.utils.Utils
import org.json.JSONArray
import org.json.JSONObject

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

            val transactionFees = isTransactionTagCorrect(transactionByteArray!!, srcParam, dstParam, dstObj["amount"] as Long)
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
            val transactionFees = isTransactionTagCorrect(dataField, params["src"] as String, dstObj["dst"] as String, dstObj["amount"] as Long)
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

private fun isRevealTagCorrect(payload: ByteArray, src:String, srcPk:String):Pair<Long, ByteArray?>
{
    var i = 0

    //Reveal Tag 7
    val firstTag = payload[i++]
    if (firstTag.compareTo(7) == 0)
    {
        val contract = payload.slice(i until payload.size).toByteArray()

        i = 22
        val contractParse = contract.slice((if (src.startsWith("KT1", true)) 1 else 2) until i).toByteArray()

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
        val publicKey = storageLimit.slice(i+1 until storageLimit.size).toByteArray()
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

private fun isTransactionTagCorrect(payload: ByteArray, srcParam:String, dstParam:String, amountParam:Long):Long
{
    //TODO handle if delegation is correct, return the fees, or do something to add the fees with reveal.

    var i = 0

    val firstTag = payload[i++]
    if (firstTag.compareTo(8) == 0)
    {

        val src = payload.slice((i+(if (srcParam.startsWith("KT1", true)) 1 else 2))..(i+(if (srcParam.startsWith("KT1", true)) 1 else 2)+19)).toByteArray()

        val isSrcValid = srcParam == if (srcParam.startsWith("KT1", true)) CryptoUtils.genericHashToKT(src) else CryptoUtils.genericHashToPkh(src)

        if (!isSrcValid)
        {
            return -1L
        }

        val size = payload.size
        val fee = payload.slice((i+2+19+1) until size).toByteArray()

        val feeList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(fee[i])

            feeList.add(bytePos)
            i++

        } while (bytePos > 128)

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

        val dst = amount.slice(i+(if (dstParam.startsWith("KT1", true)) 1 else 2) until amount.size).toByteArray()
        //TODO handle the first two bytes

        val beginsWith = dstParam.slice(0 until 3)

        val begins = when (beginsWith.toLowerCase())
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

        return retFee
    }

    return -1L
}

private fun isDelegationTagCorrect(payload: ByteArray, src:String):Long
{
    //TODO handle if delegation is correct, return the fees, or do something to add the fees with reveal.

    var i = 0

    val firstTag = payload[i++]
    if (firstTag.compareTo(10) == 0)
    {
        val contract = payload.slice(i until payload.size).toByteArray()

        i = 22
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

    if (isOriginationTag.compareTo(9) == 0)
    {
        val src = data.slice((i+2)..(i+2+19)).toByteArray()

        val isSrcValid = srcParam == CryptoUtils.genericHashToPkh(src)

        if (!isSrcValid)
        {
            return -1L
        }

        val size = data.size
        val fee = data.slice((i+2+19+1) until size).toByteArray()

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

        //TODO on recupere les 21 prochains bytes

        val mngrPubKey = storageLimit.slice(i+1 .. i+1+20).toByteArray()
        //TODO handle the first byte associated to tz1 (ed255-19)

        val isMngrPubKeyValid = srcParam == CryptoUtils.genericHashToPkh(mngrPubKey)

        if (!isMngrPubKeyValid)
        {
            return -1L
        }

        val balance = storageLimit.slice(i+1+20 until storageLimit.size).toByteArray()

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

        val spendable = balance.slice(i until balance.size).toByteArray()
        i = 0
        val isSpendable = Utils.byteToUnsignedInt(spendable[i++]).compareTo(255) == 0
        if (!isSpendable)
        {
            return -1L
        }

        val delegatable = spendable.slice(i until spendable.size).toByteArray()
        i = 0
        val isDelegatable = Utils.byteToUnsignedInt(delegatable[i++]).compareTo(255) == 0
        if (!isDelegatable)
        {
            return -1L
        }

        val delegatableField = delegatable.slice(i until delegatable.size).toByteArray()
        i = 0
        val isDelegatableFieldValid = Utils.byteToUnsignedInt(delegatableField[i++]).compareTo(255) == 0
        if (!isDelegatableFieldValid)
        {
            return -1L
        }

        val delegate = delegatableField.slice(i until delegatableField.size).toByteArray()
        val delegateParse = delegate.slice(1 .. 20).toByteArray()

        val beginsWith = delegateParam.slice(0 until 3)

        val cryptoDelegate = when (beginsWith.toLowerCase())
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

        val isScriptNull = (script[0]).compareTo(0) == 0
        if (!isScriptNull)
        {
            return -1L
        }
        return  retFee
    }

    return -1L
}

fun isAddDelegatePayloadValid(payload:String, params: JSONObject):Boolean
{
    if (payload != null && params != null)
    {
        val data = payload.hexToByteArray()

        val obj = params["dsts"] as JSONArray
        val dstObj = obj[0] as JSONObject

        val dataField = data.slice(32 until data.size).toByteArray()

        val revealFees = isRevealTagCorrect(dataField, params["src"] as String, params["src_pk"] as String)
        if (revealFees.first != -1L)
        {
            val delegationByteArray = revealFees.second

            val originationFees = isOriginationTagCorrect(delegationByteArray!!, params["src"] as String, dstObj["balance"] as Long, dstObj["delegate"] as String)
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
    return false
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


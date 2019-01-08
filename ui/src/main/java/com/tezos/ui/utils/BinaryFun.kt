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

fun isPayloadValid(payload:String, params: JSONObject):Boolean
{
    var isValid = false
    if (payload != null && params != null)
    {
        val data = payload.hexToByteArray()

        val obj = params["dsts"] as JSONArray
        val dstObj = obj[0] as JSONObject

        var i: Int
        i = when {
            data.size > 100 -> 94
            else -> 32
        }

        val isHeightValid = data[i].compareTo(8) == 0
        if (!isHeightValid)
        {
            return false
        }


        val src = data.slice((i+3)..(i+3+19)).toByteArray()

        val pkh = params["src"]



        val isSrcValid = pkh == CryptoUtils.genericHashToPkh(src)

        if (!isSrcValid)
        {
            return false
        }

        val size = data.size
        val fee = data.slice((i+3+19+1) until size).toByteArray()

        val feeList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(fee[i])

            feeList.add(bytePos)
            i++

        } while (bytePos > 128)

        val dstFees = dstObj["fee"] as String

        val isFeesValid = addBytesLittleEndian(feeList) == dstFees.toLong()

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


        val amount = storageLimit.slice(i until storageLimit.size).toByteArray()

        val amountList = ArrayList<Int>()
        i = 0
        do
        {
            val bytePos = Utils.byteToUnsignedInt(amount[i])

            amountList.add(bytePos)
            i++

        } while (bytePos >= 128)

        val dstAmount = dstObj["amount"] as String

        val isAmountValid = addBytesLittleEndian(amountList) == dstAmount.toLong()
        if (!isAmountValid)
        {
            return false
        }


        val dst = amount.slice(i+2 until amount.size).toByteArray()
        //TODO handle the first two bytes

        val dstPkh = dstObj["dst"]
        val isDstValid = dstPkh == CryptoUtils.genericHashToPkh(dst)

        isValid = isHeightValid && isSrcValid && isFeesValid && isAmountValid && isDstValid

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


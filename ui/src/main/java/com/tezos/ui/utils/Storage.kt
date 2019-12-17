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

package com.tezos.ui.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.google.gson.Gson
import java.io.Serializable
import java.util.*

/**
 * Stores application data like password hash.
 */
class Storage constructor(context: Context) {

    private val settings: SharedPreferences
    private val mnemonics: SharedPreferences

    private val gson: Gson by lazy(LazyThreadSafetyMode.NONE) { Gson() }

    data class MnemonicsData(
            val pkh: String,
            val pk:String,
            val mnemonics: String) : Serializable

    companion object {
        private const val STORAGE_SETTINGS: String = "settings"
        private const val STORAGE_PASSWORD: String = "password"
        private const val STORAGE_MNEMONICS: String = "mnemonics"
        private const val STORAGE_FINGERPRINT: String = "fingerprint_allowed"

        const val TAG: String = "storage_tag"

        fun toBundle(mnemonicsData: MnemonicsData): Bundle {
            val serializer = SeedDataSerialization(mnemonicsData)
            return serializer.getSerializedBundle()
        }

        fun fromBundle(bundle: Bundle): MnemonicsData {
            val mapper = SeedDataMapper(bundle)
            return mapper.mappedObjectFromBundle()
        }
    }

    init {
        settings = context.getSharedPreferences(STORAGE_SETTINGS, Context.MODE_PRIVATE)
        mnemonics = context.getSharedPreferences(STORAGE_MNEMONICS, Context.MODE_PRIVATE)
    }

    fun isPasswordSaved(): Boolean {
        return settings.contains(STORAGE_PASSWORD)
    }

    fun savePassword(password: String) {
        settings.edit().putString(STORAGE_PASSWORD, password).apply()
    }

    fun getPassword(): String = settings.getString(STORAGE_PASSWORD, "")!!

    fun saveFingerprintAllowed(allowed: Boolean) {
        settings.edit().putBoolean(STORAGE_FINGERPRINT, allowed).apply()
    }

    fun isFingerprintAllowed(): Boolean {
        return settings.getBoolean(STORAGE_FINGERPRINT, false)
    }

    fun hasSeed(alias: String): Boolean
    {
        return mnemonics.contains(alias)
    }

    fun hasMnemonics(): Boolean
    {
        return mnemonics.all.isNotEmpty()
    }

    fun saveSeed(mnemonics: MnemonicsData)
    {
        this.mnemonics.edit().putString(mnemonics.pkh, gson.toJson(mnemonics)).apply()
    }

    fun removeSeed(alias: String) {
        mnemonics.edit().remove(alias).apply()
    }

    fun removeSeed()
    {
        if (hasMnemonics())
        {
            var mnemonicsDataOld = getMnemonics()
            val mnemonicsData = MnemonicsData(mnemonicsDataOld.pkh, mnemonicsDataOld.pk,"")
            this.mnemonics.edit().putString(mnemonicsDataOld.pkh, gson.toJson(mnemonicsData)).apply()
        }
    }

    fun getMnemonicsList(): List<MnemonicsData> {
        val secretsList = ArrayList<MnemonicsData>()
        val seedAliases = mnemonics.all

        seedAliases
                .map { gson.fromJson(it.value as String, MnemonicsData::class.java) }
                .forEach { secretsList.add(it) }
        return secretsList
    }

    fun getMnemonics(): MnemonicsData {
        val secretsList = ArrayList<MnemonicsData>()
        val seedAliases = mnemonics.all

        seedAliases
                .map { gson.fromJson(it.value as String, MnemonicsData::class.java) }
                .forEach { secretsList.add(it) }
        return secretsList[0]
    }

    fun clear()
    {
        settings.edit().clear().apply()
        mnemonics.edit().clear().apply()
    }

    internal class SeedDataSerialization internal constructor(private val mnemonicsData: MnemonicsData)
    {
        internal fun getSerializedBundle():Bundle
        {
            val secretDataBundle = Bundle()

            secretDataBundle.putString("pkh", mnemonicsData.pkh)
            secretDataBundle.putString("mnemonics", mnemonicsData.mnemonics)

            return secretDataBundle
        }
    }

    internal class SeedDataMapper internal constructor(private val bundle: Bundle)
    {
        internal fun mappedObjectFromBundle(): MnemonicsData
        {
            val pkh = this.bundle.getString("pkh", null)
            val pk = this.bundle.getString("pk", null)
            val seed = this.bundle.getString("mnemonics", null)

            return MnemonicsData(pkh, pk, seed)
        }
    }
}
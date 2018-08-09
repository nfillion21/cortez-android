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

    data class SeedData(
            val pkh: String,
            val seed: String) : Serializable

    companion object {
        private const val STORAGE_SETTINGS: String = "settings"
        private const val STORAGE_ENCRYPTION_KEY: String = "encryption_key"
        private const val STORAGE_PASSWORD: String = "password"
        private const val STORAGE_MNEMONICS: String = "mnemonics"
        private const val STORAGE_FINGERPRINT: String = "fingerprint_allowed"

        const val TAG: String = "storage_tag"

        fun toBundle(seedData: SeedData): Bundle {
            val serializer = SeedDataSerialization(seedData)
            return serializer.getSerializedBundle()
        }

        fun fromBundle(bundle: Bundle): SeedData {
            val mapper = SeedDataMapper(bundle)
            return mapper.mappedObjectFromBundle()
        }
    }

    init {
        settings = context.getSharedPreferences(STORAGE_SETTINGS, android.content.Context.MODE_PRIVATE)
        mnemonics = context.getSharedPreferences(STORAGE_MNEMONICS, android.content.Context.MODE_PRIVATE)
    }

    fun saveEncryptionKey(key: String) {
        settings.edit().putString(STORAGE_ENCRYPTION_KEY, key).apply()
    }

    fun getEncryptionKey(): String = settings.getString(STORAGE_ENCRYPTION_KEY, "")

    fun isPasswordSaved(): Boolean {
        return settings.contains(STORAGE_PASSWORD)
    }

    fun savePassword(password: String) {
        settings.edit().putString(STORAGE_PASSWORD, password).apply()
    }

    fun getPassword(): String = settings.getString(STORAGE_PASSWORD, "")

    fun saveFingerprintAllowed(allowed: Boolean) {
        settings.edit().putBoolean(STORAGE_FINGERPRINT, allowed).apply()
    }

    fun isFingerprintAllowed(): Boolean {
        return settings.getBoolean(STORAGE_FINGERPRINT, false)
    }

    fun hasSeed(alias: String): Boolean {
        return mnemonics.contains(alias)
    }

    fun saveSeed(seed: SeedData) {
        mnemonics.edit().putString(seed.pkh, gson.toJson(seed)).apply()
    }

    fun removeSeed(alias: String) {
        mnemonics.edit().remove(alias).apply()
    }

    fun getMnemonicsList(): List<SeedData> {
        val secretsList = ArrayList<SeedData>()
        val seedAliases = mnemonics.all

        seedAliases
                .map { gson.fromJson(it.value as String, SeedData::class.java) }
                .forEach { secretsList.add(it) }
        return secretsList
    }

    fun getMnemonics(): SeedData {
        val secretsList = ArrayList<SeedData>()
        val seedAliases = mnemonics.all

        seedAliases
                .map { gson.fromJson(it.value as String, SeedData::class.java) }
                .forEach { secretsList.add(it) }
        return secretsList[0]
    }

    fun clear()
    {
        settings.edit().clear().apply()
        mnemonics.edit().clear().apply()
    }

    internal class SeedDataSerialization internal constructor(private val seedData: SeedData)
    {
        internal fun getSerializedBundle():Bundle
        {
            val secretDataBundle = Bundle()

            secretDataBundle.putString("pkh", seedData.pkh)
            secretDataBundle.putString("seed", seedData.seed)

            return secretDataBundle
        }
    }

    internal class SeedDataMapper internal constructor(private val bundle: Bundle)
    {
        internal fun mappedObjectFromBundle(): SeedData
        {
            val alias = this.bundle.getString("pkh", null)
            val seed = this.bundle.getString("seed", null)

            return SeedData(alias, seed)
        }
    }
}
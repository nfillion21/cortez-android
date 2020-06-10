package com.tezos.core.models

import android.os.Bundle
import com.tezos.core.utils.DataExtractor
import org.json.JSONArray
import org.json.JSONObject

data class Contract
(
        val blk: String,
        val mgr: String,
        val spendable: Boolean,
        val delegatable: Boolean,
        val delegate: String?,
        val script: String,
        val storage: String
) {

    companion object
    {
        @JvmStatic
        fun fromBundle(bundle: Bundle): Contract
        {
            val blk = bundle.getString("blk", null)
            val mgr = bundle.getString("mgr", null)
            val spendable = bundle.getBoolean("spendable", false)
            val delegatable = bundle.getBoolean("delegatable", false)
            val delegate = bundle.getString("delegate", null)
            val script = bundle.getString("script", null)
            val storage = bundle.getString("storage", null)

            return Contract(
                    blk = blk,
                    mgr = mgr,
                    spendable = spendable,
                    delegatable = delegatable,
                    delegate = delegate,
                    script = script,
                    storage = storage
            )
        }

        fun fromJSONArray(answer: JSONArray): Contract
        {
            val contractJSON = DataExtractor.getJSONObjectFromField(answer,0)

            val blk = DataExtractor.getStringFromField(contractJSON, "blk")
            val mgr = DataExtractor.getStringFromField(contractJSON, "mgr")
            val spendable = DataExtractor.getBooleanFromField(contractJSON, "spendable")
            val delegatable = DataExtractor.getBooleanFromField(contractJSON, "delegatable")
            val delegate = DataExtractor.getStringFromField(contractJSON, "delegate")
            val script = DataExtractor.getJSONObjectFromField(contractJSON, "script")

            val storage =
                    if (DataExtractor.getJSONObjectFromField(contractJSON, "storage") != null)
                    {
                        DataExtractor.getJSONObjectFromField(contractJSON, "storage")
                    }
                    else
                    {
                        DataExtractor.getJSONObjectFromField(script, "storage")
                    }

            return Contract(
                    blk = blk,
                    mgr = mgr,
                    spendable = spendable,
                    delegatable = delegatable,
                    delegate = delegate,
                    script = script.toString(),
                    storage = storage.toString()
            )
        }
    }

    fun toBundle(): Bundle
    {
        val contractBundle = Bundle()

        contractBundle.putString("blk", blk)
        contractBundle.putString("mgr", blk)
        contractBundle.putBoolean("spendable", spendable)
        contractBundle.putBoolean("delegatable", delegatable)
        contractBundle.putString("delegate", delegate)
        contractBundle.putString("script", script)
        contractBundle.putString("storage", storage)

        return contractBundle
    }
}

package com.tezos.ui.database

import android.os.Bundle
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.util.HashMap

@IgnoreExtraProperties
data class MultisigOperation
(
        // hexa to sign
        var binary: String,

        // creation
        var timestamp: Long,

        var notary: String,

        var signatures: HashMap<String, String> = HashMap()
) {

    @Exclude
    fun toMap(): Map<String, Any?>
    {
        return mapOf (
                "binary_operation" to binary,
                "timestamp" to timestamp,
                "notary" to notary,
                "signatures" to signatures
        )
    }

    @Exclude
    fun toBundle(): Bundle
    {
        val ongoingOperationBundle = Bundle()

        ongoingOperationBundle.putString("binary_operation", binary)
        ongoingOperationBundle.putLong("timestamp", timestamp)
        ongoingOperationBundle.putString("notary", notary)
        ongoingOperationBundle.putSerializable("signatures", signatures)

        return ongoingOperationBundle
    }

    companion object {
        @JvmStatic
        @Exclude
        fun fromMap(hashMap: HashMap<String, Any>): MultisigOperation
        {
            return MultisigOperation (
                    binary = hashMap["binary_operation"] as String,
                    timestamp = hashMap["timestamp"] as Long,
                    notary = hashMap["notary"] as String,
                    signatures = hashMap["signatures"] as HashMap<String, String>
            )
        }

        fun fromBundle(bundle: Bundle): MultisigOperation
        {
            val binary = bundle.getString("binary")
            val timestamp = bundle.getLong("timestamp")
            val notary = bundle.getString("notary")
            val signatures = bundle.getSerializable("signatures")

            return MultisigOperation (
                    binary = binary,
                    timestamp = timestamp,
                    notary = notary,
                    signatures = signatures as HashMap<String, String>
            )
        }
    }
}

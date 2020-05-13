package com.tezos.ui.database

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.util.HashMap

// [START blog_user_class]
@IgnoreExtraProperties
data class MultisigOperation
(
        // hexa to sign
        var binary: String,

        // creation
        var timestamp: Long,

        var notary: String,

        var signatures: MutableMap<String, String> = HashMap()
) {

    // [START post_to_map]
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

    companion object {
        @JvmStatic
        @Exclude
        fun fromMap(hashMap: HashMap<String, Any>): MultisigOperation
        {
            return MultisigOperation (
                    binary = hashMap["binary_operation"] as String,
                    timestamp = hashMap["timestamp"] as Long,
                    notary = hashMap["notary"] as String,
                    signatures = hashMap["signatures"] as MutableMap<String, String>
            )
        }
    }

    // [END post_to_map]
}
// [END blog_user_class]

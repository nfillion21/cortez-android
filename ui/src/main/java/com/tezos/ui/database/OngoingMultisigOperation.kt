package com.tezos.ui.database

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.util.HashMap

// [START blog_user_class]
@IgnoreExtraProperties
data class OngoingMultisigOperation
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
    // [END post_to_map]
}
// [END blog_user_class]

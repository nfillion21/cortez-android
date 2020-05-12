package com.tezos.ui.database

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.util.HashMap

// [START blog_user_class]
@IgnoreExtraProperties
data class OngoingMultisigOperation
(
        // identifier
        var uid: String?,

        // kt1
        var address: String?,

        // hexa to sign
        var binary: String?,

        // creation
        var timestamp: Long?,

        var notary: String?,

        var signatures: List<Signature> = ArrayList()
) {

    // [START post_to_map]
    @Exclude
    fun toMap(): Map<String, Any?>
    {
        return mapOf (
                "uid" to uid,
                "contract_address" to address,
                "binary_operation" to binary,
                "timestamp" to timestamp,
                "notary" to notary,
                "signatures" to signatures
        )
    }
    // [END post_to_map]
}
// [END blog_user_class]

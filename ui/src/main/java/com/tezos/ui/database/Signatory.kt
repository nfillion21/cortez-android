package com.tezos.ui.database

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

// [START comment_class]
@IgnoreExtraProperties
data class Signatory(
        var uid: String?,
        var pk: String?

        // list of signatories id
        //var operations: List<OngoingMultisigOperation> = HashMap()

) {

    // [START post_to_map]
    @Exclude
    fun toMap(): Map<String, Any?>
    {
        return mapOf (
                "uid" to uid,
                "pk" to pk
        )
    }
    // [END post_to_map]
}

// [END comment_class]

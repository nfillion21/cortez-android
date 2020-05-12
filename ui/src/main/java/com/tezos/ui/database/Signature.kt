package com.tezos.ui.database

import com.google.firebase.database.IgnoreExtraProperties

// [START comment_class]
@IgnoreExtraProperties
data class Signature(
        var pk: String?,
        var signature: String?
)
// [END comment_class]

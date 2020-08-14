package com.example.mediabox.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaBucket(
    val bucketId: Int,
    val displayName: String,
    var size: Int,
    var uri: Uri? = null
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MediaBucket

        if (bucketId != other.bucketId) return false

        return true
    }

    override fun hashCode(): Int {
        return bucketId
    }
}
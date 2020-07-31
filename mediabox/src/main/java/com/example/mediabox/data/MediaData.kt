package com.example.mediabox.data

import android.net.Uri
import android.os.Parcelable
import com.example.mediabox.ext.JPG
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaData(
    val uri: Uri,
    var width: Int = 0,
    var height: Int = 0,
    val mimeType: String = JPG
) : Parcelable
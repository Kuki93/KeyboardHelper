package com.example.mediabox.data

import android.net.Uri
import android.os.Parcelable
import com.example.mediabox.ext.JPG
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.io.InputStream

@Parcelize
data class MediaData(
    val id: Int,
    val uri: Uri,
    var width: Int = 0,
    var height: Int = 0,
    var mimeType: String = JPG
) : Parcelable {
    companion object {
        val EMPTY = MediaData(-1, Uri.EMPTY)
    }
    @IgnoredOnParcel
    var selectIndex = 0

    @IgnoredOnParcel
    var sourceIndex = 0

    @IgnoredOnParcel
    var curSelected = false

    @IgnoredOnParcel
    var selected = true

    @IgnoredOnParcel
    var notFound = false
}
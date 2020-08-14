package com.example.mediabox.data

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import kotlinx.android.parcel.Parcelize

internal const val MEDIA_TYPE_PICTURE = 0
internal const val MEDIA_TYPE_VIDEO = 1
internal const val MEDIA_TYPE_PICTURE_AND_VIDEO = 2

@IntDef(MEDIA_TYPE_PICTURE, MEDIA_TYPE_VIDEO, MEDIA_TYPE_PICTURE_AND_VIDEO)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
internal annotation class MediaBoxType

internal class MediaConfig(
    @MediaBoxType val mediaType: Int,
    val maxSelectCount: Int,
    val withEdit: Boolean,
    val withCompress: Boolean,
    val withGif: Boolean,
    val withOrigin: Boolean,
    val withShareAlbum: Boolean,
    val withCamera: Boolean,
    val withSingleCrop: CropConfig?, // maxSelectCount == 1，单选模式下，确定后是否进入裁剪
    val withTheme: MediaTheme?
) : Parcelable {

    var andOrigin = false

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readParcelable(CropConfig::class.java.classLoader),
        parcel.readParcelable(MediaTheme::class.java.classLoader)
    ) {
        andOrigin = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(mediaType)
        parcel.writeInt(maxSelectCount)
        parcel.writeByte(if (withEdit) 1 else 0)
        parcel.writeByte(if (withCompress) 1 else 0)
        parcel.writeByte(if (withGif) 1 else 0)
        parcel.writeByte(if (withOrigin) 1 else 0)
        parcel.writeByte(if (withShareAlbum) 1 else 0)
        parcel.writeByte(if (withCamera) 1 else 0)
        parcel.writeParcelable(withSingleCrop, flags)
        parcel.writeParcelable(withTheme, flags)
        parcel.writeByte(if (andOrigin) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaConfig> {

        override fun createFromParcel(parcel: Parcel): MediaConfig {
            return MediaConfig(parcel)
        }

        override fun newArray(size: Int): Array<MediaConfig?> {
            return arrayOfNulls(size)
        }

        internal val DEFAULT = MediaConfig(
            MEDIA_TYPE_PICTURE,
            9,
            true,
            true,
            true,
            true,
            false,
            true,
            null,
            null
        )
    }

}

@Parcelize
internal data class CropConfig(
    val x: Boolean = false
) : Parcelable


@Parcelize
internal data class MediaTheme(
    val x: Boolean = false
) : Parcelable

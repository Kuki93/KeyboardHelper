package com.example.mediabox

import android.app.Activity
import android.content.Intent
import com.example.mediabox.MediaBoxViewModel.Companion.MEDIA_CONFIG_KEY
import com.example.mediabox.data.*
import kotlin.ranges.IntRange
import androidx.annotation.IntRange as AIntRange

class MediaBox private constructor(@MediaBoxType private val mediaType: Int) {

    companion object {

        private const val MEDIA_DEFAULT_SELECT_COUNT = 9

        internal fun Intent.obtainMediaConfig(): MediaConfig {
            return getParcelableExtra(MEDIA_CONFIG_KEY) ?: MediaConfig.DEFAULT
        }

        fun with(@MediaBoxType mediaType: Int = MEDIA_TYPE_PICTURE): MediaBox {
            return MediaBox(mediaType)
        }
    }

    private var maxSelectCount: Int = MEDIA_DEFAULT_SELECT_COUNT

    private var withEdit: Boolean = true

    private var withCompress: Boolean = true

    private var withGif: Boolean = true

    private var withOrigin: Boolean = true

    private var withShareAlbum: Boolean = false

    private var withCamera: Boolean = true

    private var withSingleCrop: CropConfig? = null

    private var withTheme: MediaTheme? = null

    fun start(activity: Activity) {
        activity.startActivity(Intent(activity, SelectMediaActivity::class.java).apply {
            putExtra(
                MEDIA_CONFIG_KEY, MediaConfig(
                    mediaType,
                    maxSelectCount,
                    withEdit,
                    withCompress,
                    withGif,
                    withOrigin,
                    withShareAlbum,
                    withCamera,
                    withSingleCrop,
                    withTheme
                )
            )
        })
    }

    fun setMediaType(mediaType: Int): MediaBox {
        maxSelectCount = mediaType
        return this
    }

    fun setMaxSelectCount(@AIntRange(from = 1, to = 18) count: Int): MediaBox {
        if (count !in IntRange(1, 18)) {
            throw IllegalArgumentException("")
        }
        maxSelectCount = count
        return this
    }

    fun setWithEdit(with: Boolean): MediaBox {
        withEdit = with
        return this
    }

    fun setWithCompress(with: Boolean): MediaBox {
        withCompress = with
        return this
    }

    fun setWithGif(with: Boolean): MediaBox {
        withGif = with
        return this
    }

    fun setWithOrigin(with: Boolean): MediaBox {
        withOrigin = with
        return this
    }

    fun setWithShareAlbum(with: Boolean): MediaBox {
        withShareAlbum = with
        return this
    }

    fun setWithCamera(with: Boolean): MediaBox {
        withCamera = with
        return this
    }
}
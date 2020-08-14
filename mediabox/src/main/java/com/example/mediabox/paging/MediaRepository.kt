package com.example.mediabox.paging

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.mediabox.data.MediaConfig

internal class MediaRepository(
    context: Context,
    config: MediaConfig,
    mediaUri: Uri,
    private var bucket: String?
) {

    private var initialLoadSize = 60

    val mediaData by lazy {
        Pager(PagingConfig(pageSize = 100, initialLoadSize = 60)) {
            MediaListDataSource(
                context,
                config,
                mediaUri,
                bucket,
                initialLoadSize
            )
        }.flow
    }

    fun refresh(initSize: Int? = null) {
        this.initialLoadSize = initSize ?: 60
    }

    fun refresh(bucket: String?) {
        this.bucket = bucket
    }
}
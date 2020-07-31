package com.example.mediabox.paging

import android.content.Context
import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.example.mediabox.paging.MediaListDataSource

class MediaRepository(
    context: Context,
    mediaUri: Uri,
    private var bucket: String?,
    excludeGif: Boolean
) {

    val mediaData by lazy {
        Pager(PagingConfig(pageSize = 30)) {
            MediaListDataSource(
                context,
                mediaUri,
                bucket,
                excludeGif
            )
        }.flow
    }

    fun refresh(bucket: String?) {
        this.bucket = bucket
    }
}
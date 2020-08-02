package com.example.mediabox.paging

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.paging.PagingSource
import com.example.mediabox.data.MediaData
import com.example.mediabox.ext.GIF
import com.example.mediabox.ext.JPG
import com.example.mediabox.ext.checkMediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaListDataSource(
    context: Context,
    private val mediaUri: Uri,
    private val bucket: String?,
    private val excludeGif: Boolean
) : PagingSource<Int, MediaData>() {

    private val mContentResolver = context.contentResolver

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaData> {
        val offset = params.key ?: 0
        return try {
            val ret = requestData(params.loadSize, offset)
            val nextKey = if (ret.size == params.loadSize) offset.plus(params.loadSize) else null
            LoadResult.Page(ret, null, nextKey)
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    /**
     * 考虑用协程
     */
    private suspend fun requestData(pageSize: Int, offset: Int) = withContext(Dispatchers.IO) {
        val values = mutableListOf<MediaData>()
        val projection = arrayOf(MediaStore.Images.ImageColumns._ID)
        val selection = if (excludeGif) {
            if (bucket.isNullOrEmpty()) {
                MediaStore.Images.Media.MIME_TYPE + "!=?"
            } else {
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "=? and " + MediaStore.Images.Media.MIME_TYPE + "!=?"
            }
        } else {
            if (bucket.isNullOrEmpty()) {
                null
            } else {
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "=?"
            }
        }
        val args = if (excludeGif) {
            if (bucket.isNullOrEmpty()) {
                arrayOf(GIF)
            } else {
                arrayOf(bucket, GIF)
            }
        } else {
            if (bucket.isNullOrEmpty()) {
                null
            } else {
                arrayOf(bucket)
            }
        }
        val sort =
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC limit $pageSize offset $offset"
        val cursor = mContentResolver?.query(
            mediaUri, projection, selection, args, sort
        )?.apply {
            while (moveToNext()) {
                val id = getLong(getColumnIndex(MediaStore.Images.Media._ID))
                val uri = ContentUris.withAppendedId(mediaUri, id)
                val mimeType = mContentResolver.getType(uri)?.takeIf { it.isNotEmpty() } ?: JPG
                values.add(MediaData(uri, mimeType = mimeType).also {
                    it.checkMediaInfo(mContentResolver)
                })
            }
        }
        cursor?.close()
        values.toList()
    }

}
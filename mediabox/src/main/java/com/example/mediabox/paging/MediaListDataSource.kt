package com.example.mediabox.paging

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.mediabox.data.MediaConfig
import com.example.mediabox.data.MediaData
import com.example.mediabox.ext.GIF
import com.example.mediabox.ext.JPG
import com.example.mediabox.ext.checkMediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import kotlin.concurrent.thread

internal class MediaListDataSource(
    context: Context,
    private val config: MediaConfig,
    private val mediaUri: Uri,
    private val bucket: String?,
    private val initialLoadSize: Int
) : PagingSource<Int, MediaData>() {

    private val mContentResolver = context.contentResolver

    private var handler: MediaHandler? = null

    @ExperimentalPagingApi
    override fun getRefreshKey(state: PagingState<Int, MediaData>): Int? {
        return super.getRefreshKey(state)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaData> {
        val quitCheckToken = createHandler()
        val offset = params.key ?: 0
        return try {
            val firstPage = config.withCamera && params.key == null
            val loadSize = if (params.key == null) initialLoadSize else params.loadSize
            val ret = requestData(firstPage, loadSize, offset)
            val nextKey = if (ret.isEmpty() || (firstPage && ret.size == 1)) {
                null
            } else {
                offset.plus(loadSize)
            }
            LoadResult.Page(ret, null, nextKey)
        } catch (e: Exception) {
            LoadResult.Error(e)
        } finally {
            handler?.apply {
                sendMessage(obtainMessage(MediaHandler.HANDLER_LOOPER_QUIT, quitCheckToken))
            }
        }
    }

    /**
     * 考虑用协程
     */
    private suspend fun requestData(firstPage: Boolean, pageSize: Int, offset: Int) =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(MediaStore.Images.ImageColumns._ID)
            val selection = if (!config.withGif) {
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
            val args = if (!config.withGif) {
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
            (mContentResolver?.query(
                mediaUri, projection, selection, args, sort
            )?.run {
                val values = ArrayList<MediaData>(count)
                if (firstPage) {
                    values.add(MediaData.EMPTY)
                }
                while (moveToNext()) {
                    val id = getInt(getColumnIndex(MediaStore.Images.ImageColumns._ID))
                    val uri = ContentUris.withAppendedId(mediaUri, id.toLong())
                    val mimeType = mContentResolver.getType(uri)?.takeIf { it.isNotEmpty() } ?: JPG
                    values.add(MediaData(id, uri, mimeType = mimeType).also {
                        handler?.takeIf { h ->
                            !h.isQuited
                        }?.apply {
                            sendMessage(obtainMessage(MediaHandler.HANDLER_CHECK_MEDIA, it))
                        } ?: kotlin.run {
                            it.checkMediaInfo(mContentResolver)
                        }
                    })
                }
                close()
                values
            } ?: kotlin.run {
                if (firstPage) {
                    mutableListOf<MediaData>(MediaData.EMPTY)
                } else {
                    emptyList<MediaData>()
                }
            }).toList()
        }

    private fun createHandler(): Long {
        val mHandler = handler
        val token = System.nanoTime()
        if (mHandler == null || mHandler.isQuited) {
            handler = null
            thread {
                Looper.prepare()
                handler = MediaHandler(mContentResolver, token)
                Looper.loop()
            }
        } else {
            mHandler.updateQuitCheckToken(token)
        }
        return token
    }

    private class MediaHandler(
        private val mContentResolver: ContentResolver,
        private var quitCheckToken: Long
    ) : Handler() {

        companion object {
            const val HANDLER_CHECK_MEDIA = 0x01
            const val HANDLER_LOOPER_QUIT = 0x02
        }

        var isQuited = false

        fun updateQuitCheckToken(token: Long) {
            quitCheckToken = token
        }

        override fun handleMessage(msg: Message) {
            if (msg.what == HANDLER_CHECK_MEDIA) {
                (msg.obj as? MediaData)?.also {
                    try {
                        it.checkMediaInfo(mContentResolver)
                    } catch (e: FileNotFoundException) {
                        it.notFound = true
                    }
                }
            } else if (msg.what == HANDLER_LOOPER_QUIT && msg.obj == quitCheckToken) {
                safeExit()
            }
        }

        private fun safeExit() {
            looper.quitSafely()
            isQuited = true
        }
    }

}
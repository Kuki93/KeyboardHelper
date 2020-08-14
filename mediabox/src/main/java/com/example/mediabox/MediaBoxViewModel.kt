package com.example.mediabox

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import com.example.mediabox.data.*
import com.example.mediabox.ext.*
import com.example.mediabox.paging.MediaRepository
import com.yalantis.ucrop.UCrop
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import top.zibin.luban.Luban
import java.io.File

internal class MediaBoxViewModel : ViewModel() {

    companion object {

        internal const val REQUEST_CODE_STORAGE = 0x0099
        internal const val REQUEST_CODE_PREVIEW = 0x00100
        internal const val REQUEST_IMAGE_CAPTURE = 0x0101
        internal const val REQUEST_VIDEO_CAPTURE = 0x0102

        internal const val MEDIA_PICTURE = "media/picture"
        internal const val MEDIA_VIDEO = "media/video"
        internal const val MEDIA_DATA_KEY = "media_data_key"
        internal const val MEDIA_SELECT_KEY = "media_select_key"
        internal const val MEDIA_INDEX_KEY = "media_index_key"
        internal const val MEDIA_CONFIG_KEY = "media_config_key"

        internal const val FRAGMENT_ARGUMENT_DATA_KEY = "fragment_argument_data_key"
        internal const val FRAGMENT_ARGUMENT_SELECT_KEY = "fragment_argument_select_key"
        private const val FRAGMENT_SHOW_KEY = "fragment_show_key"

        fun photoClip(
            activity: Activity,
            contentResolver: ContentResolver,
            uri: Uri,
            mediaType: Int
        ) {
            val mimeType = contentResolver.getType(uri)
            if (mimeType == GIF) {
                return
            }
            val cropFileName = mimeType?.let {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
            }.let {
                if (it.isNullOrEmpty()) {
                    "${System.currentTimeMillis()}.jpg"
                } else {
                    "${System.currentTimeMillis()}.$it"
                }
            }
            val format = when (mimeType) {
                WEBP -> Bitmap.CompressFormat.WEBP
                PNG -> Bitmap.CompressFormat.PNG
                else -> Bitmap.CompressFormat.JPEG
            }
            UCrop.of(
                uri,
                Uri.fromFile(
                    File(getResultDir(activity, mediaType, false), cropFileName)
                )
            )
                .withAspectRatio(1f, 1f)
                .withOptions(UCrop.Options().apply {
                    setToolbarColor(Color.WHITE)
                    setFreeStyleCropEnabled(false)
                    setCompressionFormat(format)
                    setCompressionQuality(75)
                    setStatusBarColor(Color.WHITE)
                    setShowCropGrid(false)
                    setShowCropFrame(false)
                    setCircleDimmedLayer(true)
                })
                .start(activity)
        }

//        fun compressSingle(context: Context, uri: Uri, mediaType: Int): Single<Uri> {
//            return Single.fromCallable {
//                val path = getResultDir(context, mediaType).absolutePath
//                Luban.with(context).setTargetDir(path).load(uri).get().first()
//            }.subscribeOn(Schedulers.io())
//        }
//
//        private fun compressMultiple(
//            context: Context,
//            uris: List<Uri>,
//            mediaType: Int
//        ): Single<List<Uri>> {
//            return Single.fromCallable {
//                val path = getResultDir(context, mediaType).absolutePath
//                Luban.with(context).setTargetDir(path).load(uris).get()
//            }.subscribeOn(Schedulers.io())
//        }

        private fun getResultDir(
            context: Context,
            mediaType: Int,
            share: Boolean = false
        ): File {
            return context.run {
                when (mediaType) {
                    MEDIA_TYPE_VIDEO -> {
                        if (share) {
                            getExternalFilesDir(MediaBoxViewModel.MEDIA_VIDEO) ?: File(
                                filesDir,
                                MediaBoxViewModel.MEDIA_VIDEO
                            )
                        } else {
                            File(externalCacheDir ?: cacheDir, MediaBoxViewModel.MEDIA_VIDEO)
                        }
                    }
                    else -> {
                        if (share) {
                            getExternalFilesDir(MediaBoxViewModel.MEDIA_PICTURE) ?: File(
                                filesDir,
                                MediaBoxViewModel.MEDIA_PICTURE
                            )
                        } else {
                            File(externalCacheDir ?: cacheDir, MediaBoxViewModel.MEDIA_PICTURE)
                        }
                    }
                }.also {
                    if (!it.exists()) {
                        it.mkdirs()
                    }
                }
            }
        }
    }

    internal val buckets: MutableLiveData<List<MediaBucket>> = MutableLiveData()

    internal var mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    lateinit var repository: MediaRepository

    private val defaultBucket = MediaBucket(0, "全部", 0)

    var curBucket: MediaBucket = defaultBucket

    var bucketDisposable: Disposable? = null

    override fun onCleared() {
        super.onCleared()
        bucketDisposable?.dispose()
        bucketDisposable = null
    }

    fun getMediaFlow(
        context: Context,
        config: MediaConfig
    ): Flow<PagingData<MediaData>> {
        mediaUri = when (config.mediaType) {
            MEDIA_TYPE_PICTURE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        if (config.mediaType != MEDIA_TYPE_VIDEO) {
            findAllPhotoBuckets(context)
        } else {
            buckets.postValue(listOf(defaultBucket))
        }
        return MediaRepository(
            context,
            config,
            mediaUri,
            null
        ).let {
            repository = it
            it.mediaData
        }
    }

    fun contentChange(adapter: PagingDataAdapter<*, *>, withCamera: Boolean) {
        repository.refresh(if (withCamera) adapter.itemCount.minus(1) else adapter.itemCount)
        adapter.refresh()
    }

    fun selectBucket(adapter: PagingDataAdapter<*, *>, bucket: MediaBucket) {
        if (curBucket.bucketId == bucket.bucketId) {
            return
        }
        curBucket = bucket
        val bucketName = if (bucket.bucketId == 0) {
            null
        } else {
            bucket.displayName
        }
        repository.refresh(bucketName)
        adapter.refresh()
    }

    fun showSelectMediaBucketFragment(manager: FragmentManager) {
        SelectMediaBucketFragment().apply {
            arguments = Bundle().also {
                it.putInt(FRAGMENT_ARGUMENT_SELECT_KEY, curBucket.bucketId)
                it.putParcelableArrayList(FRAGMENT_ARGUMENT_DATA_KEY, getMediaBucketList())
            }
            show(manager, FRAGMENT_SHOW_KEY)
        }
    }

    fun updateSelectMediaBucketFragment(manager: FragmentManager, list: List<MediaBucket>) {
        manager.findFragmentByTag(FRAGMENT_SHOW_KEY)?.takeIf {
            it.isAdded && it.isVisible
        }?.let {
            it as? SelectMediaBucketFragment
        }?.also {
            it.update(list.toMutableList())
        }
    }

    fun findAllPhotoBuckets(context: Context) {
        bucketDisposable?.dispose()
        bucketDisposable = Single.fromCallable {
            val projection = arrayOf(
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.BUCKET_ID,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME
            )
            val ret = mutableMapOf<String, MediaBucket>()
            val cursor = context.contentResolver?.query(
                mediaUri, projection, null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " desc "
            )?.also { cursor ->
                defaultBucket.size = cursor.count
                while (cursor.moveToNext()) {
                    val id =
                        cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_ID))
                    cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME))
                        ?.takeIf {
                            it.isNotEmpty()
                        }?.also {
                            ret.putOrUpdate(it, {
                                MediaBucket(
                                    cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_ID)),
                                    it,
                                    1,
                                    ContentUris.withAppendedId(
                                        mediaUri,
                                        cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID))
                                    ).also { uri ->
                                        if (cursor.isFirst) {
                                            defaultBucket.uri = uri
                                        }
                                    }
                                )
                            }) { value ->
                                value.size = value.size.plus(1)
                                value
                            }
                        }
                }
            }
            cursor?.close()
            ArrayList<MediaBucket>(ret.size.plus(1)).apply {
                add(defaultBucket)
                ret.flatMapElementTo(this) {
                    it.value
                }
            }
        }.subscribeOn(Schedulers.io())
            .subscribe({
                buckets.postValue(it)
            }, { buckets.postValue(listOf(defaultBucket)) })
    }

    private fun getMediaBucketList(): ArrayList<MediaBucket> {
        return (buckets.value?.toMutableList() ?: mutableListOf(defaultBucket)) as ArrayList
    }
}
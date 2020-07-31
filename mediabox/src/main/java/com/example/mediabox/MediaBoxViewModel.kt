package com.example.mediabox

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import androidx.annotation.IntDef
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import com.example.mediabox.data.MediaData
import com.example.mediabox.paging.MediaRepository
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

class MediaBoxViewModel : ViewModel() {

    companion object {

        const val REQUEST_CODE_STORAGE = 0x0099
        const val MEDIA_PICTURE = "media/picture"
        const val MEDIA_VIDEO = "media/video"
        const val MEDIA_DATA_KEY = "media_data_key"

        const val MEDIA_TYPE_PICTURE = 0
        const val MEDIA_TYPE_VIDEO = 1
        const val MEDIA_TYPE_PICTURE_AND_VIDEO = 2

        @IntDef(MEDIA_TYPE_PICTURE, MEDIA_TYPE_VIDEO, MEDIA_TYPE_PICTURE_AND_VIDEO)
        @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
        annotation class MediaBoxType

        const val defaultBucketName = "全部"
    }

    val buckets: MutableLiveData<List<String>> = MutableLiveData()

    val defaultBuckets = listOf(defaultBucketName)

    var mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    lateinit var repository: MediaRepository

    private var bucketName: String? = null

    private var excludeGif: Boolean = true

    @SuppressLint("CheckResult")
    fun getMediaFlow(
        context: Context,
        mediaType: Int,
        excludeGif: Boolean = true
    ): Flow<PagingData<MediaData>> {
        this.excludeGif = excludeGif
        mediaUri = when (mediaType) {
            MEDIA_TYPE_PICTURE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MEDIA_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
//        if (mediaType != MEDIA_TYPE_VIDEO) {
//            findAllPhotoBuckets(context)
//                .subscribeOn(Schedulers.io())
//                .subscribe({
//                    buckets.postValue(it)
//                }, { buckets.postValue(defaultBuckets) })
//        } else {
//            buckets.postValue(defaultBuckets)
//        }
        return MediaRepository(
            context,
            mediaUri,
            null,
            excludeGif
        ).let {
            repository = it
            it.mediaData
        }
    }

    fun contentChange(adapter: PagingDataAdapter<*, *>) {
        adapter.refresh()
    }

    fun selectBucket(adapter: PagingDataAdapter<*, *>, bucket: String? = null) {
        bucketName = if (defaultBucketName == bucketName) {
            null
        } else {
            bucket
        }
        repository.refresh(bucketName)
        adapter.refresh()
    }

    private fun findAllPhotoBuckets(context: Context): Single<List<String>> {
        return Single.fromCallable {
            val projection = arrayOf(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
            val ret = mutableSetOf(defaultBucketName)
            val cursor = context.contentResolver?.query(
                mediaUri, projection, null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " desc "
            )?.also {
                while (it.moveToNext()) {
                    val bucket =
                        it.getString(it.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME))
                    if (!bucket.isNullOrEmpty()) {
                        ret.add(bucket)
                    }
                }
            }
            cursor?.close()
            ret.toList()
        }
    }
}
package com.example.mediabox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mediabox.adapter.MediaAdapter
import com.example.mediabox.data.MediaData
import com.example.mediabox.ext.GIF
import com.example.mediabox.ext.PNG
import com.example.mediabox.ext.WEBP
import com.example.mediabox.paging.MediaLifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.zibin.luban.Luban
import java.io.File

class MainActivity : AppCompatActivity() {

    inner class MediaContentObserver : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            viewModel.contentChange(mediaAdapter)
        }
    }

    @MediaBoxViewModel.Companion.MediaBoxType
    private var mediaType: Int = MediaBoxViewModel.MEDIA_TYPE_PICTURE

    private val mediaContentObserver by lazy {
        MediaContentObserver()
    }

    private val mediaAdapter by lazy(LazyThreadSafetyMode.NONE) {
        MediaAdapter().apply {
            itemClickListener = { v, data ->
//                photoClip(uri)
                openPreviewMedia(v, data)
            }
        }
    }

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this).get(MediaBoxViewModel::class.java).also {
            it.mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //兼容5.0及以上支持全透明
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
        }

        recyclerview?.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = mediaAdapter
        }

        action_bar_spinner?.setOnClickListener {
//            viewModel.selectBucket(mediaAdapter, "WeiXin")
        }

        if (!checkPermission()) {
            action_bar_spinner?.isEnabled = false
        } else {
            init()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        if (requestCode == MediaBoxViewModel.REQUEST_CODE_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(recyclerview, "需要权限访问相册", Snackbar.LENGTH_INDEFINITE)
                        .setAction("确定") {
                            requestPermissions(
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                MediaBoxViewModel.REQUEST_CODE_STORAGE
                            )
                        }.setDuration(3000)
                        .show()
                }
            } else {
                init()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data)
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(mediaContentObserver)
    }

    private fun init() {
        action_bar_spinner?.isEnabled = true
        lifecycleScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            viewModel.getMediaFlow(this@MainActivity, mediaType, false).collectLatest {
                mediaAdapter.submitData(it)
            }
        }

        contentResolver.registerContentObserver(viewModel.mediaUri, false, mediaContentObserver)
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MediaBoxViewModel.REQUEST_CODE_STORAGE
                )
                return false
            }
        }
        return true
    }

    private fun photoClip(uri: Uri) {
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
                File(getResultDir(false), cropFileName)
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
            .start(this)
    }

    private fun compressSingle(uri: Uri): Single<Uri> {
        return Single.fromCallable {
            val path = getResultDir().absolutePath
            Luban.with(this).setTargetDir(path).load(uri).get().first()
        }.subscribeOn(Schedulers.io())
    }

    private fun compressMultiple(uris: List<Uri>): Single<List<Uri>> {
        return Single.fromCallable {
            val path = getResultDir().absolutePath
            Luban.with(this).setTargetDir(path).load(uris).get()
        }.subscribeOn(Schedulers.io())
    }

    private fun getResultDir(share: Boolean = false): File {
        return when (mediaType) {
            MediaBoxViewModel.MEDIA_TYPE_VIDEO -> {
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

    private fun openPreviewMedia(imageView: View, data: MediaData) {
        val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                imageView,
                imageView.transitionName
            ).toBundle()
        } else {
            null
        }

        if (data.width <= 0 || data.height <= 0) {
            contentResolver?.openFileDescriptor(data.uri, "r")?.use {
                it.fileDescriptor?.let { fd ->
                    BitmapFactory.decodeFileDescriptor(fd)
                }?.also { bitmap ->
                    data.width = bitmap.width
                    data.height = bitmap.height
                    bitmap.recycle()
                }
            }
        }
        Intent(this, PreviewActivity::class.java)
            .putExtra(MediaBoxViewModel.MEDIA_DATA_KEY, data)
            .let {
                startActivity(it, options)
            }
    }

}

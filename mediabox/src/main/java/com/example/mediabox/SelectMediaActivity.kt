package com.example.mediabox

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediabox.MediaBox.Companion.obtainMediaConfig
import com.example.mediabox.adapter.MediaAdapter
import com.example.mediabox.adapter.OnMediaBucketChangeListener
import com.example.mediabox.data.MediaBucket
import com.example.mediabox.data.MediaConfig
import com.example.mediabox.data.MediaData
import com.example.mediabox.ext.*
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_select_media.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class SelectMediaActivity : AppCompatActivity(), OnMediaBucketChangeListener {

    private val mediaConfig: MediaConfig by lazy(LazyThreadSafetyMode.NONE) {
        intent.obtainMediaConfig()
    }

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this).get(MediaBoxViewModel::class.java).also {
            it.mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    private val mediaAdapter by lazy(LazyThreadSafetyMode.NONE) {
        MediaAdapter(mediaConfig.maxSelectCount).apply {
            itemClickListener = { longClick, position, data ->
                if (data == MediaData.EMPTY) {
                    if (longClick) {
                        dispatchTakeVideoIntent()
                    } else {
                        dispatchTakePictureIntent()
                    }
                } else {
                    getSources().also {
                        openPreviewMedia(
                            if (mediaConfig.withCamera) position.minus(1) else position,
                            it.first,
                            it.second
                        )
                    }
                }
            }

            selectCountChangeListener = { count, max ->
                if (count <= 0) {
                    btn_done.isEnabled = false
                    btn_done.text = "确定"
                    btn_preview.isEnabled = false
                    btn_preview.text = "预览"
                } else {
                    btn_done.isEnabled = true
                    btn_done.text = "预览($count/$max)"
                    btn_preview.isEnabled = true
                    btn_preview.text = "预览($count/$max)"
                }
            }
        }
    }

    private val mediaContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            viewModel.findAllPhotoBuckets(this@SelectMediaActivity)
            viewModel.contentChange(mediaAdapter, mediaConfig.withCamera)
            contentChange = true
        }
    }

    private val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            if (contentChange) {
                mediaAdapter.updateSelect(positionStart, itemCount)
                contentChange = false
            }
        }
    }

    private var cameraPhotoURI: Pair<Uri, String>? = null

    private var contentChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_media)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
        }
        recyclerview?.apply {
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    if (parent.getChildLayoutPosition(view).rem(4) != 3) {
                        outRect.right = dip(1)
                    }
                    outRect.bottom = dip(1)
                }
            })
            layoutManager = GridLayoutManager(this@SelectMediaActivity, 4)
            adapter = mediaAdapter
        }

        btn_done?.setOnClickListener {

        }

        toolbar?.setNavigationOnClickListener {
            finish()
        }

        layout_select?.setOnClickListener {
            viewModel.showSelectMediaBucketFragment(supportFragmentManager)
        }

        btn_preview?.setOnClickListener {
            openPreviewMedia(
                position = -1,
                sources = mediaAdapter.getSelectSources()
            )
        }

        btn_origin?.visibility = if (mediaConfig.withOrigin) {
            btn_origin?.setOnClickListener {
                mediaConfig.andOrigin = !mediaConfig.andOrigin
                it.isSelected = mediaConfig.andOrigin
            }
            View.VISIBLE
        } else {
            View.GONE
        }

        if (checkPermission()) {
            init()
        }

        mediaAdapter.registerAdapterDataObserver(adapterDataObserver)
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
                if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
                    || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ) {
                    Snackbar.make(recyclerview, "需要权限访问相册", Snackbar.LENGTH_INDEFINITE)
                        .setAction("确定") {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ),
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
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                UCrop.REQUEST_CROP -> {
                    data ?: return
                    val resultUri = UCrop.getOutput(data)
                }
                MediaBoxViewModel.REQUEST_CODE_PREVIEW -> {
                    data?.apply {
                        if (mediaConfig.withOrigin) {
                            mediaConfig.andOrigin = obtainMediaConfig().andOrigin
                            btn_origin.isSelected = mediaConfig.andOrigin
                        }
                        getIntArrayExtra(MediaBoxViewModel.MEDIA_SELECT_KEY)?.takeIf {
                            it.isNotEmpty()
                        }?.toMutableSet()?.also {
                            mediaAdapter.diffUpdate(it)
                        } ?: kotlin.run {
                            mediaAdapter.clearSelect()
                        }
                    }
                }
                MediaBoxViewModel.REQUEST_IMAGE_CAPTURE -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        cameraPhotoURI?.second?.also {
                            galleryAddPic(true, it)
                        }
                    } else {
                        cameraPhotoURI?.first?.also {
                            mediaAdapter.addCameraURI(it)
                        }
                    }
                }
                else -> {

                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == MediaBoxViewModel.REQUEST_IMAGE_CAPTURE) {
                cameraPhotoURI?.also {
                    deleteFile(it.first, it.second)
                    cameraPhotoURI = null
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            data ?: return
            val cropError = UCrop.getError(data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaAdapter.unregisterAdapterDataObserver(adapterDataObserver)
        contentResolver.unregisterContentObserver(mediaContentObserver)
    }

    override fun onChange(bucket: MediaBucket) {
        contentChange = false
        viewModel.selectBucket(mediaAdapter, bucket)
        tv_select?.text = viewModel.curBucket.displayName
    }

    private fun init() {
        tv_select?.text = viewModel.curBucket.displayName

        viewModel.buckets.observe(this, Observer {
            viewModel.updateSelectMediaBucketFragment(supportFragmentManager, it)
        })

        lifecycleScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            viewModel.getMediaFlow(this@SelectMediaActivity, mediaConfig).collectLatest {
                mediaAdapter.submitData(it)
            }
        }

        contentResolver.registerContentObserver(viewModel.mediaUri, false, mediaContentObserver)
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermission(Manifest.permission_group.STORAGE)) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    MediaBoxViewModel.REQUEST_CODE_STORAGE
                )
                return false
            }
        }
        return true
    }

    private fun openPreviewMedia(
        position: Int,
        selectArray: IntArray? = null,
        sources: ArrayList<MediaData>
    ) {
        Intent(this, PreviewActivity::class.java).apply {
            putExtra(MediaBoxViewModel.MEDIA_CONFIG_KEY, mediaConfig)
            putExtra(MediaBoxViewModel.MEDIA_DATA_KEY, sources)
            selectArray?.also {
                putExtra(MediaBoxViewModel.MEDIA_SELECT_KEY, it)
            }
            putExtra(MediaBoxViewModel.MEDIA_INDEX_KEY, position)
            startActivityForResult(this, MediaBoxViewModel.REQUEST_CODE_PREVIEW)
        }
    }


    private fun dispatchTakePictureIntent() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "垃圾手机，居然没有相机", Toast.LENGTH_SHORT).show()
            return
        }
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                Single.fromCallable {
                    createMediaUri()
                }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        it ?: return@subscribe
                        cameraPhotoURI = it
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, it.first)
                        startActivityForResult(
                            takePictureIntent,
                            MediaBoxViewModel.REQUEST_IMAGE_CAPTURE
                        )
                    }, {
                        Toast.makeText(this, "创建文件失败", Toast.LENGTH_SHORT).show()
                    })
            }
        }
    }

    private fun dispatchTakeVideoIntent() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "垃圾手机，居然没有相机", Toast.LENGTH_SHORT).show()
            return
        }
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, MediaBoxViewModel.REQUEST_VIDEO_CAPTURE)
            }
        }
    }

    private fun galleryAddPic(add: Boolean = true, path: String, mimeType: String = JPG) {
        notifySystemMediaV2(
            path,
            mimeType,
            MediaScannerConnection.OnScanCompletedListener { _, uri ->
                if (add) {
                    mediaAdapter.addCameraURI(uri)
                }
            })
    }

    @SuppressLint("CheckResult")
    private fun deleteFile(uri: Uri? = null, path: String? = null) {
        Single.fromCallable {
            var ret = uri?.let {
                deleteFileByUri(it)
            } ?: false

            var p: String? = null
            if (!ret) {
                ret = path?.let {
                    deleteFileByPath(it).also { _ ->
                        p = path
                    }
                } ?: false

                if (!ret && uri != null) {
                    getRealFilePath(uri)?.let {
                        deleteFileByPath(it).also { _ ->
                            p = it
                        }
                    }
                }
            }
            p
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                it ?: return@subscribe
                galleryAddPic(false, it)
            }, {
                it.printStackTrace()
            })
    }

}
package com.example.mediabox

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager2.widget.ViewPager2
import com.example.mediabox.MediaBox.Companion.obtainMediaConfig
import com.example.mediabox.MediaBoxViewModel.Companion.photoClip
import com.example.mediabox.adapter.PreviewAdapter
import com.example.mediabox.adapter.SelectAdapter
import com.example.mediabox.data.MediaConfig
import com.example.mediabox.data.MediaData
import com.example.mediabox.ext.*
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class PreviewActivity : AppCompatActivity() {

    private val screenSize by lazy(LazyThreadSafetyMode.NONE) {
        getScreenSize()
    }

    private val mediaConfig: MediaConfig by lazy(LazyThreadSafetyMode.NONE) {
        intent.obtainMediaConfig()
    }

    private val mediaData: ArrayList<MediaData> by lazy(LazyThreadSafetyMode.NONE) {
        intent.getParcelableArrayListExtra<MediaData>(MediaBoxViewModel.MEDIA_DATA_KEY)
    }

    private val selectLinkedSet: MutableSet<Int> by lazy(LazyThreadSafetyMode.NONE) {
        intent.getIntArrayExtra(MediaBoxViewModel.MEDIA_SELECT_KEY)?.toMutableSet()
            ?: mediaData.map {
                it.id
            }.toMutableSet()
    }

    private val index: Int by lazy(LazyThreadSafetyMode.NONE) {
        intent.getIntExtra(MediaBoxViewModel.MEDIA_INDEX_KEY, 0)
    }

    private val previewAdapter by lazy(LazyThreadSafetyMode.NONE) {
        PreviewAdapter(screenSize, mediaData)
    }

    private val selectAdapter by lazy(LazyThreadSafetyMode.NONE) {
        SelectAdapter(index, AsyncDifferConfig.Builder(
            object : DiffUtil.ItemCallback<MediaData>() {
                override fun areItemsTheSame(
                    oldItem: MediaData,
                    newItem: MediaData
                ): Boolean {
                    return oldItem.id == newItem.id && oldItem.uri.path == newItem.uri.path
                }

                override fun areContentsTheSame(
                    oldItem: MediaData,
                    newItem: MediaData
                ): Boolean {
                    return oldItem.curSelected == newItem.curSelected && oldItem.selected == newItem.selected
                }

                override fun getChangePayload(oldItem: MediaData, newItem: MediaData): Any? {
                    if (oldItem.curSelected != newItem.curSelected
                        || oldItem.selected != newItem.selected
                    ) {
                        return "select"
                    }
                    return super.getChangePayload(oldItem, newItem)
                }
            }
        ).setBackgroundThreadExecutor(Dispatchers.Default.asExecutor())
            .build()).also {
            it.itemClickListener = { index, _ ->
                view_pager.setCurrentItem(index, false)
            }
        }
    }

    private var hasFullscreenFlag = false

    private var gestureDetector: GestureDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        showSystemUI(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStatusBarColor(this, Color.TRANSPARENT)
        }

        val statusH = getStatusBarHeight(this@PreviewActivity)

        top_action_layout.updateLayoutParams {
            height = height.plus(statusH)
        }

        top_action_layout.also {
            it.updatePadding(top = it.paddingTop.plus(statusH))
        }

//        bottom_action_layout.takeIf {
//            isNavBarVisible(this)
//        }?.also {
//            it.updatePadding(bottom = it.paddingBottom.plus(getNavigationBarHeight(this)))
//        }

        view_pager.offscreenPageLimit = 5
        view_pager.adapter = previewAdapter

        recycler_view_select?.also {
            it.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            it.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    dip(8).apply {
                        outRect.left = this
                        outRect.right = this
                    }
                }
            })
            it.adapter = selectAdapter
            selectAdapter.submitList(emptyList())
        }

        updateSelectCount()

        view_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tv_select.text = "${position.plus(1)}/${mediaData.size}"
                selectAdapter.updateCurrentIndex(position)
                mediaData[position].apply {
                    btn_select?.isSelected = selectLinkedSet.indexOf(id) != -1
                    if (mediaConfig.withEdit || mediaConfig.withOrigin) {
                        val visible = if (isGif()) View.GONE else View.VISIBLE
                        TransitionManager.beginDelayedTransition(bottom_action_layout)
                        if (mediaConfig.withEdit) {
                            btn_edit.visibility = visible
                        }
                        if (mediaConfig.withOrigin) {
                            btn_origin.visibility = visible
                        }
                    }
                }
            }
        })

        view_pager.setCurrentItem(index.coerceAtLeast(0), false)

        if (mediaConfig.withEdit) {
            btn_edit.setOnClickListener {
                photoClip(
                    this,
                    contentResolver,
                    mediaData[view_pager.currentItem].uri,
                    mediaConfig.mediaType
                )
            }
        }

        if (mediaConfig.withOrigin) {
            btn_origin.isSelected = mediaConfig.andOrigin
            btn_origin.setOnClickListener {
                mediaConfig.andOrigin = !mediaConfig.andOrigin
                it.isSelected = mediaConfig.andOrigin
            }
        }

        btn_back.setOnClickListener {
            setResult()
            finish()
        }

        btn_select.setOnClickListener {
            changeSelect(mediaData[view_pager.currentItem])
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                hasFullscreenFlag = false
                stopImmersiveState()
            } else {
                hasFullscreenFlag = true
                startImmersiveState()
            }
        }

        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                if (!hasFullscreenFlag) {
                    hideSystemUI(this@PreviewActivity)
                } else {
                    showSystemUI(this@PreviewActivity)
                }
                return super.onSingleTapUp(e)
            }
        }).also {
            gestureDetector = it
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.y >= bottom_action_layout.top || ev.y <= top_action_layout.bottom) {
            return super.dispatchTouchEvent(ev)
        }
        gestureDetector?.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        setResult()
        super.onBackPressed()
    }

    private fun startImmersiveState() {
        TransitionManager.beginDelayedTransition(root_content_view, TransitionSet().apply {
            addTransition(Fade(Fade.OUT).also {
                it.addTarget(R.id.top_action_layout)
                it.addTarget(R.id.bottom_action_layout)
                it.duration = 350
            })
            addTransition(Slide(Gravity.TOP).also {
                it.addTarget(R.id.top_action_layout)
                it.duration = 800
            })
            addTransition(Slide(Gravity.BOTTOM).also {
                it.addTarget(R.id.bottom_action_layout)
                it.duration = 800
            })
            ordering = TransitionSet.ORDERING_TOGETHER
        })
        top_action_layout.visibility = View.GONE
        bottom_action_layout.visibility = View.GONE
    }

    private fun stopImmersiveState() {
        TransitionManager.beginDelayedTransition(root_content_view, TransitionSet().apply {
            addTransition(Fade(Fade.IN).also {
                it.addTarget(R.id.top_action_layout)
                it.addTarget(R.id.bottom_action_layout)
                it.duration = 300
            })
            addTransition(Slide(Gravity.TOP).also {
                it.addTarget(R.id.top_action_layout)
                it.duration = 600
            })
            addTransition(Slide(Gravity.BOTTOM).also {
                it.addTarget(R.id.bottom_action_layout)
                it.duration = 600
            })
            ordering = TransitionSet.ORDERING_TOGETHER
        })
        top_action_layout.visibility = View.VISIBLE
        bottom_action_layout.visibility = View.VISIBLE
    }

    private fun updateSelectCount(cur: MediaData? = null) {
        val count = selectLinkedSet.size
        if (count <= 0) {
            btn_done.isEnabled = false
            btn_done.text = "确定"
            recycler_view_select.visibility = View.GONE
        } else {
            btn_done.isEnabled = true
            btn_done.text = "确定($count/${mediaConfig.maxSelectCount})"
            recycler_view_select.visibility = View.VISIBLE
            if (index == -1) {
                if (cur == null) {
                    mediaData.forEachIndexed { index, data ->
                        if (index == 0) {
                            data.curSelected = true
                        }
                        data.selectIndex = index
                        data.sourceIndex = index
                    }
                    selectAdapter.submitList(mediaData)
                } else {
                    selectAdapter.notifyItemChanged(cur.sourceIndex, "select")
                }
            } else {
                selectAdapter.submitList(mediaData.filterIndexed { index, data ->
                    val selectIndex = selectLinkedSet.indexOf(data.id)
                    (selectIndex != -1).also {
                        if (data.sourceIndex == index) {
                            data.curSelected = true
                        }
                        data.selectIndex = selectIndex
                        data.sourceIndex = index
                    }
                }.sortedBy {
                    it.selectIndex
                })
            }
        }
        btn_select?.isSelected = selectLinkedSet.indexOf(mediaData[view_pager.currentItem].id) != -1
    }

    private fun changeSelect(data: MediaData) {
        val selectIndex = selectLinkedSet.indexOf(data.id)
        val isSelected = selectIndex != -1
        if (!isSelected && selectLinkedSet.size >= mediaConfig.maxSelectCount) {
            Toast.makeText(this, "你最多只能选择${mediaConfig.maxSelectCount}张图片", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (!isSelected) {
            selectLinkedSet.add(data.id)
        } else {
            selectLinkedSet.remove(data.id)
        }
        if (index == -1) {
            data.selected = !isSelected
        }
        updateSelectCount(data)
    }

    private fun setResult() {
        setResult(Activity.RESULT_OK, Intent().also {
            it.putExtra(MediaBoxViewModel.MEDIA_CONFIG_KEY, mediaConfig)
            it.putExtra(MediaBoxViewModel.MEDIA_SELECT_KEY, selectLinkedSet.toIntArray())
        })
    }
}
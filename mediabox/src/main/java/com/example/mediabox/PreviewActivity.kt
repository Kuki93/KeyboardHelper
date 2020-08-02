package com.example.mediabox

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager2.widget.ViewPager2
import com.example.mediabox.adapter.PreviewAdapter
import com.example.mediabox.data.MediaData
import com.example.mediabox.ext.*
import kotlinx.android.synthetic.main.activity_preview.*


class PreviewActivity : AppCompatActivity() {

    private val screenSize by lazy(LazyThreadSafetyMode.NONE) {
        getScreenSize()
    }

    private val mediaData: ArrayList<MediaData> by lazy(LazyThreadSafetyMode.NONE) {
        intent.getParcelableArrayListExtra<MediaData>(MediaBoxViewModel.MEDIA_DATA_KEY)
    }

    private val index: Int by lazy(LazyThreadSafetyMode.NONE) {
        intent.getIntExtra(MediaBoxViewModel.MEDIA_INDEX_KEY, 0)
    }

    private val previewAdapter by lazy(LazyThreadSafetyMode.NONE) {
        PreviewAdapter(screenSize, mediaData)
    }

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        showSystemUI(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStatusBarColor(this, Color.TRANSPARENT)
        }

        top_action_layout.also {
            it.updatePadding(top = it.paddingTop.plus(getStatusBarHeight(this)))
        }
        bottom_action_layout.also {
            it.updatePadding(bottom = it.paddingBottom.plus(getNavigationBarHeight(this)))
        }

        view_pager.offscreenPageLimit = 5
        view_pager.adapter = previewAdapter
        view_pager.setCurrentItem(index, false)

        tv_select.text = "$index/${mediaData.size}"

        view_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tv_select.text = "${position.plus(1)}/${mediaData.size}"
            }
        })

        btn_back.setOnClickListener {
            finish()
        }

        btn_edit.setOnClickListener {
        }
        btn_select.setOnClickListener {
        }

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                hideSystemUI(this@PreviewActivity)
                return super.onSingleTapUp(e)
            }
        })


        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                stopImmersiveState()
            } else {
                startImmersiveState()
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun startImmersiveState() {
        TransitionManager.beginDelayedTransition(root_content_view, TransitionSet().apply {
            addTransition(Fade(Fade.OUT).also {
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
        top_action_layout.visibility = View.GONE
        bottom_action_layout.visibility = View.GONE
    }

    private fun stopImmersiveState() {
        TransitionManager.beginDelayedTransition(root_content_view, TransitionSet().apply {
            addTransition(Fade(Fade.IN).also {
                it.addTarget(R.id.top_action_layout)
                it.addTarget(R.id.bottom_action_layout)
                it.duration = 250
            })
            addTransition(Slide(Gravity.TOP).also {
                it.addTarget(R.id.top_action_layout)
                it.duration = 500
            })
            addTransition(Slide(Gravity.BOTTOM).also {
                it.addTarget(R.id.bottom_action_layout)
                it.duration = 500
            })
            ordering = TransitionSet.ORDERING_TOGETHER
        })
        top_action_layout.visibility = View.VISIBLE
        bottom_action_layout.visibility = View.VISIBLE
    }
}
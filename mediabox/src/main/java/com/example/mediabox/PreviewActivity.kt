package com.example.mediabox

import android.os.Build
import android.os.Bundle
import android.transition.ArcMotion
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.example.mediabox.data.MediaData
import com.example.mediabox.ext.*
import com.example.mediabox.glide.gif.FrameSequenceDrawable
import com.example.mediabox.view.LagerMediaView
import com.example.mediabox.view.PhotoMediaView
import com.example.mediabox.view.SubsamplingScalePlusImageView
import kotlinx.android.synthetic.main.activity_preview.*
import java.lang.Exception

class PreviewActivity : AppCompatActivity() {

    private lateinit var mediaView: IMediaShowListener

    private val mediaData by lazy(LazyThreadSafetyMode.NONE) {
        intent.getParcelableExtra<MediaData>(MediaBoxViewModel.MEDIA_DATA_KEY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        val screenSize = getScreenSize()
        mediaView = if (!mediaData.isGif() && mediaData.isLagerImage(screenSize)) {
            LagerMediaView(this)
        } else {
            PhotoMediaView(this)
        }

        mediaView.apply {
            val layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
            )
            layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            root_content_view.addView(getImageView(), layoutParams)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            supportPostponeEnterTransition()
            mediaView.getImageView().transitionName = mediaData.uri.path
            if (mediaView is LagerMediaView) {
                (mediaView as LagerMediaView).initLagerState(screenSize, mediaData)
                (mediaView as LagerMediaView).setOnImageEventListener(object :
                    SubsamplingScalePlusImageView.DefaultOnImageEventListener() {
                    override fun onImageLoaded() {
                        supportStartPostponedEnterTransition()
                    }

                    override fun onImageLoadError(e: Exception?) {
                        supportStartPostponedEnterTransition()
                    }
                })
                mediaView.showMediaWithListener(mediaData) {
                    supportStartPostponedEnterTransition()
                }
            } else {
                mediaView.showMediaWithListener(mediaData) {
                    supportStartPostponedEnterTransition()
                }
            }
        } else {
            mediaView.showMediaWithListener(mediaData) {
                (mediaView.getImageView() as? LagerMediaView)?.initLagerState(screenSize, mediaData)
            }
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            // Note that system bars will only be "visible" if none of the
            // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                // TODO: The system bars are visible. Make any desired
                // adjustments to your UI, such as showing the action bar or
                // other navigational controls.
            } else {
                // TODO: The system bars are NOT visible. Make any desired
                // adjustments to your UI, such as hiding the action bar or
                // other navigational controls.
            }
        }
    }

    override fun onStart() {
        super.onStart()
        fetchFrameSequenceDrawable()?.start()
    }

    override fun onStop() {
        super.onStop()
        fetchFrameSequenceDrawable()?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchFrameSequenceDrawable()?.destroy()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setWindowShareElementEnterTransition() {
//        window.sharedElementEnterTransition = TransitionSet()
//            .addTransition(SubsamplingScaleImageViewSharedTransition().also {
//                it.setImageViewScaleType(1)
//                it.setSubsamplingScaleType(0)
//            })
//            .addTransition(ChangeBounds().also {
//                val arcMotion = ArcMotion()
//                arcMotion.maximumAngle = 90f
//                arcMotion.minimumVerticalAngle = 0f
//                arcMotion.minimumHorizontalAngle = 15f
//                it.pathMotion = arcMotion
//            })
//            .setInterpolator(LinearOutSlowInInterpolator())
//            .setOrdering(TransitionSet.ORDERING_TOGETHER)
//            .setDuration(350)
//
//        window.enterTransition = Fade().apply {
//            excludeTarget(android.R.id.statusBarBackground, true)
//            excludeTarget(android.R.id.navigationBarBackground, true)
//            excludeTarget(R.id.action_bar_container, true)
//        }
//        window.sharedElementExitTransition = TransitionSet()
//            .addTransition(SubsamplingScaleImageViewSharedTransition().also {
//                it.setImageViewScaleType(1)
//                it.setSubsamplingScaleType(0)
//            })
//            .addTransition(ChangeBounds().also {
//                val arcMotion = ArcMotion()
//                arcMotion.maximumAngle = 90f
//                arcMotion.minimumVerticalAngle = 0f
//                arcMotion.minimumHorizontalAngle = 15f
//                it.pathMotion = arcMotion
//            })
//            .setInterpolator(LinearOutSlowInInterpolator())
//            .setOrdering(TransitionSet.ORDERING_TOGETHER)
//            .setDuration(350)
//
//        window.exitTransition = Fade().apply {
//            excludeTarget(android.R.id.statusBarBackground, true)
//            excludeTarget(android.R.id.navigationBarBackground, true)
//            excludeTarget(R.id.action_bar_container, true)
//        }
    }

    private fun fetchFrameSequenceDrawable(): FrameSequenceDrawable? {
        return mediaView.takeIf {
            mediaData.isGif()
        }?.getSourceDrawable()?.let {
            it as? FrameSequenceDrawable
        }
    }


    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            window.statusBarColor = Color.TRANSPARENT
//            window.navigationBarColor = Color.TRANSPARENT
//        }
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

}
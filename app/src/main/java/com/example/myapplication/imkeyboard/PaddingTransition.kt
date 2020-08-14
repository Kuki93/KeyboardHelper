package com.example.myapplication.imkeyboard

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.transition.Transition
import androidx.transition.TransitionValues

class PaddingTransition : Transition() {

    companion object {
        private const val PROPNAME_PADDING = "imkeyboard:paddingtransition:padding"
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        transitionValues.values[PROPNAME_PADDING] = transitionValues.view.generatePaddingRect()
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        transitionValues.values[PROPNAME_PADDING] = transitionValues.view.let {
            it.getPaddingRectByTag() ?: it.generatePaddingRect()
        }
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        // This transition can only be applied to views that are on both starting and ending scenes.
        if (null == startValues || null == endValues) {
            return null
        }
        // Store a convenient reference to the target. Both the starting and ending layout have the
        // same target.
        val view: View = endValues.view
        // Store the object containing the background property for both the starting and ending
        // layouts.
        val startPaddingRect = startValues.values[PROPNAME_PADDING] as Rect?
        val endPaddingRect = endValues.values[PROPNAME_PADDING] as Rect?
        if (startPaddingRect == null || endPaddingRect == null || startPaddingRect == endPaddingRect) {
            return null
        }
        val animator = ValueAnimator.ofInt(
            startPaddingRect.top,
            endPaddingRect.top
        )
        val offsetStart = endPaddingRect.left.minus(startPaddingRect.left)
        val offsetEnd = endPaddingRect.right.minus(startPaddingRect.right)
        val offsetBottom = endPaddingRect.bottom.minus(startPaddingRect.bottom)

        fun mapValue(start: Int, offset: Int, fraction: Float): Int {
            return start.plus(offset.times(fraction).toInt())
        }
        animator.doOnStart {
            startPaddingRect.apply {
                view.setPaddingRelative(left, top, right, bottom)
            }
        }
        // Add an update listener to the Animator object.
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedFraction
            view.setPaddingRelative(
                mapValue(startPaddingRect.left, offsetStart, fraction),
                animation.animatedValue as Int,
                mapValue(startPaddingRect.right, offsetEnd, fraction),
                mapValue(startPaddingRect.bottom, offsetBottom, fraction)
            )
        }
        animator.doOnEnd {
            endPaddingRect.apply {
                view.setPaddingRelative(left, top, right, bottom)
            }
        }
        // Return the Animator object to the transitions framework. As the framework changes
        // between the starting and ending layouts, it applies the animation you've created.
        return animator
    }
}
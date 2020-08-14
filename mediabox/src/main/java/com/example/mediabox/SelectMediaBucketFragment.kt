package com.example.mediabox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediabox.MediaBoxViewModel.Companion.FRAGMENT_ARGUMENT_DATA_KEY
import com.example.mediabox.MediaBoxViewModel.Companion.FRAGMENT_ARGUMENT_SELECT_KEY
import com.example.mediabox.adapter.MediaBucketAdapter
import com.example.mediabox.adapter.OnMediaBucketChangeListener
import com.example.mediabox.data.MediaBucket
import com.example.mediabox.ext.getScreenSize
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class SelectMediaBucketFragment : BottomSheetDialogFragment() {

    private val mAdapter = MediaBucketAdapter().also {
        it.itemClickListener = { bucket ->
            fetchListener()?.onChange(bucket)
            dismiss()
        }
    }

    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return requireContext().let {
            RecyclerView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundResource(R.drawable.select_media_bucket_bottom_sheet_background)
                layoutManager = LinearLayoutManager(it)
                adapter = mAdapter
                recyclerView = this
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.also {
            it.setBackgroundColor(0x00000000)
            it.updateLayoutParams {
                height = requireContext().getScreenSize().y.times(7).div(10)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.apply {
            val selectId = getInt(FRAGMENT_ARGUMENT_SELECT_KEY, 0)
            getParcelableArrayList<MediaBucket>(FRAGMENT_ARGUMENT_DATA_KEY)?.also {
                update(it, selectId)
            }
        }
    }

    fun update(new: MutableList<MediaBucket>, selectId: Int? = null) {
        mAdapter.update(new, selectId)
        selectId ?: return
        new.indexOfFirst { mediaBucket ->
            mediaBucket.bucketId == selectId
        }.takeIf { index ->
            index > 0
        }?.also {
            (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            recyclerView.scrollToPosition(it.minus(3).coerceAtLeast(0))
        }
    }

    private fun fetchListener(): OnMediaBucketChangeListener? {
        return activity as? OnMediaBucketChangeListener
    }
}
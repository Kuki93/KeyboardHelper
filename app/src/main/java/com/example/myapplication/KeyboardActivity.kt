package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.myapplication.imkeyboard.KeyboardHelper
import com.example.myapplication.imkeyboard.SmoothPositionLinearLayoutManager
import com.example.myapplication.imkeyboard.scrollToBottom
import kotlinx.android.synthetic.main.activity_keyboard3.*
import kotlin.concurrent.thread


class KeyboardActivity : AppCompatActivity() {

    private val helper: KeyboardHelper?
        get() = root.keyboardHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard3)

        class MyVH(item: View) : RecyclerView.ViewHolder(item)

        var count = 30
        val adapter = object : RecyclerView.Adapter<MyVH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyVH {
                return MyVH(AppCompatTextView(this@KeyboardActivity).also {
                    it.layoutParams =
                        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
                })
            }

            override fun getItemCount(): Int {
                return count
            }

            override fun onBindViewHolder(holder: MyVH, position: Int) {
                (holder.itemView as TextView).also {
                    it.text = "我是${position.plus(1)}位置"
                }
            }
        }

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
//
//                recycler_view.scrollToBottom()
                recycler_view.post {
                    recycler_view.smoothScrollToPosition(count - 1)
                }
            }
        })

        val l = SmoothPositionLinearLayoutManager(this)
        toolbar.setNavigationOnClickListener {
//            helper?.handlerToggleEvent(it)
            recycler_view.scrollToBottom()
            thread {
                repeat(1) {
                    runOnUiThread {
                        count += 8
                        adapter.notifyItemRangeInserted(count, 8)
                    }
                    Thread.sleep(50)
                }
            }
        }
        recycler_view.itemAnimator = NoAlphaItemAnimator()
        recycler_view.setHasFixedSize(true)
        recycler_view.layoutManager = l
        recycler_view.adapter = adapter
        recycler_view.scrollToBottom()
    }
}

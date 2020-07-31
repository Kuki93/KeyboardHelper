package com.example.myapplication

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.example.myapplication.imkeyboard.KeyboardHelper
import com.example.myapplication.imkeyboard.SmoothPositionLinearLayoutManager
import com.example.myapplication.imkeyboard.scrollToBottom
import kotlinx.android.synthetic.main.activity_keyboard3.*

class KeyboardActivity : AppCompatActivity() {

    private val helper: KeyboardHelper?
        get() = root.keyboardHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard3)

        class MyVH(item: View) : RecyclerView.ViewHolder(item)

        val adapter = object : RecyclerView.Adapter<MyVH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyVH {
                return MyVH(AppCompatTextView(this@KeyboardActivity).also {
                    it.layoutParams =
                        ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
                })
            }

            override fun getItemCount(): Int {
                return 100
            }

            override fun onBindViewHolder(holder: MyVH, position: Int) {
                (holder.itemView as TextView).also {
                    it.text = "我是${position.plus(1)}位置"
                }
            }
        }
        toolbar.updatePadding()
        toolbar.setNavigationOnClickListener {
            helper?.handlerToggleEvent(it)
        }

//        helper?.setLambdaPreHandleToggleClickListener { view, _, switchPanelVisible ->
//            if (switchPanelVisible) {
//                TransitionManager.beginDelayedTransition(root)
//                if (view.id == btn_toggle.id) {
//                    tv1.visibility = View.VISIBLE
//                    tv2.visibility = View.GONE
//                } else {
//                    tv2.visibility = View.VISIBLE
//                    tv1.visibility = View.GONE
//                }
//            }
//            return@setLambdaPreHandleToggleClickListener false
//        }

        recycler_view.layoutManager = SmoothPositionLinearLayoutManager(this)
        recycler_view.adapter = adapter

        recycler_view.scrollToBottom()
    }
}

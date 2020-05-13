package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.imkeyboard.KeyboardHelper
import com.example.myapplication.imkeyboard.SmoothPositionLinearLayoutManager
import kotlinx.android.synthetic.main.activity_keyboard.*


class KeyboardActivity : AppCompatActivity() {

    private val helper = KeyboardHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard)
        helper.setMarginFixStrategy(1)
        helper.startSmartEscrow(appcompat_edit_text, ll, btn_toggle, recycler_view)
        helper.setOnKeyboardObserver { _, switchOpen, _ ->
            btn_toggle.text = if (switchOpen) "切换至键盘" else "切换至面板"
        }

        class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        recycler_view.layoutManager = SmoothPositionLinearLayoutManager(this).also {
//            it.reverseLayout = true
//            it.stackFromEnd = true
        }

        recycler_view.adapter = object : RecyclerView.Adapter<MyViewHolder>() {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
                return MyViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(android.R.layout.simple_list_item_1, parent, false)
                )
            }

            override fun getItemCount(): Int {
                return 200000
            }

            override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
                (holder.itemView as TextView).apply {
                    text = "我是$position"
                }
            }
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

}

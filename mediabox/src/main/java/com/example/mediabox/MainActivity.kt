package com.example.mediabox

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_select.setOnClickListener {
            Intent(this, SelectMediaActivity::class.java).apply {
                startActivity(this)
            }
        }
    }

}

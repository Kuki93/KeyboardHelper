package com.example.mediabox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_select.setOnClickListener {
            MediaBox.with()
                .setWithGif(true)
                .setWithCamera(true)
                .start(this)
        }
    }

}

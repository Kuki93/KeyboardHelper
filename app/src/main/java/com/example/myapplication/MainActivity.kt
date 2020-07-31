package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.myapplication.imkeyboard.KeyboardHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val helper = KeyboardHelper.from(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        helper.startAutomaticMode(root)
        tv1.setOnClickListener {

        }
    }

    fun haha(v: View) {
        startActivity(Intent(this, KeyboardActivity::class.java))
    }
}

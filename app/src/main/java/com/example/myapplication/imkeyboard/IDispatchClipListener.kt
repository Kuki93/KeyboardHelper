package com.example.myapplication.imkeyboard

interface IDispatchClipListener {

    var disableHandler: Boolean

    fun isDisableTouchEvent(): Boolean {
        return disableHandler.also {
            disableHandler = false
        }
    }
}

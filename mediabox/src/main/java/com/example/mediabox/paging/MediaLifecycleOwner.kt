package com.example.mediabox.paging

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

class MediaLifecycleOwner(provider: LifecycleOwner) : LifecycleOwner {

    private val mLifecycleRegistry = MediaLifecycle(provider)

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }
}
package com.example

import android.app.Application
import com.example.data.local.GestureConfigStore

class NotchApp : Application() {

    lateinit var configStore: GestureConfigStore
        private set

    companion object {
        lateinit var instance: NotchApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        configStore = GestureConfigStore(this)
    }
}

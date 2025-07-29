package com.lee.quickgallery

import android.app.Application
import com.chibatching.kotpref.Kotpref
import timber.log.Timber

class App : Application() {

    companion object {
        @Volatile
        private var INSTANCE: App? = null

        val instance: App
            get() = INSTANCE ?: throw IllegalStateException("App is not initialized")
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        // Timber 초기화 (디버그 빌드에서만 로그 출력)
        Timber.plant(Timber.DebugTree())

        Kotpref.init(this)
    }
}
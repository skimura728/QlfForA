package com.example.qlffora.util

import android.util.Log
import com.example.qlffora.BuildConfig

fun logDebug(message: String) {
    if (BuildConfig.DEBUG) {
        val tag = Throwable().stackTrace
            .first { it.className.startsWith("com.example.qlffora") }
            .let { it.className.substringAfterLast('.') }
        Log.d(tag, message)
    }
}


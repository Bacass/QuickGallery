package com.lee.quickgallery.util

import com.chibatching.kotpref.KotprefModel

object AppPrefs : KotprefModel() {
//    var userEmail: String by stringPref()
//    var userPass: String by stringPref()
//    var cardRegDate: Long by longPref()
//    var communityId: Int by intPref(1)
//    var lastRequestApiTime: Long by longPref()

    var isShowMainScreen: Boolean by booleanPref(false)

    // 버전 정보
    var appVersion: String by stringPref("1.0.0")
    var lastSettingsUpdate: String by stringPref("")
}

package com.lee.quickgallery.util

import com.chibatching.kotpref.KotprefModel

object AppPrefs : KotprefModel() {
//    var userEmail: String by stringPref()
//    var userPass: String by stringPref()
//    var cardRegDate: Long by longPref()
//    var communityId: Int by intPref(1)
//    var lastRequestApiTime: Long by longPref()

    var isShowMainScreen: Boolean by booleanPref(false)
    
    // 표시 설정
    var showMediaCount: Boolean by booleanPref(true)
    var mediaSortType: String by stringPref(SortType.TIME_DESC.name)
    var autoRefresh: Boolean by booleanPref(false)
    
    // 폴더 순서 저장
    var folderOrder: String by stringPref("")

    // 버전 정보
    var appVersion: String by stringPref("1.0.0")
    var lastSettingsUpdate: String by stringPref("")
}

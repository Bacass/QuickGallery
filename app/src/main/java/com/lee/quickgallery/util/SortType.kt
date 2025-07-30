package com.lee.quickgallery.util

enum class SortType(val displayName: String) {
    TIME_DESC("시간순 (내림차순)"),
    TIME_ASC("시간순 (오름차순)"),
    NAME_DESC("이름순 (내림차순)"),
    NAME_ASC("이름순 (오름차순)");
    
    companion object {
        fun fromString(value: String): SortType {
            return values().find { it.name == value } ?: TIME_DESC
        }
    }
} 
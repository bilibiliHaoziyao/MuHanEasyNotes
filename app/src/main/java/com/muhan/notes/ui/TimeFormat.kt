package com.muhan.notes.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

/** 把毫秒时间戳格式化为本地时区的 "yyyy-MM-dd HH:mm" */
fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DATE_FORMATTER)

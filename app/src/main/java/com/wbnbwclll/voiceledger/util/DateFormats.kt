package com.wbnbwclll.voiceledger.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateFormats {

    private val DATE_TIME = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    private val DATE = DateTimeFormatter.ofPattern("yyyy年M月d日")

    fun dateTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(DATE_TIME)

    fun date(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(DATE)
}

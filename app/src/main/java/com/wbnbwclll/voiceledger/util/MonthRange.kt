package com.wbnbwclll.voiceledger.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 某年某月，用于列表页筛选 */
data class MonthPeriod(val year: Int, val month: Int) {

    fun startMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        MonthRange.startMillis(year, month, zone)

    /** 区间右边界（不含）：下个月 1 日 0 点 */
    fun endMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        next().startMillis(zone)

    fun previous(): MonthPeriod = if (month == 1) MonthPeriod(year - 1, 12) else MonthPeriod(year, month - 1)

    fun next(): MonthPeriod = if (month == 12) MonthPeriod(year + 1, 1) else MonthPeriod(year, month + 1)

    companion object {
        fun of(millis: Long, zone: ZoneId = ZoneId.systemDefault()): MonthPeriod =
            MonthRange.monthOf(millis, zone)
    }
}

object MonthRange {

    fun startMillis(year: Int, month: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.of(year, month, 1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun monthOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()): MonthPeriod {
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return MonthPeriod(date.year, date.monthValue)
    }
}

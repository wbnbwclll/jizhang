package com.wbnbwclll.voiceledger.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeechTextTest {

    @Test
    fun 从识别文本提取整数金额() {
        assertEquals(3200L, SpeechText.extractAmountCents("打车32"))
        assertEquals(1200L, SpeechText.extractAmountCents("早饭12块"))
    }

    @Test
    fun 从识别文本提取小数金额() {
        assertEquals(2850L, SpeechText.extractAmountCents("午饭花了28.5"))
    }

    @Test
    fun 无数字返回null() {
        assertNull(SpeechText.extractAmountCents("今天天气不错"))
    }

    @Test
    fun 中文数字当前不解析() {
        assertNull(SpeechText.extractAmountCents("二十八块五"))
    }
}

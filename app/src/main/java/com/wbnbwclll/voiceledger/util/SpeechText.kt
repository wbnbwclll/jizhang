package com.wbnbwclll.voiceledger.util

/**
 * 从语音识别文本中提取金额。
 * 目前只处理阿拉伯数字（如 "打车32"、"花了28.5"），
 * 中文数字（"二十八块五"）留给后续 AI 解析处理。
 */
object SpeechText {

    private val NUMBER = Regex("""\d+(?:\.\d{1,2})?""")

    fun extractAmountCents(text: String): Long? =
        NUMBER.find(text)?.value?.let { Money.parseYuanToCents(it) }
}

package com.wbnbwclll.voiceledger.model

/**
 * 记录来源。
 * 本期只有 MANUAL，VOICE / TEXT 预留给后续语音与文字记账。
 */
enum class EntrySource {
    MANUAL,
    VOICE,
    TEXT
}

/**
 * 解析方式。
 * 本期只有 MANUAL，后续由 AI 解析或本地规则解析产生的记录使用其余取值。
 */
enum class ParseSource {
    MANUAL,
    AI,
    LOCAL_RULE
}

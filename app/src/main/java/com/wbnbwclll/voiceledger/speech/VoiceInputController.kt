package com.wbnbwclll.voiceledger.speech

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.wbnbwclll.voiceledger.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.IOException

data class VoiceUiState(
    val isModelReady: Boolean = false,
    val isLoadingModel: Boolean = false,
    val isRecording: Boolean = false,
    val partialText: String = "",
    val message: String? = null
)

/**
 * 端侧离线语音输入：Vosk 小模型 + AudioRecord。
 * 音频只在内存中流转，不落盘、不上传。
 */
class VoiceInputController(private val context: Context) {

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null
    private var released = false

    @Volatile
    private var recording = false
    private var onFinalText: ((String) -> Unit)? = null

    /** 开始录音；模型未加载时先异步加载 */
    fun start(onFinal: (String) -> Unit) {
        if (released || recording || _state.value.isLoadingModel) return
        onFinalText = onFinal

        val loaded = model
        if (loaded != null) {
            beginRecording(loaded)
            return
        }

        _state.update { it.copy(isLoadingModel = true, message = null) }
        StorageService.unpack(
            context,
            ASSET_DIR,
            MODEL_DIR,
            { unpacked ->
                if (released) {
                    unpacked.close()
                    return@unpack
                }
                model = unpacked
                _state.update { it.copy(isLoadingModel = false, isModelReady = true) }
                beginRecording(unpacked)
            },
            { error: IOException ->
                if (released) return@unpack
                _state.update {
                    it.copy(
                        isLoadingModel = false,
                        message = context.getString(R.string.voice_error, error.message ?: "未知错误")
                    )
                }
            }
        )
    }

    private fun beginRecording(target: Model) {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            _state.update { it.copy(message = context.getString(R.string.voice_mic_error)) }
            return
        }

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer * 2, BUFFER_SAMPLES * 2)
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            _state.update { it.copy(message = context.getString(R.string.voice_mic_error)) }
            return
        }

        val rec = Recognizer(target, SAMPLE_RATE.toFloat())
        recognizer = rec
        audioRecord = record
        record.startRecording()
        recording = true
        _state.update { it.copy(isRecording = true, partialText = "", message = null) }

        worker = Thread {
            val buffer = ShortArray(BUFFER_SAMPLES)
            while (recording) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    rec.acceptWaveForm(buffer, read)
                    val partial = jsonField(rec.partialResult, "partial")
                    if (partial.isNotEmpty()) {
                        _state.update { it.copy(partialText = partial) }
                    }
                }
            }
        }.also { it.start() }
    }

    /** 停止录音并返回最终文本；识别失败时给提示 */
    fun stop() {
        if (!recording) return
        recording = false
        runCatching { worker?.join(1500) }
        worker = null
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null

        val text = runCatching { jsonField(recognizer?.finalResult, "text") }.getOrDefault("")
        runCatching { recognizer?.close() }
        recognizer = null

        _state.update {
            it.copy(
                isRecording = false,
                partialText = "",
                message = if (text.isBlank()) context.getString(R.string.voice_empty) else null
            )
        }
        val callback = onFinalText
        onFinalText = null
        if (text.isNotBlank()) callback?.invoke(text)
    }

    fun release() {
        released = true
        onFinalText = null
        stop()
        runCatching { model?.close() }
        model = null
        _state.update { it.copy(isModelReady = false) }
    }

    private fun jsonField(json: String?, key: String): String =
        runCatching { JSONObject(json ?: "{}").optString(key).trim() }.getOrDefault("")

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SAMPLES = 2048
        private const val ASSET_DIR = "vosk-model-cn"
        private const val MODEL_DIR = "vosk-model-cn"
    }
}

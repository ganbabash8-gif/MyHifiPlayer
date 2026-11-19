package com.example.myhifiplayer

import android.content.Intent
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    init { System.loadLibrary("myhifiplayer") }

    external fun startAudioEngine()
    external fun setDspMode(mode: Int)
    external fun writePcm(pcmData: FloatArray)

    private val scope = CoroutineScope(Dispatchers.Default)
    private var isPlaying = false

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Toast.makeText(this, "Файл выбран. Запускаю...", Toast.LENGTH_SHORT).show()
            startAudioEngine()
            isPlaying = true
            scope.launch { decodeAndPlay(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TestMaster.runAndCopy(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#081008"))
            setPadding(32, 64, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Yautja × Neural Player\n[INSIDE THE JACK]"
            setTextColor(Color.parseColor("#7dff7d"))
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        mainLayout.addView(title)

        val loadBtn = Button(this).apply {
            text = "🎵 Выбрать трек и врубить"
            setTextColor(Color.parseColor("#050805"))
            setBackgroundColor(Color.parseColor("#7dff7d"))
            setOnClickListener { getContent.launch("audio/*") }
        }
        mainLayout.addView(loadBtn)

        val dspModes = listOf("0. Direct", "1. Philips Smooth (Holy Grail)", "2. Marantz Warm")
        for ((index, mode) in dspModes.withIndex()) {
            val btn = Button(this).apply {
                text = mode
                setTextColor(Color.parseColor("#c0e0c0"))
                setBackgroundColor(Color.parseColor("#0e1a0e"))
                setOnClickListener { setDspMode(index) }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 16, 0, 0) }
            mainLayout.addView(btn, params)
        }

        setContentView(mainLayout)
    }

    private fun decodeAndPlay(uri: Uri) {
        val extractor = MediaExtractor()
        extractor.setDataSource(this, uri, null)
        
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val fmt = extractor.getTrackFormat(i)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = fmt
                break
            }
        }
        if (trackIndex < 0) return
        
        extractor.selectTrack(trackIndex)
        val mime = format!!.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var isEOS = false

        while (isPlaying && !isEOS) {
            val inIndex = codec.dequeueInputBuffer(10000)
            if (inIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inIndex)!!
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    isEOS = true
                } else {
                    codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }

            val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outIndex)!!
                val pcmShort = ShortArray(bufferInfo.size / 2)
                outputBuffer.asShortBuffer().get(pcmShort)
                
                // Конвертируем в Float (-1.0 до 1.0)
                val pcmFloat = FloatArray(pcmShort.size) { pcmShort[it] / 32768.0f }
                writePcm(pcmFloat)
                
                codec.releaseOutputBuffer(outIndex, false)
            }
        }
        codec.stop()
        codec.release()
        extractor.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPlaying = false
        scope.cancel()
    }
}

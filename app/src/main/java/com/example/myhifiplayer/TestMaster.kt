package com.example.myhifiplayer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File
import kotlin.concurrent.thread

object TestMaster {
    private const val TAG = "TestMaster"
    private const val ENC_PCM_24_PACKED = 21
    private const val ENC_PCM_32 = 22

    fun runAndCopy(context: Context) {
        val app = context.applicationContext
        thread(name = "TestMaster") {
            val report = try {
                buildReport(app)
            } catch (t: Throwable) {
                "TestMaster crashed: $t"
            }
            report.lines().forEach { Log.i(TAG, it) }
            Handler(Looper.getMainLooper()).post {
                try {
                    val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("TestMaster", report))
                    Toast.makeText(app, "Тест-мастер: отчёт скопирован", Toast.LENGTH_LONG).show()
                } catch (t: Throwable) {
                    Log.e(TAG, "clipboard failed", t)
                }
            }
        }
    }

    private fun buildReport(ctx: Context): String {
        val sb = StringBuilder()
        fun h(t: String) { sb.append("\n== ").append(t).append(" ==\n") }
        fun l(t: String) { sb.append(t).append('\n') }

        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        h("DEVICE")
        l("model=${Build.MODEL} board=${Build.BOARD} hardware=${Build.HARDWARE}")
        l("android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT} abis=${Build.SUPPORTED_ABIS.joinToString()}")
        val cores = Runtime.getRuntime().availableProcessors()
        l("cores=$cores")
        for (i in 0 until cores) {
            val base = "/sys/devices/system/cpu/cpu$i/cpufreq/"
            val f = readFirst(base + "cpuinfo_max_freq") ?: readFirst(base + "scaling_max_freq")
            l("cpu$i max_kHz=${f ?: "n/a"}")
        }

        h("AUDIO MANAGER")
        l("OUTPUT_SAMPLE_RATE=${am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)}")
        l("OUTPUT_FRAMES_PER_BUFFER=${am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)}")
        l("native_rate_music=${AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)}")
        val pm = ctx.packageManager
        l("feature low_latency=${pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)} pro=${pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)}")

        h("OUTPUT DEVICES")
        val outs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (d in outs) {
            l("- id=${d.id} type=${typeName(d.type)} name=${d.productName}")
            l("  rates=${d.sampleRates.toList()} channels=${d.channelCounts.toList()}")
            l("  encodings(raw ints)=${d.encodings.toList()}")
            l("  mixerAttrs(API34)=${mixerAttrs(am, d)}")
        }

        h("FORMAT PROBE")
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val encs = listOf(
            "PCM16" to AudioFormat.ENCODING_PCM_16BIT,
            "PCM24" to ENC_PCM_24_PACKED,
            "PCM32" to ENC_PCM_32,
            "FLOAT" to AudioFormat.ENCODING_PCM_FLOAT
        )
        val rates = listOf(44100, 48000, 88200, 96000, 176400, 192000)
        for ((name, enc) in encs) {
            val line = StringBuilder("$name: ")
            for (sr in rates) {
                val fmt = AudioFormat.Builder()
                    .setEncoding(enc)
                    .setSampleRate(sr)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
                val direct = try { AudioTrack.isDirectPlaybackSupported(fmt, attrs) } catch (t: Throwable) { false }
                line.append("$sr[direct=${if (direct) "Y" else "n"},track=${tryCreate(sr, enc, attrs)}] ")
            }
            l(line.toString())
        }

        h("ROUTE PROBE")
        try {
            val t = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(48000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(48000 * 2 * 4 / 5)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            t.play()
            t.write(FloatArray(9600), 0, 9600, AudioTrack.WRITE_BLOCKING)
            Thread.sleep(120)
            val rd = t.routedDevice
            l("routed=${rd?.let { typeName(it.type) + " / " + it.productName } ?: "null"}")
            t.stop()
            t.release()
        } catch (e: Throwable) {
            l("route probe failed: $e")
        }

        h("AUDIO POLICY FILES")
        val paths = listOf(
            "/vendor/etc/audio_policy_configuration.xml",
            "/vendor/etc/audio/audio_policy_configuration.xml",
            "/odm/etc/audio_policy_configuration.xml",
            "/system/etc/audio_policy_configuration.xml"
        )
        for (p in paths) {
            val f = File(p)
            if (!f.exists()) { l("$p: нет файла"); continue }
            try {
                val lines = f.readLines().filter {
                    it.contains("samplingRates") || it.contains("<mixPort") ||
                        it.contains("Headphone") || it.contains("Headset") || it.contains("Speaker")
                }.take(80)
                l("$p: ${lines.size} строк")
                lines.forEach { l("  " + it.trim()) }
            } catch (t: Throwable) {
                l("$p: недоступно (${t.javaClass.simpleName})")
            }
        }

        h("GETPROP")
        try {
            val proc = Runtime.getRuntime().exec("getprop")
            proc.inputStream.bufferedReader().useLines { seq ->
                seq.filter {
                    val s = it.lowercase()
                    s.contains("audio") || s.contains("dsd") || s.contains("hifi") || s.contains("hires")
                }.take(80).forEach { l(it) }
            }
        } catch (t: Throwable) {
            l("getprop failed: $t")
        }

        h("END")
        return sb.toString()
    }

    private fun tryCreate(sr: Int, enc: Int, attrs: AudioAttributes): String {
        return try {
            val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_STEREO, enc)
            if (minBuf <= 0) return "no($minBuf)"
            val t = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(enc)
                        .setSampleRate(sr)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            val ok = t.state == AudioTrack.STATE_INITIALIZED
            t.release()
            if (ok) "ok" else "init-fail"
        } catch (e: Throwable) {
            "err:${e.javaClass.simpleName}"
        }
    }

    private fun mixerAttrs(am: AudioManager, d: AudioDeviceInfo): String = try {
        val m = AudioManager::class.java.getMethod("getSupportedMixerAttributes", AudioDeviceInfo::class.java)
        val list = m.invoke(am, d) as? List<*>
        if (list.isNullOrEmpty()) "[]" else list.joinToString("; ")
    } catch (t: Throwable) {
        "n/a(${t.javaClass.simpleName})"
    }

    private fun readFirst(path: String): String? = try {
        File(path).readText().trim().ifEmpty { null }
    } catch (t: Throwable) {
        null
    }

    private fun typeName(t: Int): String = when (t) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "BUILTIN_EARPIECE"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "BUILTIN_SPEAKER"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET(jack)"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "WIRED_HEADPHONES(jack)"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "LINE_ANALOG"
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> "LINE_DIGITAL"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BT_SCO"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BT_A2DP"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB_DEVICE"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB_ACCESSORY"
        22 -> "USB_HEADSET"
        else -> "TYPE_$t"
    }
}

package com.lagradost.cloudstream3.tv.presentation.screens.player

import android.content.Context
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Silent 8 kHz mono PCM WAV: plays offline and is seekable, so the tests need no network. */
internal fun createSilentWav(context: Context, seconds: Int): File {
    val sampleRate = 8_000
    val dataSize = sampleRate * seconds
    val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVE".toByteArray())
        put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
        putInt(sampleRate); putInt(sampleRate); putShort(1); putShort(8)
        put("data".toByteArray()); putInt(dataSize)
    }.array()
    return File(context.cacheDir, "silence_$seconds.wav").apply {
        outputStream().use { out ->
            out.write(header)
            out.write(ByteArray(dataSize) { 0x80.toByte() })
        }
    }
}

internal fun createVttFile(context: Context): File =
    File(context.cacheDir, "test_subtitle.vtt").apply {
        writeText("WEBVTT\n\n00:00:01.000 --> 00:00:02.000\nHello\n")
    }

internal fun testSubtitle(file: File) = SubtitleData(
    originalName = "Polish",
    nameSuffix = "",
    url = file.toURI().toString(),
    origin = SubtitleOrigin.URL,
    mimeType = "text/vtt",
    headers = emptyMap(),
    languageCode = "pl",
)

@Suppress("DEPRECATION")
internal fun testFileLink(file: File) = ExtractorLink(
    source = "TestSource",
    name = "TestName",
    url = file.toURI().toString(),
    referer = "",
    quality = 720,
    type = ExtractorLinkType.VIDEO,
)

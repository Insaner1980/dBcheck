package com.dbcheck.app.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import io.mockk.every
import io.mockk.mockk

internal fun stubAudioTrackBuilders(track: AudioTrack) {
    every { anyConstructed<AudioAttributes.Builder>().setUsage(any()) } answers { self as AudioAttributes.Builder }
    every { anyConstructed<AudioAttributes.Builder>().setContentType(any()) } answers {
        self as AudioAttributes.Builder
    }
    every { anyConstructed<AudioAttributes.Builder>().build() } returns mockk()
    every { anyConstructed<AudioFormat.Builder>().setSampleRate(any()) } answers { self as AudioFormat.Builder }
    every { anyConstructed<AudioFormat.Builder>().setEncoding(any()) } answers { self as AudioFormat.Builder }
    every { anyConstructed<AudioFormat.Builder>().setChannelMask(any()) } answers { self as AudioFormat.Builder }
    every { anyConstructed<AudioFormat.Builder>().build() } returns mockk()
    every { anyConstructed<AudioTrack.Builder>().setAudioAttributes(any()) } answers { self as AudioTrack.Builder }
    every { anyConstructed<AudioTrack.Builder>().setAudioFormat(any()) } answers { self as AudioTrack.Builder }
    every { anyConstructed<AudioTrack.Builder>().setBufferSizeInBytes(any()) } answers { self as AudioTrack.Builder }
    every { anyConstructed<AudioTrack.Builder>().setTransferMode(any()) } answers { self as AudioTrack.Builder }
    every { anyConstructed<AudioTrack.Builder>().build() } returns track
}

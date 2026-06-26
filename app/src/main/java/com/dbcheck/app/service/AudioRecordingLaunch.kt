package com.dbcheck.app.service

import com.dbcheck.app.domain.audio.AudioEngine
import com.dbcheck.app.domain.audio.AudioRecordingFailure
import com.dbcheck.app.domain.audio.AudioRecordingResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class AudioRecordingLaunch(val job: Job, val started: CompletableDeferred<Boolean>)

internal fun CoroutineScope.launchAudioRecording(
    audioEngine: AudioEngine,
    onRecordingStarted: suspend () -> Unit,
    onRecordingFinished: (AudioRecordingResult) -> Unit,
): AudioRecordingLaunch {
    val startResult = CompletableDeferred<Boolean>()
    val job =
        launch {
            val result =
                runCatching {
                    audioEngine.startRecording {
                        onRecordingStarted()
                        startResult.complete(true)
                    }
                }.getOrElse { error ->
                    if (!startResult.isCompleted) {
                        startResult.complete(false)
                    }
                    if (error is CancellationException) throw error
                    AudioRecordingResult.Failed(AudioRecordingFailure.StartFailed)
                }
            if (!startResult.isCompleted) {
                startResult.complete(false)
            }
            onRecordingFinished(result)
        }
    return AudioRecordingLaunch(job = job, started = startResult)
}

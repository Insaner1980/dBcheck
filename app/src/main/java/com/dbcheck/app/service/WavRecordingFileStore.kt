package com.dbcheck.app.service

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.dbcheck.app.R
import com.dbcheck.app.data.export.ExportFileCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class WavRecordingFileStore
    @Inject
    constructor(@param:ApplicationContext private val context: Context) {
        fun createRecordingFile(sessionId: Long, startedAtMs: Long): File {
            val directory = recordingDirectory()
            return File(directory, "dBcheck_wav_session_${sessionId}_$startedAtMs.wav")
        }

        fun hasRecordingForSession(sessionId: Long): Boolean = recordingFileForSession(sessionId) != null

        fun recordingFileForSession(sessionId: Long): File? {
            val prefix = recordingFilePrefix(sessionId)
            return recordingDirectory()
                .listFiles()
                .orEmpty()
                .filter { file -> file.isFile && file.name.startsWith(prefix) && file.extension == WAV_EXTENSION }
                .maxWithOrNull(compareBy<File> { file -> file.recordingStartedAt(prefix) ?: Long.MIN_VALUE })
        }

        fun createShareIntent(sessionId: Long): Intent? {
            val recordingFile = recordingFileForSession(sessionId) ?: return null
            val uri =
                FileProvider.getUriForFile(
                    context,
                    ExportFileCache.fileProviderAuthority(context),
                    recordingFile,
                )
            return Intent(Intent.ACTION_SEND).apply {
                type = WAV_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData =
                    ClipData.newUri(
                        context.contentResolver,
                        context.getString(R.string.report_wav_share_clip_label),
                        uri,
                    )
            }
        }

        fun deleteRecordingForSession(sessionId: Long): Boolean {
            val recordingFile = recordingFileForSession(sessionId) ?: return false
            return recordingFile.delete().also { deleted ->
                if (!deleted && recordingFile.exists()) {
                    recordingFile.deleteOnExit()
                }
            }
        }

        private fun recordingDirectory(): File =
            File(context.filesDir, WAV_RECORDINGS_DIRECTORY_NAME).apply { mkdirs() }

        private fun recordingFilePrefix(sessionId: Long): String = "dBcheck_wav_session_${sessionId}_"

        private fun File.recordingStartedAt(prefix: String): Long? = name
                .removePrefix(prefix)
                .removeSuffix(".$WAV_EXTENSION")
                .toLongOrNull()

        companion object {
            const val WAV_RECORDINGS_DIRECTORY_NAME = "wav_recordings"
            private const val WAV_EXTENSION = "wav"
            private const val WAV_MIME_TYPE = "audio/wav"
        }
    }

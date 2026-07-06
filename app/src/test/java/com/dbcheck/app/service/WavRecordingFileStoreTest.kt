package com.dbcheck.app.service

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.dbcheck.app.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WavRecordingFileStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @After
    fun tearDown() {
        unmockkStatic(FileProvider::class)
    }

    @Test
    fun createRecordingFileUsesPrivateWavDirectoryAndSessionName() {
        val filesDir = temporaryFolder.newFolder("files")
        val context = wavContext(filesDir)
        val store = WavRecordingFileStore(context)

        val file = store.createRecordingFile(sessionId = 42L, startedAtMs = 1_700_000_000_000L)

        assertEquals(
            File(filesDir, "wav_recordings/dBcheck_wav_session_42_1700000000000.wav").canonicalFile,
            file.canonicalFile,
        )
        assertTrue(file.parentFile?.isDirectory == true)
    }

    @Test
    fun recordingFileForSessionReturnsLatestMatchingWav() {
        val filesDir = temporaryFolder.newFolder("files")
        val store = WavRecordingFileStore(wavContext(filesDir))
        store.createRecordingFile(sessionId = 41L, startedAtMs = 1_700_000_000_000L).writeText("other")
        val older = store.createRecordingFile(sessionId = 42L, startedAtMs = 1_700_000_000_000L).apply {
            writeText("older")
        }
        val newer = store.createRecordingFile(sessionId = 42L, startedAtMs = 1_700_000_060_000L).apply {
            writeText("newer")
        }
        older.setLastModified(1_700_000_000_000L)
        newer.setLastModified(1_700_000_060_000L)

        assertEquals(newer.canonicalFile, store.recordingFileForSession(42L)?.canonicalFile)
    }

    @Test
    fun createShareIntentUsesFileProviderContentUriAndReadGrant() {
        val filesDir = temporaryFolder.newFolder("files")
        val context = wavContext(filesDir)
        val store = WavRecordingFileStore(context)
        val wavFile = store.createRecordingFile(sessionId = 42L, startedAtMs = 1_700_000_000_000L).apply {
            writeText("wav")
        }
        val shareUri = Uri.parse("content://com.dbcheck.app.fileprovider/wav_recordings/session.wav")
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(context, "com.dbcheck.app.fileprovider", wavFile) } returns shareUri

        val intent = store.createShareIntent(sessionId = 42L)

        assertEquals(Intent.ACTION_SEND, intent?.action)
        assertEquals("audio/wav", intent?.type)
        assertEquals(shareUri, intent?.getParcelableExtra(Intent.EXTRA_STREAM))
        assertTrue((intent!!.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertEquals(shareUri, intent.clipData?.getItemAt(0)?.uri)
    }

    @Test
    fun deleteRecordingForSessionDeletesOnlyMatchingWav() {
        val filesDir = temporaryFolder.newFolder("files")
        val store = WavRecordingFileStore(wavContext(filesDir))
        val target = store.createRecordingFile(sessionId = 42L, startedAtMs = 1_700_000_000_000L).apply {
            writeText("wav")
        }
        val other = store.createRecordingFile(sessionId = 43L, startedAtMs = 1_700_000_000_000L).apply {
            writeText("other")
        }

        assertTrue(store.deleteRecordingForSession(42L))

        assertFalse(target.exists())
        assertTrue(other.exists())
        assertNull(store.recordingFileForSession(42L))
    }

    private fun wavContext(filesDir: File): Context {
        val contentResolver = mockk<ContentResolver>(relaxed = true)
        return mockk {
            every { this@mockk.filesDir } returns filesDir
            every { packageName } returns "com.dbcheck.app"
            every { this@mockk.contentResolver } returns contentResolver
            every { getString(R.string.report_wav_share_clip_label) } returns "dBcheck WAV recording"
        }
    }
}

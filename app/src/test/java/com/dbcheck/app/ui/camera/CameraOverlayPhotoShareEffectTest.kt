package com.dbcheck.app.ui.camera

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class CameraOverlayPhotoShareEffectTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sharingUsesCurrentContextAndFlowAndStopsWhenDisposed() {
        val firstContext = mockk<Context>(relaxed = true)
        val secondContext = mockk<Context>(relaxed = true)
        val context = mutableStateOf(firstContext)
        val firstFlow = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
        val secondFlow = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
        var firstFlowCollections = 0
        val flow = mutableStateOf<Flow<Intent>>(firstFlow.onStart { firstFlowCollections++ })
        val title = mutableStateOf("Share")
        val visible = mutableStateOf(true)
        var originalErrors = 0
        var currentErrors = 0
        val onError = mutableStateOf<() -> Unit>({ originalErrors++ })
        val intent = Intent(Intent.ACTION_SEND)

        composeTestRule.setContent {
            if (visible.value) {
                CameraOverlayPhotoShareEffect(
                    context = context.value,
                    shareChooserTitle = title.value,
                    photoShareIntents = flow.value,
                    onPhotoCaptureError = onError.value,
                )
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(1, firstFlow.subscriptionCount.value)
            assertTrue(firstFlow.tryEmit(intent))
        }
        composeTestRule.waitForIdle()
        verify(exactly = 1) { firstContext.startActivity(any()) }

        composeTestRule.runOnIdle { context.value = secondContext }
        composeTestRule.waitForIdle()
        assertEquals(1, firstFlowCollections)
        composeTestRule.runOnIdle { title.value = "Share photo" }
        composeTestRule.waitForIdle()
        assertEquals(1, firstFlowCollections)
        composeTestRule.runOnIdle {
            assertEquals(1, firstFlow.subscriptionCount.value)
            assertTrue(firstFlow.tryEmit(intent))
        }
        composeTestRule.waitForIdle()
        verify(exactly = 1) { firstContext.startActivity(any()) }
        verify(exactly = 1) {
            secondContext.startActivity(match { it.getStringExtra(Intent.EXTRA_TITLE) == "Share photo" })
        }

        composeTestRule.runOnIdle { flow.value = secondFlow }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            assertEquals(0, firstFlow.subscriptionCount.value)
            assertEquals(1, secondFlow.subscriptionCount.value)
            assertTrue(firstFlow.tryEmit(intent))
            assertTrue(secondFlow.tryEmit(intent))
        }
        composeTestRule.waitForIdle()
        verify(exactly = 2) { secondContext.startActivity(any()) }

        every { secondContext.startActivity(any()) } throws ActivityNotFoundException()
        composeTestRule.runOnIdle { onError.value = { currentErrors++ } }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { assertTrue(secondFlow.tryEmit(intent)) }
        composeTestRule.waitForIdle()
        assertEquals(0, originalErrors)
        assertEquals(1, currentErrors)

        composeTestRule.runOnIdle { visible.value = false }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            assertEquals(0, secondFlow.subscriptionCount.value)
            assertTrue(secondFlow.tryEmit(intent))
        }
        composeTestRule.waitForIdle()
        verify(exactly = 1) { firstContext.startActivity(any()) }
        verify(exactly = 3) { secondContext.startActivity(any()) }
    }
}

package com.dbcheck.app.ui.settings.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PassiveMonitoringStartConfirmationControllerTest {
    @Test
    fun doubleConfirmInvokesStartExactlyOnce() {
        val controller = PassiveMonitoringStartConfirmationController()
        var startCalls = 0

        controller.request()
        assertTrue(controller.isOpen)
        controller.confirm { startCalls += 1 }
        controller.confirm { startCalls += 1 }

        assertFalse(controller.isOpen)
        assertEquals(1, startCalls)
    }

    @Test
    fun cancelClosesWithoutInvokingStart() {
        val controller = PassiveMonitoringStartConfirmationController()
        var startCalls = 0

        controller.request()
        controller.cancel()
        controller.confirm { startCalls += 1 }

        assertFalse(controller.isOpen)
        assertEquals(0, startCalls)
    }

    @Test
    fun dismissClosesWithoutInvokingStart() {
        val controller = PassiveMonitoringStartConfirmationController()
        var startCalls = 0

        controller.request()
        controller.dismiss()
        controller.confirm { startCalls += 1 }

        assertFalse(controller.isOpen)
        assertEquals(0, startCalls)
    }

    @Test
    fun reopenAllowsExactlyOneNewConfirmedStart() {
        val controller = PassiveMonitoringStartConfirmationController()
        var startCalls = 0

        controller.request()
        controller.confirm { startCalls += 1 }
        controller.request()
        controller.confirm { startCalls += 1 }

        assertFalse(controller.isOpen)
        assertEquals(2, startCalls)
    }
}

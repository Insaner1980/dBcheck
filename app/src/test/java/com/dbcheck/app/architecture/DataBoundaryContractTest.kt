package com.dbcheck.app.architecture

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataBoundaryContractTest {
    @Test
    fun sessionDetailConsumesReportMeasurementsInsteadOfRoomEntities() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/history/detail/SessionDetailViewModel.kt").readText()

        assertTrue(source.contains("getReportMeasurementsForSession"))
        assertFalse(source.contains("MeasurementEntity"))
    }

    @Test
    fun widgetReadsSessionsThroughRepositoryInsteadOfDaoEntities() {
        val source = projectFile("src/main/java/com/dbcheck/app/widget/DbCheckWidget.kt").readText()

        assertTrue(source.contains("SessionRepository"))
        assertFalse(source.contains("SessionDao"))
        assertFalse(source.contains("SessionEntity"))
    }

    @Test
    fun audioSessionManagerDoesNotConstructRoomEntitiesDirectly() {
        val source = projectFile("src/main/java/com/dbcheck/app/service/AudioSessionManager.kt").readText()

        assertFalse(source.contains("data.local.db.entity"))
        assertFalse(source.contains("SessionEntity("))
        assertFalse(source.contains("MeasurementEntity("))
    }
}

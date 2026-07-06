package com.dbcheck.app.architecture

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    @Test
    fun domainLayerDoesNotImportPlatformOrOuterLayers() {
        val domainSources = domainSourceFiles()
        val forbiddenImports =
            Regex(
                "^import\\s+(android|androidx|com\\.dbcheck\\.app\\.(data|service|sync|ui|billing|widget))\\.",
                RegexOption.MULTILINE,
            )

        val violations =
            domainSources
                .mapNotNull { file ->
                    forbiddenImports.findAll(file.readText())
                        .map { match -> "${file.relativeTo(appSourceRoot()).path}: ${match.value}" }
                        .toList()
                        .takeIf { it.isNotEmpty() }
                }
                .flatten()

        assertTrue("Forbidden domain imports:\n${violations.joinToString("\n")}", violations.isEmpty())
    }

    private fun domainSourceFiles(): List<File> = projectPath("src/main/java/com/dbcheck/app/domain")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()

    private fun appSourceRoot(): File = projectPath("src/main/java")

    private fun projectPath(path: String): File = listOf(
            File(path),
            File("app", path),
        ).firstOrNull(File::exists)
        ?: error("Project path does not exist: $path")
}

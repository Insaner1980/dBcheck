package com.dbcheck.app.build

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Properties

class GradleWrapperSecurityTest {
    @Test
    fun wrapperDistributionIsPinnedWithSha256Checksum() {
        val properties =
            Properties().apply {
                projectFile("../gradle/wrapper/gradle-wrapper.properties")
                    .inputStream()
                    .use(::load)
            }

        assertEquals(
            "https://services.gradle.org/distributions/gradle-9.6.1-bin.zip",
            properties.getProperty("distributionUrl"),
        )
        assertEquals(
            "distributionSha256Sum must pin the Gradle distribution used by release CI.",
            "9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14",
            properties.getProperty("distributionSha256Sum"),
        )
    }
}

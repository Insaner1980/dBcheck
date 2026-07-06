package com.dbcheck.app.build

import com.dbcheck.app.projectFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            "https://services.gradle.org/distributions/gradle-9.4.1-bin.zip",
            properties.getProperty("distributionUrl"),
        )
        assertTrue(
            "distributionSha256Sum must pin the Gradle distribution used by release CI.",
            properties.getProperty("distributionSha256Sum")
                ?.matches(Regex("[a-f0-9]{64}")) == true,
        )
    }
}

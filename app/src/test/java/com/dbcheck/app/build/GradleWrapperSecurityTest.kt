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
            "https://services.gradle.org/distributions/gradle-9.8.1-bin.zip",
            properties.getProperty("distributionUrl"),
        )
        assertEquals(
            "distributionSha256Sum must pin the Gradle distribution used by release CI.",
            "dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f",
            properties.getProperty("distributionSha256Sum"),
        )
    }
}

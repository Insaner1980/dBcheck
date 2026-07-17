package com.dbcheck.app.release

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class FontLicenseNoticeTest {
    @Test
    fun bundledFontsHavePackagedCopyrightAndLicenseNotice() {
        val notice = projectFile("src/main/assets/licenses/fonts/FONT_LICENSES.txt").readText()

        assertTrue(notice.contains("Copyright 2018 The Manrope Project Authors"))
        assertTrue(notice.contains("Copyright 2019 The Manrope Project Authors"))
        assertTrue(notice.contains("Copyright 2020 The Space Grotesk Project Authors"))
        assertTrue(notice.contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertTrue(notice.contains("manrope_regular.ttf"))
        assertTrue(notice.contains("manrope_medium.ttf"))
        assertTrue(notice.contains("manrope_semibold.ttf"))
        assertTrue(notice.contains("manrope_bold.ttf"))
        assertTrue(notice.contains("space_grotesk_regular.ttf"))
        assertTrue(notice.contains("space_grotesk_medium.ttf"))
        assertTrue(notice.contains("space_grotesk_semibold.ttf"))
        assertTrue(notice.contains("space_grotesk_bold.ttf"))
    }
}

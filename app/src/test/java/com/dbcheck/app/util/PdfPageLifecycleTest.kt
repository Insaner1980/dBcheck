package com.dbcheck.app.util

import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import com.dbcheck.app.testStringContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException
import java.lang.reflect.InvocationTargetException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PdfPageLifecycleTest {
    @After
    fun tearDown() {
        unmockkConstructor(PdfDocument.PageInfo.Builder::class)
    }

    @Test
    fun drawingFailureFinishesOwnedPageBeforeDocumentCanClose() {
        mockkConstructor(PdfDocument.PageInfo.Builder::class)
        val pageInfo = mockk<PdfDocument.PageInfo>()
        every { anyConstructed<PdfDocument.PageInfo.Builder>().create() } returns pageInfo
        val canvas = mockk<Canvas>(relaxed = true)
        val page = mockk<PdfDocument.Page>()
        every { page.canvas } returns canvas
        val document = mockk<PdfDocument>(relaxed = true)
        every { document.startPage(pageInfo) } returns page
        val error = IOException("Page drawing failed")
        every { canvas.drawColor(any()) } throws error
        val styleClass = Class.forName("com.dbcheck.app.util.PdfReportStyle")
        val style = mockkClass(styleClass.kotlin, relaxed = true)
        val method = ExportPdfReportUseCase::class.java.getDeclaredMethod(
            "drawPage", PdfDocument::class.java, styleClass,
            Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
            String::class.java, Function1::class.java,
        ).apply { isAccessible = true }
        val exporter = ExportPdfReportUseCase(testStringContext(), UnconfinedTestDispatcher())

        val failure = try {
            method.invoke(exporter, document, style, 1, 5, "Summary", { _: Canvas -> Unit })
            null
        } catch (failure: InvocationTargetException) {
            failure.cause
        }

        assertSame(error, failure)
        verify(exactly = 1) { document.finishPage(page) }
    }
}

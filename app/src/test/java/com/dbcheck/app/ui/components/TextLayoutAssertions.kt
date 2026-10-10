package com.dbcheck.app.ui.components

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

internal fun SemanticsNodeInteraction.assertTextFits(text: String) {
    val results = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
        assertTrue(action(results))
    }
    assertEquals(1, results.size)
    val layout = results.single()
    assertEquals(text.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
    assertTrue("Text is clipped vertically: $text", layout.getLineBottom(layout.lineCount - 1) <= layout.size.height)
    repeat(layout.lineCount) { line ->
        assertTrue(
            "Text is clipped horizontally: $text",
            layout.getLineRight(line) - layout.getLineLeft(line) <= layout.size.width,
        )
    }
}

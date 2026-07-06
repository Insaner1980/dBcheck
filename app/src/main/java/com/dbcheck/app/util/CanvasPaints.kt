package com.dbcheck.app.util

import android.graphics.Paint
import android.graphics.Typeface

internal fun sansSerifPaint(color: Int, textSize: Float, bold: Boolean): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSize
        typeface =
            Typeface.create(
                "sans-serif",
                if (bold) Typeface.BOLD else Typeface.NORMAL,
            )
    }

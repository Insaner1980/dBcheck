package com.dbcheck.app.util

internal sealed interface PdfTimeSeriesShape {
    data object Empty : PdfTimeSeriesShape

    data class SinglePoint(val point: PdfChartPoint) : PdfTimeSeriesShape

    data class Line(val points: List<PdfChartPoint>) : PdfTimeSeriesShape
}

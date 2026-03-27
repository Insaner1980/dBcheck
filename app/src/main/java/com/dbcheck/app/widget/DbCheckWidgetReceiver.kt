package com.dbcheck.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DbCheckWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DbCheckWidget()

    companion object {
        /** Call after a measurement session ends to refresh widget data. */
        suspend fun updateAllWidgets(context: Context) {
            withContext(Dispatchers.Main) {
                val manager = GlanceAppWidgetManager(context)
                val widget = DbCheckWidget()
                manager.getGlanceIds(DbCheckWidget::class.java).forEach { id ->
                    widget.update(context, id)
                }
            }
        }
    }
}

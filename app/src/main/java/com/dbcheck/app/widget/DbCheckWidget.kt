package com.dbcheck.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.dbcheck.app.MainActivity
import com.dbcheck.app.R
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.util.labelStringRes
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun sessionDao(): SessionDao

    fun userPreferencesDataStore(): UserPreferencesDataStore
}

class DbCheckWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            )
        val sessionDao = entryPoint.sessionDao()
        val prefsStore = entryPoint.userPreferencesDataStore()

        val prefs = prefsStore.userPreferences.firstOrNull()
        val isPro = prefs?.isProUser ?: false

        val lastSession: SessionEntity? =
            if (isPro) {
                sessionDao.getRecentSessions(1).firstOrNull()?.firstOrNull()
            } else {
                null
            }

        provideContent {
            GlanceTheme {
                val text = WidgetTextResources.from(context)
                when (widgetContentMode(isPro = isPro, lastSession = lastSession)) {
                    WidgetContentMode.PRO_LOCKED -> ProLockedContent(text)
                    WidgetContentMode.SESSION -> SessionContent(session = requireNotNull(lastSession), text = text)
                    WidgetContentMode.EMPTY -> EmptyContent(text)
                }
            }
        }
    }

    @Composable
    private fun SessionContent(session: SessionEntity, text: WidgetTextResources) {
        val noiseLevel = NoiseLevel.fromDb(session.avgDb)
        val timeAgo = formatTimeAgo(session.endTime ?: session.startTime, text)

        WidgetSurface {
            WidgetBrandLabel(text)
            Spacer(GlanceModifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${session.avgDb.toInt()}",
                    style =
                        TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface,
                        ),
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = text.dbUnit,
                    style =
                        TextStyle(
                            fontSize = 14.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                        ),
                )
            }
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = text.noiseLevelLabel(noiseLevel),
                style =
                    TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.primary,
                    ),
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = timeAgo,
                style =
                    TextStyle(
                        fontSize = 10.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }

    @Composable
    private fun EmptyContent(text: WidgetTextResources) {
        WidgetSurface {
            WidgetBrandLabel(text)
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = text.emptyTitle,
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface,
                    ),
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = text.emptySubtitle,
                style =
                    TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }

    @Composable
    private fun ProLockedContent(text: WidgetTextResources) {
        WidgetSurface(centerHorizontally = true) {
            Text(
                text = "\uD83D\uDD12",
                style = TextStyle(fontSize = 20.sp),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = text.proTitle,
                style =
                    TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = text.upgradeToUnlock,
                style =
                    TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }

    @Composable
    @Suppress("FunctionNaming")
    private fun WidgetSurface(centerHorizontally: Boolean = false, content: @Composable () -> Unit) {
        val modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>())
        val horizontalAlignment =
            if (centerHorizontally) {
                Alignment.CenterHorizontally
            } else {
                Alignment.Start
            }

        Column(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = horizontalAlignment,
        ) {
            content()
        }
    }

    @Composable
    @Suppress("FunctionNaming")
    private fun WidgetBrandLabel(text: WidgetTextResources) {
        Text(
            text = text.brand,
            style =
                TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
        )
    }

    private fun formatTimeAgo(timestampMs: Long, text: WidgetTextResources): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestampMs
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)

        return when {
            diffMinutes < 1 -> {
                text.justNow
            }

            diffMinutes < 60 -> {
                text.minutesAgo(diffMinutes)
            }

            diffHours < 24 -> {
                text.hoursAgo(diffHours)
            }

            diffDays < 7 -> {
                text.daysAgo(diffDays)
            }

            else -> {
                val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestampMs))
            }
        }
    }
}

internal enum class WidgetContentMode {
    PRO_LOCKED,
    SESSION,
    EMPTY,
}

internal fun widgetContentMode(isPro: Boolean, lastSession: SessionEntity?): WidgetContentMode = when {
    !isPro -> WidgetContentMode.PRO_LOCKED
    lastSession != null && lastSession.avgDb > 0f -> WidgetContentMode.SESSION
    else -> WidgetContentMode.EMPTY
}

private data class WidgetTextResources(
    val context: Context,
    val brand: String,
    val dbUnit: String,
    val emptyTitle: String,
    val emptySubtitle: String,
    val proTitle: String,
    val upgradeToUnlock: String,
    val justNow: String,
) {
    fun minutesAgo(minutes: Long): String = context.getString(R.string.widget_minutes_ago, minutes)

    fun hoursAgo(hours: Long): String = context.getString(R.string.widget_hours_ago, hours)

    fun daysAgo(days: Long): String = context.getString(R.string.widget_days_ago, days)

    fun noiseLevelLabel(level: NoiseLevel): String = context.getString(level.labelStringRes())

    companion object {
        fun from(context: Context): WidgetTextResources = WidgetTextResources(
                context = context,
                brand = context.getString(R.string.widget_brand),
                dbUnit = context.getString(R.string.unit_db),
                emptyTitle = context.getString(R.string.widget_empty_title),
                emptySubtitle = context.getString(R.string.widget_empty_subtitle),
                proTitle = context.getString(R.string.widget_pro_title),
                upgradeToUnlock = context.getString(R.string.widget_upgrade_to_unlock),
                justNow = context.getString(R.string.widget_just_now),
            )
    }
}

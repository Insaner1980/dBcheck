package com.dbcheck.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.preferences.UserPreferencesDataStore
import com.dbcheck.app.domain.noise.NoiseLevel
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
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
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
                if (!isPro) {
                    ProLockedContent()
                } else if (lastSession != null && lastSession.avgDb > 0f) {
                    SessionContent(session = lastSession)
                } else {
                    EmptyContent()
                }
            }
        }
    }

    @Composable
    private fun SessionContent(session: SessionEntity) {
        val noiseLevel = NoiseLevel.fromDb(session.avgDb)
        val timeAgo = formatTimeAgo(session.endTime ?: session.startTime)

        DataWidgetColumn {
            WidgetBrand()
            Spacer(GlanceModifier.height(DbCheckWidgetSpacing.XSmall))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${session.avgDb.toInt()}",
                    style =
                        TextStyle(
                            fontSize = DbCheckWidgetTypography.MetricValue,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface,
                        ),
                )
                Spacer(GlanceModifier.width(DbCheckWidgetSpacing.XSmall))
                Text(
                    text = "dB",
                    style =
                        TextStyle(
                            fontSize = DbCheckWidgetTypography.MetricUnit,
                            color = GlanceTheme.colors.onSurfaceVariant,
                        ),
                )
            }
            Spacer(GlanceModifier.height(DbCheckWidgetSpacing.Tiny))
            Text(
                text = noiseLevel.label,
                style =
                    TextStyle(
                        fontSize = DbCheckWidgetTypography.Brand,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.primary,
                    ),
            )
            Spacer(GlanceModifier.height(DbCheckWidgetSpacing.Tiny))
            Text(
                text = timeAgo,
                style =
                    TextStyle(
                        fontSize = DbCheckWidgetTypography.Metadata,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }

    @Composable
    private fun EmptyContent() {
        DataWidgetColumn {
            WidgetBrand()
            Spacer(GlanceModifier.height(DbCheckWidgetSpacing.Small))
            Text(
                text = "No data yet",
                style =
                    TextStyle(
                        fontSize = DbCheckWidgetTypography.MetricUnit,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface,
                    ),
            )
            Spacer(GlanceModifier.height(DbCheckWidgetSpacing.Tiny))
            Text(
                text = "Tap to start measuring",
                style =
                    TextStyle(
                        fontSize = DbCheckWidgetTypography.Brand,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }

    @Composable
    private fun DataWidgetColumn(content: @Composable () -> Unit) {
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.widgetBackground)
                    .padding(DbCheckWidgetSpacing.ContentPadding)
                    .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }

    @Composable
    private fun WidgetBrand() {
        Text(
            text = "dBcheck",
            style =
                TextStyle(
                    fontSize = DbCheckWidgetTypography.Brand,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
        )
    }

    @Composable
    private fun ProLockedContent() {
        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.widgetBackground)
                    .padding(DbCheckWidgetSpacing.ContentPadding)
                    .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "\uD83D\uDD12",
                style = TextStyle(fontSize = DbCheckWidgetTypography.LockedIcon),
            )
            Spacer(GlanceModifier.height(DbCheckWidgetSpacing.XSmall))
            Text(
                text = "dBcheck Pro",
                style =
                    TextStyle(
                        fontSize = DbCheckWidgetTypography.LockedTitle,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
            )
            Spacer(GlanceModifier.height(DbCheckWidgetSpacing.Tiny))
            Text(
                text = "Upgrade to unlock",
                style =
                    TextStyle(
                        fontSize = DbCheckWidgetTypography.Brand,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }

    private fun formatTimeAgo(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - timestampMs
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)

        return when {
            diffMinutes < 1 -> {
                "Just now"
            }

            diffMinutes < 60 -> {
                "${diffMinutes}m ago"
            }

            diffHours < 24 -> {
                "${diffHours}h ago"
            }

            diffDays < 7 -> {
                "${diffDays}d ago"
            }

            else -> {
                val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestampMs))
            }
        }
    }
}

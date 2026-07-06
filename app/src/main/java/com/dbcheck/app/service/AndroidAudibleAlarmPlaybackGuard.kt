package com.dbcheck.app.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAudibleAlarmPlaybackGuard @Inject constructor(@ApplicationContext context: Context) :
    AudibleAlarmPlaybackGuard,
    SensorEventListener {
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    @Volatile
    private var proximityCovered = false

    override fun startMonitoring() {
        proximitySensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun stopMonitoring() {
        sensorManager?.unregisterListener(this)
        proximityCovered = false
    }

    override fun canPlayAudibleAlarm(): Boolean = (powerManager?.isInteractive ?: true) && !proximityCovered

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val distance = event.values.firstOrNull() ?: return
        proximityCovered = distance < event.sensor.maximumRange
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

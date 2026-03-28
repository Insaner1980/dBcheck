package com.dbcheck.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticFeedbackHelper
    @Inject
    constructor(
        private val context: Context,
    ) {
        private val vibrator: Vibrator by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }

        fun lightTick() {
            vibrator.vibrate(
                VibrationEffect.createOneShot(10, VibrationEffect.EFFECT_TICK),
            )
        }

        fun mediumClick() {
            vibrator.vibrate(
                VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
    }

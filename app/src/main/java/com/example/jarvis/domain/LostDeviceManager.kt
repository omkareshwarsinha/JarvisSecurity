package com.example.jarvis.domain

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MobiArmour Layer 6: Authorized Lost-Device Actions
 *
 * Implements user-authorized emergency response actions:
 * 1. High-decibel audible security siren / alarm ring (to locate misplaced device or scare interceptor)
 * 2. Device Administration lock screen trigger (requires user Device Admin opt-in)
 * 3. Emergency SOS contact & message preparation
 * 4. Guided remote security hardening checklist (Google Find My Device, remote wipe guides)
 */
class LostDeviceManager(private val context: Context) {

    private var activeRingtone: Ringtone? = null

    private val _isAlarmSounding = MutableStateFlow(false)
    val isAlarmSounding: StateFlow<Boolean> = _isAlarmSounding.asStateFlow()

    private val _isDeviceAdminActive = MutableStateFlow(checkDeviceAdminActive())
    val isDeviceAdminActive: StateFlow<Boolean> = _isDeviceAdminActive.asStateFlow()

    fun checkDeviceAdminActive(): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.isAdminActive(ComponentName(context, MobiArmourAdminReceiver::class.java)) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun refreshAdminStatus() {
        _isDeviceAdminActive.value = checkDeviceAdminActive()
    }

    /**
     * Triggers high-decibel emergency locating alarm on the device
     */
    fun startEmergencyAlarm() {
        if (_isAlarmSounding.value) return
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            // Maximize alarm and music stream volumes for audibility
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            }

            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            activeRingtone = RingtoneManager.getRingtone(context, alarmUri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                play()
            }
            _isAlarmSounding.value = true
        } catch (e: Exception) {
            _isAlarmSounding.value = false
        }
    }

    /**
     * Stops the audible siren
     */
    fun stopEmergencyAlarm() {
        try {
            activeRingtone?.stop()
            activeRingtone = null
        } catch (e: Exception) {
            // Ignore stop errors
        } finally {
            _isAlarmSounding.value = false
        }
    }

    /**
     * Locks the device screen immediately if Device Admin is authorized by the user
     */
    fun triggerInstantScreenLock(): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            if (dpm != null && dpm.isAdminActive(ComponentName(context, MobiArmourAdminReceiver::class.java))) {
                dpm.lockNow()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getDeviceAdminActivationIntent(): Intent {
        val componentName = ComponentName(context, MobiArmourAdminReceiver::class.java)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "MobiArmour requires Device Admin authorization to allow you to instantly lock the screen during lost-device emergencies."
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getFindMyDeviceIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/android/find")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    companion object {
        @Volatile
        private var instance: LostDeviceManager? = null

        fun getInstance(context: Context): LostDeviceManager {
            return instance ?: synchronized(this) {
                instance ?: LostDeviceManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

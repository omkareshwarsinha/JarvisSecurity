package com.example.jarvis.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import com.example.jarvis.data.db.JarvisDatabase
import com.example.jarvis.data.db.entities.SecurityEventEntity
import com.example.jarvis.data.model.FindingSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for authorized incoming SMS messages from configured Trusted Devices.
 * Executes emergency response actions:
 *  - /lockdown : Full device emergency response (high-decibel siren + screen lock + GPS location SMS reply)
 *  - /siren    : High-decibel audible locator alarm
 *  - /location : Live GPS coordinate reply
 *  - /silence  : Stops alarm audio
 */
class RemoteLockdownReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")

        val pendingResult = goAsync()

        val database = JarvisDatabase.getInstance(context)
        val lostDeviceManager = LostDeviceManager.getInstance(context)

        receiverScope.launch {
            try {
                val trustedDevices = database.trustedDeviceDao().getEnabledDevices()
                if (trustedDevices.isEmpty()) return@launch

                for (pdu in pdus) {
                    val pduBytes = pdu as? ByteArray ?: continue
                    val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pduBytes, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pduBytes)
                    } ?: continue

                    val senderNumber = message.originatingAddress ?: continue
                    val messageBody = message.messageBody?.trim() ?: continue

                    // Check if sender matches any authorized trusted device
                    val matchingDevice = trustedDevices.find { device ->
                        isPhoneNumberMatch(device.phoneNumber, senderNumber)
                    }

                    if (matchingDevice != null) {
                        val parsedCommand = SmsCommandParser.parse(messageBody)
                        if (parsedCommand != ParsedSmsCommand.Unknown) {
                            Log.i("RemoteLockdown", "Authorized command received from ${matchingDevice.contactName}: $messageBody")
                            handleAuthorizedCommand(
                                context = context,
                                senderNumber = senderNumber,
                                contactName = matchingDevice.contactName,
                                command = parsedCommand,
                                lostDeviceManager = lostDeviceManager,
                                database = database
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e("RemoteLockdown", "Error processing incoming SMS command", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAuthorizedCommand(
        context: Context,
        senderNumber: String,
        contactName: String,
        command: ParsedSmsCommand,
        lostDeviceManager: LostDeviceManager,
        database: JarvisDatabase
    ) {
        when (command) {
            is ParsedSmsCommand.Lockdown -> {
                // 1. High-decibel siren alarm
                lostDeviceManager.startEmergencyAlarm()

                // 2. Instant Device Admin screen lock
                val lockSucceeded = lostDeviceManager.triggerInstantScreenLock()

                // 3. Obtain current location (cached/last known fix)
                val location = getBestLocation(context)
                val locationStr = if (location != null) {
                    "https://maps.google.com/?q=${location.latitude},${location.longitude} (Cached fix, Acc: ${location.accuracy.toInt()}m)"
                } else {
                    "GPS fix unavailable or location services disabled"
                }

                // 4. Send SMS response back to trusted contact
                val reply = "MobiArmour ALERT: Lockdown executed on command from $contactName!\n" +
                        "Siren: ACTIVE\n" +
                        "Screen Lock: ${if (lockSucceeded) "ENGAGED" else "ADMIN NOT GRANTED"}\n" +
                        "Location: $locationStr"
                sendSmsReply(senderNumber, reply)

                // 5. Log Security Event
                database.securityEventDao().insertEvent(
                    SecurityEventEntity(
                        timestamp = System.currentTimeMillis(),
                        title = "Remote Emergency Lockdown Triggered",
                        category = "EMERGENCY_DEFENSE",
                        severity = "CRITICAL",
                        description = "Authorized lockdown command executed via SMS from $contactName ($senderNumber).",
                        source = "REMOTE_COMMAND",
                        details = "Device locked and audible siren activated."
                    )
                )
            }

            is ParsedSmsCommand.Siren -> {
                lostDeviceManager.startEmergencyAlarm()
                sendSmsReply(senderNumber, "MobiArmour: High-decibel siren activated by $contactName.")
                database.securityEventDao().insertEvent(
                    SecurityEventEntity(
                        timestamp = System.currentTimeMillis(),
                        title = "Remote Siren Alarm Activated",
                        category = "EMERGENCY_DEFENSE",
                        severity = "WARNING",
                        description = "Emergency siren initiated via SMS from $contactName.",
                        source = "REMOTE_COMMAND",
                        details = "Audible locator active."
                    )
                )
            }

            is ParsedSmsCommand.Location -> {
                val location = getBestLocation(context)
                val reply = if (location != null) {
                    "MobiArmour Location: https://maps.google.com/?q=${location.latitude},${location.longitude} (Cached fix, Acc: ${location.accuracy.toInt()}m)"
                } else {
                    "MobiArmour: Unable to acquire GPS lock. Please ensure location is enabled."
                }
                sendSmsReply(senderNumber, reply)
                database.securityEventDao().insertEvent(
                    SecurityEventEntity(
                        timestamp = System.currentTimeMillis(),
                        title = "Remote Location Dispatched",
                        category = "EMERGENCY_DEFENSE",
                        severity = "INFO",
                        description = "GPS coordinates sent to $contactName ($senderNumber).",
                        source = "REMOTE_COMMAND",
                        details = "Coordinates dispatched via SMS."
                    )
                )
            }

            is ParsedSmsCommand.Silence -> {
                lostDeviceManager.stopEmergencyAlarm()
                sendSmsReply(senderNumber, "MobiArmour: Alarm siren stopped.")
            }

            ParsedSmsCommand.Unknown -> { /* No-op */ }
        }
    }

    private fun getBestLocation(context: Context): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val gpsLoc = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: SecurityException) { null }
            val netLoc = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: SecurityException) { null }
            val passiveLoc = try { lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) } catch (e: SecurityException) { null }

            val candidates = listOfNotNull(gpsLoc, netLoc, passiveLoc)
            candidates.maxByOrNull { it.time }
        } catch (e: Throwable) {
            null
        }
    }

    private fun sendSmsReply(recipientNumber: String, text: String) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Get default instance safely
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            // If message is longer than 160 chars, divide message
            val parts = smsManager.divideMessage(text)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(recipientNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(recipientNumber, null, text, null, null)
            }
        } catch (e: Throwable) {
            Log.e("RemoteLockdown", "Failed to send SMS reply", e)
        }
    }

    private fun isPhoneNumberMatch(trustedNumber: String, incomingNumber: String): Boolean {
        val cleanTrusted = trustedNumber.filter { it.isDigit() }
        val cleanIncoming = incomingNumber.filter { it.isDigit() }

        if (cleanTrusted == cleanIncoming) return true

        // Match trailing 10 digits for country code variations (e.g. +1800... vs 800...)
        if (cleanTrusted.length >= 10 && cleanIncoming.length >= 10) {
            val subTrusted = cleanTrusted.takeLast(10)
            val subIncoming = cleanIncoming.takeLast(10)
            if (subTrusted == subIncoming) return true
        }

        return false
    }
}

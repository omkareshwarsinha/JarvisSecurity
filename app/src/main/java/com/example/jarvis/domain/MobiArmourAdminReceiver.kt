package com.example.jarvis.domain

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * DeviceAdminReceiver for MobiArmour to support user-authorized remote/local screen locking
 * when device is misplaced or under immediate physical tampering risk.
 */
class MobiArmourAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "MobiArmour: Device Admin protection enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "MobiArmour: Device Admin protection disabled", Toast.LENGTH_SHORT).show()
    }
}

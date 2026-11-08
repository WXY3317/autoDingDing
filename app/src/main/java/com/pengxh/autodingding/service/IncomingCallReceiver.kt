package com.pengxh.autodingding.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.pengxh.autodingding.ui.MainActivity
import com.pengxh.autodingding.utils.PushDiagnostics

/** Starts the command on the first ringing event; MainActivity waits until resumed to execute it. */
class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return

        val now = System.currentTimeMillis()
        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val pendingAt = prefs.getLong(KEY_PENDING_AT, 0L)
                if (pendingAt != 0L && now - pendingAt in 0L..MAX_CALL_DURATION_MS) return
                prefs.edit().putLong(KEY_PENDING_AT, now).apply()
                PushDiagnostics.phoneCall(context, "来电响铃，正在尝试打开钉钉")
                val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "autoDing:incomingCall")
                    .acquire(10_000L)
                try {
                    context.startActivity(Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_LAUNCH_DING))
                } catch (e: Exception) {
                    Log.e("IncomingCallReceiver", "Unable to open command activity", e)
                    PushDiagnostics.phoneCall(context, "来电指令打开页面失败：${e.javaClass.simpleName}")
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                prefs.edit().remove(KEY_PENDING_AT).apply()
            }
        }
    }

    companion object {
        const val PREFS_NAME = "incoming_call_command"
        const val KEY_ENABLED = "enabled"
        private const val KEY_PENDING_AT = "pending_at"
        private const val MAX_CALL_DURATION_MS = 30 * 60 * 1000L
    }
}

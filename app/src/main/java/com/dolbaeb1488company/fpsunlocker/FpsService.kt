package com.dolbaeb1488company.fpsunlocker

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat

class FpsService : Service() {

    private val CHANNEL_ID = "fps_unlocker_channel"
    private val NOTIFICATION_ID = 1
    
    companion object {
        const val ACTION_TOGGLE_FPS = "com.dolbaeb1488company.fpsunlocker.TOGGLE_FPS"
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_TOGGLE_FPS) {
                toggleFps()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceiver(receiver, IntentFilter(ACTION_TOGGLE_FPS))
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun toggleFps() {
        try {
            val currentValue = Settings.System.getString(contentResolver, "peak_refresh_rate")
            val isChecked = currentValue != "1"
            Settings.System.putString(contentResolver, "peak_refresh_rate", if (isChecked) "1" else "120.00001")
            
            // Notify UI if it's running
            sendBroadcast(Intent("com.dolbaeb1488company.fpsunlocker.UPDATE_UI").putExtra("isChecked", isChecked))
            
            // Update notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.e("FpsService", "Error toggling FPS", e)
        }
    }

    private fun buildNotification(): Notification {
        val currentValue = Settings.System.getString(contentResolver, "peak_refresh_rate")
        val isEnabled = currentValue == "1"
        val statusText = if (isEnabled) getString(R.string.on) else getString(R.string.off)
        
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val toggleIntent = Intent(ACTION_TOGGLE_FPS)
        val togglePendingIntent = PendingIntent.getBroadcast(this, 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE)

        val actionText = if (isEnabled) getString(R.string.turn_off) else getString(R.string.turn_on)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content, statusText))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_manage, actionText, togglePendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "FPS Unlocker Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}

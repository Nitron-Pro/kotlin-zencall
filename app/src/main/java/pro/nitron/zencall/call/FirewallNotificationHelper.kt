package pro.nitron.zencall.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import pro.nitron.zencall.MainActivity
import pro.nitron.zencall.R

object FirewallNotificationHelper {

    private const val CHANNEL_ID = "zencall_firewall_blocks"
    private const val CHANNEL_NAME = "تماس‌های مسدودشده فایروال"
    private const val CHANNEL_DESC = "نمایش اعلان به هنگام مسدودسازی تماس‌های مزاحم یا خارج از زمانبندی"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(false)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showBlockedCallNotification(
        context: Context,
        rawNumber: String?,
        callerName: String?,
        reason: String,
        modeName: String?
    ) {
        try {
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "تماس مسدود شد: ${callerName ?: rawNumber ?: "ناشناس"}"
            val content = buildString {
                if (callerName != null && rawNumber != null) {
                    append("$rawNumber • ")
                }
                append(reason)
                if (!modeName.isNullOrBlank()) {
                    append(" (حالت: $modeName)")
                }
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_zencall_logo)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val notificationId = (rawNumber?.hashCode() ?: System.currentTimeMillis().toInt()).and(0x7FFFFFFF)
            notificationManager?.notify(notificationId, builder.build())
        } catch (_: Exception) {
            // Safe guard against background notification errors
        }
    }
}

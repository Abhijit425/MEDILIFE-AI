package com.medilife.ai.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.medilife.ai.MainActivity
import com.medilife.ai.R

object Notifications {
    const val CH_MEDS = "medicine_reminders"
    const val CH_WELLNESS = "wellness_reminders"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_MEDS, "Medicine reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Reminders for your scheduled medicine doses"
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CH_WELLNESS, "Wellness reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Hydration and wellness nudges"
        })
    }

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(ctx: Context, req: Int) = PendingIntent.getActivity(
        ctx, req, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun showDose(ctx: Context, medId: Long, at: Long, title: String, body: String) {
        if (!canPost(ctx)) return
        val notifId = (medId * 10_000 + (at / 60_000) % 10_000).toInt()
        fun action(status: String, req: Int) = PendingIntent.getBroadcast(
            ctx, notifId * 2 + req,
            Intent(ctx, DoseActionReceiver::class.java).apply {
                putExtra(DoseActionReceiver.EXTRA_MED, medId)
                putExtra(DoseActionReceiver.EXTRA_AT, at)
                putExtra(DoseActionReceiver.EXTRA_STATUS, status)
                putExtra(DoseActionReceiver.EXTRA_NOTIF, notifId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CH_MEDS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx, notifId))
            .addAction(0, "Taken", action("TAKEN", 0))
            .addAction(0, "Skip", action("SKIPPED", 1))
            .build()
        try { NotificationManagerCompat.from(ctx).notify(notifId, n) } catch (_: SecurityException) { }
    }

    fun showWellness(ctx: Context, id: Int, title: String, body: String) {
        if (!canPost(ctx)) return
        val n = NotificationCompat.Builder(ctx, CH_WELLNESS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx, id))
            .build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) { }
    }
}

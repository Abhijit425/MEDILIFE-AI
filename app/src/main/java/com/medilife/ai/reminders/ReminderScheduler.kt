package com.medilife.ai.reminders

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.medilife.ai.data.DoseSchedule
import com.medilife.ai.data.MedicineEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Schedules one daily WorkManager job per medicine dose time. WorkManager survives
 * reboots; Android may delay delivery slightly under battery optimisation.
 */
class ReminderScheduler(private val ctx: Context) {
    private val wm get() = WorkManager.getInstance(ctx)

    fun rescheduleAll(meds: List<MedicineEntity>, enabled: Boolean) {
        wm.cancelAllWorkByTag(TAG_DOSE)
        if (!enabled) return
        meds.filter { it.active && it.remindersOn }.forEach { m ->
            DoseSchedule.parseTimes(m.times).forEach { t ->
                val now = LocalDateTime.now()
                var next = LocalDate.now().atTime(t)
                if (!next.isAfter(now)) next = next.plusDays(1)
                val delay = java.time.Duration.between(now, next).toMillis()
                val req = PeriodicWorkRequestBuilder<DoseReminderWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag(TAG_DOSE)
                    .setInputData(workDataOf(DoseReminderWorker.K_MED to m.id, DoseReminderWorker.K_H to t.hour, DoseReminderWorker.K_M to t.minute))
                    .build()
                wm.enqueueUniquePeriodicWork("dose_${m.id}_${t.hour}_${t.minute}", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, req)
            }
        }
    }

    fun setHydration(enabled: Boolean) {
        if (!enabled) { wm.cancelUniqueWork(HYDRATION); return }
        val req = PeriodicWorkRequestBuilder<HydrationWorker>(2, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork(HYDRATION, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancelAll() { wm.cancelAllWorkByTag(TAG_DOSE); wm.cancelUniqueWork(HYDRATION) }

    companion object {
        const val TAG_DOSE = "dose_reminder"
        const val HYDRATION = "hydration_reminder"
        val zone: ZoneId get() = ZoneId.systemDefault()
    }
}

package com.medilife.ai.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medilife.ai.MediLifeApp
import com.medilife.ai.data.DoseSchedule
import com.medilife.ai.data.DoseStatus
import com.medilife.ai.data.FoodRelation
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

class DoseReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as MediLifeApp).container
        if (!c.settings.current().remindersEnabled) return Result.success()
        val medId = inputData.getLong(K_MED, -1)
        val time = LocalTime.of(inputData.getInt(K_H, 8), inputData.getInt(K_M, 0))
        val med = c.repo.medicine(medId) ?: return Result.success()
        val today = LocalDate.now()
        if (!med.remindersOn || !DoseSchedule.isActiveOn(med, today)) return Result.success()
        val at = DoseSchedule.epoch(today, time)
        val logs = c.repo.doseLogsSince(DoseSchedule.startOfDay(today)).first()
        if (logs.any { it.medicineId == medId && it.scheduledAt == at && it.status != DoseStatus.PENDING }) return Result.success()
        val food = if (med.foodRelation != FoodRelation.ANY) " · ${med.foodRelation}" else ""
        Notifications.showDose(
            applicationContext, medId, at,
            "Time for ${med.name} ${med.strength}".trim(),
            "${med.dosage.ifBlank { "As prescribed" }}$food. Tap Taken or Skip to log it."
        )
        return Result.success()
    }

    companion object { const val K_MED = "med"; const val K_H = "h"; const val K_M = "m" }
}

class HydrationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as MediLifeApp).container
        if (!c.settings.current().hydrationReminders) return Result.success()
        val h = LocalTime.now().hour
        if (h in 8..20) Notifications.showWellness(applicationContext, 7001, "Hydration check 💧", "A glass of water now keeps you on track — within any fluid limit your doctor has set.")
        return Result.success()
    }
}

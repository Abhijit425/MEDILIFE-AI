package com.medilife.ai.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class ScheduledDose(
    val medicine: MedicineEntity,
    val time: LocalTime,
    val at: Long,
    val status: String
)

object DoseSchedule {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun parseTimes(times: String): List<LocalTime> = times.split(",")
        .mapNotNull { t ->
            val p = t.trim().split(":")
            if (p.size == 2) runCatching { LocalTime.of(p[0].toInt(), p[1].toInt()) }.getOrNull() else null
        }.sorted()

    fun dayOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    fun epoch(date: LocalDate, time: LocalTime): Long = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun isActiveOn(m: MedicineEntity, date: LocalDate): Boolean {
        if (!m.active) return false
        if (dayOf(m.startDate).isAfter(date)) return false
        val end = m.endDate?.let { dayOf(it) }
        return end == null || !end.isBefore(date)
    }

    fun dosesFor(date: LocalDate, meds: List<MedicineEntity>, logs: List<DoseLogEntity>): List<ScheduledDose> {
        val byKey = logs.associateBy { it.medicineId to it.scheduledAt }
        return meds.filter { isActiveOn(it, date) }.flatMap { m ->
            parseTimes(m.times).map { t ->
                val at = epoch(date, t)
                ScheduledDose(m, t, at, byKey[m.id to at]?.status ?: DoseStatus.PENDING)
            }
        }.sortedBy { it.at }
    }

    /** Adherence over the last 7 days, counting only doses whose time has passed. Null if nothing scheduled. */
    fun weeklyAdherence(meds: List<MedicineEntity>, logs: List<DoseLogEntity>, now: Long = System.currentTimeMillis()): Int? {
        val today = dayOf(now)
        var due = 0; var taken = 0
        for (i in 0..6) {
            dosesFor(today.minusDays(i.toLong()), meds, logs).filter { it.at <= now }.forEach {
                due++; if (it.status == DoseStatus.TAKEN) taken++
            }
        }
        return if (due == 0) null else (taken * 100 / due)
    }

    fun nextDose(meds: List<MedicineEntity>, logs: List<DoseLogEntity>, now: Long = System.currentTimeMillis()): ScheduledDose? {
        val today = dayOf(now)
        return (dosesFor(today, meds, logs) + dosesFor(today.plusDays(1), meds, logs))
            .firstOrNull { it.at > now && it.status == DoseStatus.PENDING }
    }
}

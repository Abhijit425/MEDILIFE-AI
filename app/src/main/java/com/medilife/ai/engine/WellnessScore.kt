package com.medilife.ai.engine

import com.medilife.ai.data.DoseSchedule
import com.medilife.ai.data.HealthSnapshot
import com.medilife.ai.data.MeasureType

data class WellnessScore(val score: Int, val parts: List<Pair<String, Int>>, val note: String)

/**
 * A habit/engagement score built only from what the user logs (adherence, logging
 * consistency, self-reported sleep and activity). It is explicitly NOT a clinical
 * measurement and makes no diagnostic claim.
 */
object WellnessScoreCalc {
    const val LABEL = "Wellness habits score"
    const val NOTE = "Based only on your logged habits (medicine adherence, logging, sleep, activity). Not a medical or clinical measurement."

    fun compute(s: HealthSnapshot, now: Long = System.currentTimeMillis()): WellnessScore? {
        val weekAgo = now - 7L * 86_400_000
        val parts = mutableListOf<Pair<String, Int>>()
        DoseSchedule.weeklyAdherence(s.medicines, s.doseLogs, now)?.let { parts += "Medicine adherence" to it }
        val recent = s.measurements.filter { it.timestamp >= weekAgo }
        val loggedDays = recent.map { DoseSchedule.dayOf(it.timestamp) }.distinct().size
        if (s.measurements.isNotEmpty()) parts += "Logging consistency" to (loggedDays * 100 / 7).coerceAtMost(100)
        val sleep = recent.filter { it.type == MeasureType.SLEEP }.map { it.value }
        if (sleep.isNotEmpty()) parts += "Sleep (goal 7 h)" to ((sleep.average() / 7.0) * 100).toInt().coerceIn(0, 100)
        val steps = recent.filter { it.type == MeasureType.STEPS }.map { it.value }
        if (steps.isNotEmpty()) parts += "Activity (goal 7,000 steps)" to ((steps.average() / 7000.0) * 100).toInt().coerceIn(0, 100)
        if (parts.size < 2) return null
        return WellnessScore(parts.map { it.second }.average().toInt(), parts, NOTE)
    }
}

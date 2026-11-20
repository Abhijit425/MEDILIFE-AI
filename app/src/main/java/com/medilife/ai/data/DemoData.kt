package com.medilife.ai.data

import com.medilife.ai.engine.LabParser
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Realistic demonstration data. Every row is flagged isDemo = true so it is shown
 * with a DEMO DATA badge and can be wiped in one tap without touching real records.
 */
object DemoData {
    suspend fun insert(repo: HealthRepository) {
        val today = LocalDate.now()
        fun day(offset: Long, h: Int = 9, m: Int = 0) = DoseSchedule.epoch(today.plusDays(offset), LocalTime.of(h, m))

        repo.saveProfile(
            ProfileEntity(
                name = "Arjun Demo", age = 46, gender = "Male", heightCm = 172.0, weightKg = 81.0,
                bloodGroup = "B+", doctorName = "Dr. Meera Sen (demo)", doctorSpecialty = "General Physician",
                doctorPhone = "+91 90000 00000", clinic = "City Care Clinic (demo)",
                emergencyName = "Priya (demo)", emergencyRelation = "Spouse", emergencyPhone = "+91 90000 00001",
                isDemo = true
            )
        )
        listOf(
            MedicalRecordEntity(type = RecordType.CONDITION, title = "Type 2 diabetes", details = "Diagnosed 2019. Managed with metformin and diet.", date = day(-2200), isDemo = true),
            MedicalRecordEntity(type = RecordType.CONDITION, title = "Hypertension", details = "Diagnosed 2021.", date = day(-1500), isDemo = true),
            MedicalRecordEntity(type = RecordType.ALLERGY, title = "Penicillin", details = "Skin rash in 2010.", isDemo = true),
            MedicalRecordEntity(type = RecordType.SURGERY, title = "Appendectomy", details = "Laparoscopic, 2015, uneventful.", date = day(-3900), isDemo = true),
            MedicalRecordEntity(type = RecordType.FAMILY, title = "Father – coronary artery disease", details = "Heart attack at age 58.", isDemo = true),
            MedicalRecordEntity(type = RecordType.FAMILY, title = "Mother – type 2 diabetes", isDemo = true),
            MedicalRecordEntity(type = RecordType.PAST_MEDICINE, title = "Telmisartan 40 mg", details = "Switched to amlodipine in 2023.", isDemo = true)
        ).forEach { repo.saveRecord(it) }

        listOf(
            TimelineEventEntity(date = day(-2200), title = "Diagnosed with type 2 diabetes", category = "Diagnosis", isDemo = true),
            TimelineEventEntity(date = day(-1500), title = "Diagnosed with hypertension", category = "Diagnosis", isDemo = true),
            TimelineEventEntity(date = day(-90), title = "Annual health check-up", description = "HbA1c 7.6%, advised diet review.", category = "Check-up", isDemo = true),
            TimelineEventEntity(date = day(-7), title = "Follow-up blood tests", description = "HbA1c improved to 7.1%.", category = "Lab", isDemo = true)
        ).forEach { repo.saveTimeline(it) }

        val start = day(-30)
        val metformin = repo.saveMedicine(MedicineEntity(name = "Metformin", strength = "500 mg", dosage = "1 tablet", frequency = "Twice daily", times = "08:30,20:30", startDate = start, instructions = "Take with meals.", foodRelation = FoodRelation.AFTER, prescribedBy = "Dr. Meera Sen", isDemo = true))
        val amlo = repo.saveMedicine(MedicineEntity(name = "Amlodipine", strength = "5 mg", dosage = "1 tablet", frequency = "Once daily", times = "09:00", startDate = start, foodRelation = FoodRelation.ANY, prescribedBy = "Dr. Meera Sen", isDemo = true))
        val atorva = repo.saveMedicine(MedicineEntity(name = "Atorvastatin", strength = "10 mg", dosage = "1 tablet", frequency = "Once daily at night", times = "21:30", startDate = start, foodRelation = FoodRelation.ANY, prescribedBy = "Dr. Meera Sen", isDemo = true))
        // Deliberately shows the safety engine: amoxicillin is a penicillin and the profile records a penicillin allergy.
        repo.saveMedicine(MedicineEntity(name = "Amoxicillin", strength = "500 mg", dosage = "", frequency = "Three times daily", times = "08:00,14:00,20:00", startDate = day(-1), endDate = day(4), instructions = "Demo entry: shows an allergy alert.", foodRelation = FoodRelation.ANY, isDemo = true))
        repo.saveMedicine(MedicineEntity(name = "Ibuprofen", strength = "400 mg", dosage = "1 tablet if needed", frequency = "As needed", times = "", startDate = day(-10), expiryDate = day(-20), foodRelation = FoodRelation.AFTER, instructions = "Demo entry: expired strip.", isDemo = true))

        val rnd = Random(7)
        val now = System.currentTimeMillis()
        for (d in -7L..0L) {
            listOf(metformin to LocalTime.of(8, 30), metformin to LocalTime.of(20, 30), amlo to LocalTime.of(9, 0), atorva to LocalTime.of(21, 30)).forEach { (id, t) ->
                val at = DoseSchedule.epoch(today.plusDays(d), t)
                if (at < now) {
                    val status = if (rnd.nextInt(100) < 88) DoseStatus.TAKEN else DoseStatus.SKIPPED
                    repo.logDose(id, at, status, demo = true)
                }
            }
        }

        for (d in -13L..0L) {
            val t = day(d, 8, 0)
            val sys = 128 + rnd.nextInt(-6, 9) - (13 + d).toInt() / 3
            repo.saveMeasurement(MeasurementEntity(type = MeasureType.BLOOD_PRESSURE, value = sys.toDouble(), value2 = (82 + rnd.nextInt(-4, 5)).toDouble(), unit = "mmHg", timestamp = t, isDemo = true))
            repo.saveMeasurement(MeasurementEntity(type = MeasureType.HEART_RATE, value = (72 + rnd.nextInt(-6, 7)).toDouble(), unit = "bpm", timestamp = t, isDemo = true))
            repo.saveMeasurement(MeasurementEntity(type = MeasureType.GLUCOSE, value = (118 + rnd.nextInt(-14, 18)).toDouble(), unit = "mg/dL", timestamp = t + 60_000, note = "Fasting", isDemo = true))
            repo.saveMeasurement(MeasurementEntity(type = MeasureType.SLEEP, value = ((6.2 + rnd.nextDouble() * 1.6) * 10).roundToInt() / 10.0, unit = "h", timestamp = t - 3_600_000, isDemo = true))
            repo.saveMeasurement(MeasurementEntity(type = MeasureType.STEPS, value = (4200 + rnd.nextInt(0, 4500)).toDouble(), unit = "steps", timestamp = day(d, 21, 0), isDemo = true))
            if (d % 3 == 0L) repo.saveMeasurement(MeasurementEntity(type = MeasureType.WEIGHT, value = ((81.6 + d * 0.05 + rnd.nextDouble() * 0.4) * 10).roundToInt() / 10.0, unit = "kg", timestamp = t, isDemo = true))
            if (d % 2 == 0L) repo.saveMeasurement(MeasurementEntity(type = MeasureType.SPO2, value = (96 + rnd.nextInt(0, 3)).toDouble(), unit = "%", timestamp = t, isDemo = true))
        }

        listOf(
            SymptomEntity(name = "Headache", severity = 4, notes = "Afternoon, after screen time.", timestamp = day(-6, 15), isDemo = true),
            SymptomEntity(name = "Headache", severity = 3, timestamp = day(-3, 16), isDemo = true),
            SymptomEntity(name = "Fatigue", severity = 5, notes = "Slept poorly.", timestamp = day(-2, 11), isDemo = true),
            SymptomEntity(name = "Headache", severity = 2, timestamp = day(-1, 14), isDemo = true)
        ).forEach { repo.saveSymptom(it) }

        repo.saveAppointment(AppointmentEntity(doctor = "Dr. Meera Sen (demo)", purpose = "Diabetes & BP follow-up", location = "City Care Clinic", dateTime = day(6, 11, 0), isDemo = true))
        repo.saveAppointment(AppointmentEntity(doctor = "Eye clinic (demo)", purpose = "Annual diabetic eye screening", location = "Vision Centre", dateTime = day(24, 10, 30), isDemo = true))

        fun res(name: String, v: Double, unit: String, lo: Double?, hi: Double?) = LabResultEntity(
            reportId = 0, testName = name, value = v, valueText = v.toString(), unit = unit, refLow = lo, refHigh = hi,
            refText = when { lo != null && hi != null -> "$lo - $hi"; hi != null -> "< $hi"; lo != null -> "> $lo"; else -> "" },
            flag = LabParser.flagFor(v, lo, hi), reportDate = 0, isDemo = true
        )
        repo.saveReport(
            LabReportEntity(title = "Diabetes & lipid panel (DEMO)", labName = "Demo Diagnostics", reportDate = day(-90), sourceType = "Demo", isDemo = true),
            listOf(res("HbA1c", 7.6, "%", 4.0, 5.6), res("Fasting glucose", 142.0, "mg/dL", 70.0, 100.0), res("LDL cholesterol", 138.0, "mg/dL", null, 100.0), res("HDL cholesterol", 41.0, "mg/dL", 40.0, null), res("Creatinine", 0.96, "mg/dL", 0.7, 1.3))
        )
        repo.saveReport(
            LabReportEntity(title = "Follow-up panel (DEMO)", labName = "Demo Diagnostics", reportDate = day(-7), sourceType = "Demo", isDemo = true),
            listOf(res("HbA1c", 7.1, "%", 4.0, 5.6), res("Fasting glucose", 126.0, "mg/dL", 70.0, 100.0), res("LDL cholesterol", 112.0, "mg/dL", null, 100.0), res("HDL cholesterol", 44.0, "mg/dL", 40.0, null), res("Creatinine", 0.98, "mg/dL", 0.7, 1.3), res("Hemoglobin", 14.2, "g/dL", 13.0, 17.0))
        )
    }
}

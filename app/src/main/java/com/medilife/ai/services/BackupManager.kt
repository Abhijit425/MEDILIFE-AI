package com.medilife.ai.services

import com.medilife.ai.data.AppointmentEntity
import com.medilife.ai.data.DoseLogEntity
import com.medilife.ai.data.HealthRepository
import com.medilife.ai.data.LabReportEntity
import com.medilife.ai.data.LabResultEntity
import com.medilife.ai.data.MeasurementEntity
import com.medilife.ai.data.MedicalRecordEntity
import com.medilife.ai.data.MedicineEntity
import com.medilife.ai.data.ProfileEntity
import com.medilife.ai.data.SymptomEntity
import com.medilife.ai.data.TimelineEventEntity
import com.medilife.ai.data.FieldCrypto
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

/**
 * User-controlled backup. Only real (non-demo) records are exported, as JSON encrypted
 * with a passphrase the user chooses (PBKDF2 + AES-256-GCM). The file goes wherever the
 * user saves it — MediLife never uploads it.
 */
object BackupManager {
    private fun JSONObject.optLongOrNull(k: String) = if (isNull(k) || !has(k)) null else getLong(k)
    private fun JSONObject.optDoubleOrNull(k: String) = if (isNull(k) || !has(k)) null else getDouble(k)
    private fun JSONObject.optIntOrNull(k: String) = if (isNull(k) || !has(k)) null else getInt(k)
    private fun <T> JSONArray.objs(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }
    private fun JSONObject.n(k: String, v: Any?) = put(k, v ?: JSONObject.NULL)

    suspend fun export(repo: HealthRepository, passphrase: CharArray, out: OutputStream) {
        val s = repo.snapshot()
        val realMedIds = s.medicines.filter { !it.isDemo }.map { it.id }.toSet()
        val realReportIds = s.reports.filter { !it.isDemo }.map { it.id }.toSet()
        val root = JSONObject().put("format", "medilife-backup").put("version", 1).put("created", System.currentTimeMillis())
        s.profile?.takeIf { !it.isDemo }?.let { p ->
            root.put("profile", JSONObject().put("name", p.name).n("age", p.age).put("gender", p.gender).n("heightCm", p.heightCm).n("weightKg", p.weightKg)
                .put("bloodGroup", p.bloodGroup).put("doctorName", p.doctorName).put("doctorSpecialty", p.doctorSpecialty).put("doctorPhone", p.doctorPhone)
                .put("clinic", p.clinic).put("emergencyName", p.emergencyName).put("emergencyRelation", p.emergencyRelation).put("emergencyPhone", p.emergencyPhone))
        }
        root.put("records", JSONArray(s.records.filter { !it.isDemo }.map { JSONObject().put("type", it.type).put("title", it.title).put("details", it.details).n("date", it.date) }))
        root.put("timeline", JSONArray(s.timeline.filter { !it.isDemo }.map { JSONObject().put("date", it.date).put("title", it.title).put("description", it.description).put("category", it.category) }))
        root.put("medicines", JSONArray(s.medicines.filter { !it.isDemo }.map {
            JSONObject().put("id", it.id).put("name", it.name).put("strength", it.strength).put("dosage", it.dosage).put("form", it.form).put("frequency", it.frequency)
                .put("times", it.times).put("startDate", it.startDate).n("endDate", it.endDate).n("expiryDate", it.expiryDate).put("instructions", it.instructions)
                .put("foodRelation", it.foodRelation).put("prescribedBy", it.prescribedBy).put("remindersOn", it.remindersOn).put("active", it.active)
        }))
        root.put("doseLogs", JSONArray(s.doseLogs.filter { !it.isDemo && it.medicineId in realMedIds }.map { JSONObject().put("medicineId", it.medicineId).put("scheduledAt", it.scheduledAt).put("status", it.status).put("loggedAt", it.loggedAt) }))
        root.put("measurements", JSONArray(s.measurements.filter { !it.isDemo }.map {
            JSONObject().put("type", it.type).put("value", it.value).n("value2", it.value2).put("unit", it.unit).put("timestamp", it.timestamp).put("source", it.source).put("note", it.note)
        }))
        root.put("symptoms", JSONArray(s.symptoms.filter { !it.isDemo }.map { JSONObject().put("name", it.name).put("severity", it.severity).put("notes", it.notes).put("timestamp", it.timestamp) }))
        root.put("appointments", JSONArray(s.appointments.filter { !it.isDemo }.map { JSONObject().put("doctor", it.doctor).put("purpose", it.purpose).put("location", it.location).put("dateTime", it.dateTime).put("notes", it.notes) }))
        root.put("reports", JSONArray(s.reports.filter { !it.isDemo }.map { r ->
            JSONObject().put("title", r.title).put("labName", r.labName).put("reportDate", r.reportDate).put("rawText", r.rawText).put("sourceType", r.sourceType)
                .put("results", JSONArray(s.results.filter { it.reportId == r.id && r.id in realReportIds }.map {
                    JSONObject().put("testName", it.testName).n("value", it.value).put("valueText", it.valueText).put("unit", it.unit).n("refLow", it.refLow).n("refHigh", it.refHigh).put("refText", it.refText).put("flag", it.flag)
                }))
        }))
        out.write(FieldCrypto.encryptWithPassphrase(root.toString().toByteArray(Charsets.UTF_8), passphrase))
        out.flush()
    }

    /** Replaces all current data with the backup contents. */
    suspend fun restore(repo: HealthRepository, passphrase: CharArray, input: InputStream) {
        val json = String(FieldCrypto.decryptWithPassphrase(input.readBytes(), passphrase), Charsets.UTF_8)
        val root = JSONObject(json)
        require(root.optString("format") == "medilife-backup") { "Not a MediLife backup" }
        repo.clearAll()
        root.optJSONObject("profile")?.let { p ->
            repo.saveProfile(ProfileEntity(name = p.optString("name"), age = p.optIntOrNull("age"), gender = p.optString("gender"), heightCm = p.optDoubleOrNull("heightCm"),
                weightKg = p.optDoubleOrNull("weightKg"), bloodGroup = p.optString("bloodGroup"), doctorName = p.optString("doctorName"), doctorSpecialty = p.optString("doctorSpecialty"),
                doctorPhone = p.optString("doctorPhone"), clinic = p.optString("clinic"), emergencyName = p.optString("emergencyName"), emergencyRelation = p.optString("emergencyRelation"),
                emergencyPhone = p.optString("emergencyPhone")))
        }
        root.optJSONArray("records")?.objs { MedicalRecordEntity(type = it.getString("type"), title = it.getString("title"), details = it.optString("details"), date = it.optLongOrNull("date")) }?.forEach { repo.saveRecord(it) }
        root.optJSONArray("timeline")?.objs { TimelineEventEntity(date = it.getLong("date"), title = it.getString("title"), description = it.optString("description"), category = it.optString("category", "General")) }?.forEach { repo.saveTimeline(it) }
        root.optJSONArray("medicines")?.objs {
            MedicineEntity(id = it.getLong("id"), name = it.getString("name"), strength = it.optString("strength"), dosage = it.optString("dosage"), form = it.optString("form", "Tablet"),
                frequency = it.optString("frequency"), times = it.optString("times"), startDate = it.getLong("startDate"), endDate = it.optLongOrNull("endDate"), expiryDate = it.optLongOrNull("expiryDate"),
                instructions = it.optString("instructions"), foodRelation = it.optString("foodRelation"), prescribedBy = it.optString("prescribedBy"),
                remindersOn = it.optBoolean("remindersOn", true), active = it.optBoolean("active", true))
        }?.forEach { repo.saveMedicine(it) }
        root.optJSONArray("doseLogs")?.objs { DoseLogEntity(medicineId = it.getLong("medicineId"), scheduledAt = it.getLong("scheduledAt"), status = it.getString("status"), loggedAt = it.optLong("loggedAt")) }
            ?.forEach { repo.dao.insertDoseLog(it) }
        root.optJSONArray("measurements")?.objs {
            MeasurementEntity(type = it.getString("type"), value = it.getDouble("value"), value2 = it.optDoubleOrNull("value2"), unit = it.optString("unit"), timestamp = it.getLong("timestamp"), source = it.optString("source", "MANUAL"), note = it.optString("note"))
        }?.forEach { repo.saveMeasurement(it) }
        root.optJSONArray("symptoms")?.objs { SymptomEntity(name = it.getString("name"), severity = it.getInt("severity"), notes = it.optString("notes"), timestamp = it.getLong("timestamp")) }?.forEach { repo.saveSymptom(it) }
        root.optJSONArray("appointments")?.objs { AppointmentEntity(doctor = it.getString("doctor"), purpose = it.optString("purpose"), location = it.optString("location"), dateTime = it.getLong("dateTime"), notes = it.optString("notes")) }?.forEach { repo.saveAppointment(it) }
        root.optJSONArray("reports")?.objs { r ->
            val report = LabReportEntity(title = r.getString("title"), labName = r.optString("labName"), reportDate = r.getLong("reportDate"), rawText = r.optString("rawText"), sourceType = r.optString("sourceType"))
            val results = r.optJSONArray("results")?.objs {
                LabResultEntity(reportId = 0, testName = it.getString("testName"), value = it.optDoubleOrNull("value"), valueText = it.optString("valueText"), unit = it.optString("unit"),
                    refLow = it.optDoubleOrNull("refLow"), refHigh = it.optDoubleOrNull("refHigh"), refText = it.optString("refText"), flag = it.optString("flag"), reportDate = 0)
            } ?: emptyList()
            report to results
        }?.forEach { (r, res) -> repo.saveReport(r, res) }
    }
}

package com.medilife.ai.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth. Sensitive free-text fields are encrypted before they
 * reach SQLite and decrypted when read back.
 */
class HealthRepository(private val db: AppDatabase) {
    val dao = db.dao()
    private val c = FieldCrypto

    val profile: Flow<ProfileEntity?> = dao.profile()
    suspend fun saveProfile(p: ProfileEntity) = dao.upsertProfile(p.copy(id = 1))

    val records: Flow<List<MedicalRecordEntity>> = dao.records().map { l -> l.map(::decRecord) }
    suspend fun recordsOnce() = dao.recordsOnce().map(::decRecord)
    suspend fun saveRecord(r: MedicalRecordEntity) =
        dao.upsertRecord(r.copy(title = c.enc(r.title), details = c.enc(r.details)))
    suspend fun deleteRecord(r: MedicalRecordEntity) = dao.deleteRecord(r)
    private fun decRecord(r: MedicalRecordEntity) = r.copy(title = c.dec(r.title), details = c.dec(r.details))

    val timeline: Flow<List<TimelineEventEntity>> = dao.timeline().map { l -> l.map { it.copy(description = c.dec(it.description)) } }
    suspend fun saveTimeline(e: TimelineEventEntity) = dao.upsertTimeline(e.copy(description = c.enc(e.description)))
    suspend fun deleteTimeline(e: TimelineEventEntity) = dao.deleteTimeline(e)

    val medicines: Flow<List<MedicineEntity>> = dao.medicines().map { l -> l.map(::decMed) }
    suspend fun medicinesOnce() = dao.medicinesOnce().map(::decMed)
    suspend fun medicine(id: Long) = dao.medicine(id)?.let(::decMed)
    fun medicineFlow(id: Long) = dao.medicineFlow(id).map { it?.let(::decMed) }
    suspend fun saveMedicine(m: MedicineEntity): Long {
        val enc = m.copy(instructions = c.enc(m.instructions))
        val r = dao.upsertMedicine(enc)
        return if (m.id != 0L) m.id else r
    }
    suspend fun deleteMedicine(id: Long) { dao.deleteDoseLogsFor(id); dao.deleteMedicine(id) }
    private fun decMed(m: MedicineEntity) = m.copy(instructions = c.dec(m.instructions))

    fun doseLogsSince(from: Long) = dao.doseLogsSince(from)
    fun doseLogsFor(id: Long) = dao.doseLogsFor(id)
    suspend fun logDose(medId: Long, at: Long, status: String, demo: Boolean = false) =
        dao.insertDoseLog(DoseLogEntity(medicineId = medId, scheduledAt = at, status = status, isDemo = demo))
    suspend fun clearDose(medId: Long, at: Long) = dao.clearDoseLog(medId, at)

    val measurements = dao.measurements()
    suspend fun saveMeasurement(m: MeasurementEntity) = dao.upsertMeasurement(m)
    suspend fun deleteMeasurement(m: MeasurementEntity) = dao.deleteMeasurement(m)

    val symptoms: Flow<List<SymptomEntity>> = dao.symptoms().map { l -> l.map { it.copy(notes = c.dec(it.notes)) } }
    suspend fun saveSymptom(s: SymptomEntity) = dao.upsertSymptom(s.copy(notes = c.enc(s.notes)))
    suspend fun deleteSymptom(s: SymptomEntity) = dao.deleteSymptom(s)

    val appointments = dao.appointments()
    suspend fun saveAppointment(a: AppointmentEntity) = dao.upsertAppointment(a)
    suspend fun deleteAppointment(a: AppointmentEntity) = dao.deleteAppointment(a)

    val reports: Flow<List<LabReportEntity>> = dao.reports().map { l -> l.map { it.copy(rawText = c.dec(it.rawText)) } }
    fun report(id: Long) = dao.reportFlow(id).map { it?.copy(rawText = c.dec(it.rawText)) }
    fun resultsFor(id: Long) = dao.resultsFor(id)
    val allResults = dao.allResults()
    suspend fun saveReport(r: LabReportEntity, results: List<LabResultEntity>) =
        dao.insertReportWithResults(r.copy(rawText = c.enc(r.rawText)), results)
    suspend fun deleteReport(id: Long) = dao.deleteReport(id)

    val chat: Flow<List<ChatMessageEntity>> = dao.chat().map { l -> l.map { it.copy(text = c.dec(it.text)) } }
    suspend fun addChat(m: ChatMessageEntity) = dao.insertChat(m.copy(text = c.enc(m.text)))
    suspend fun clearChat() = dao.clearChat()

    val demoActive: Flow<Boolean> = dao.demoMedicineCount().map { it > 0 }
    suspend fun removeDemoData() = dao.deleteAllDemo()
    suspend fun loadDemoData() { dao.deleteAllDemo(); DemoData.insert(this) }

    fun clearAll() = db.clearAllTables()

    /** Snapshot used by the AI engine, tips engine, exports and safety checks. */
    suspend fun snapshot(): HealthSnapshot = HealthSnapshot(
        profile = dao.profileOnce(),
        records = recordsOnce(),
        medicines = medicinesOnce(),
        measurements = dao.measurementsOnce().sortedByDescending { it.timestamp },
        symptoms = dao.symptomsOnce().map { it.copy(notes = c.dec(it.notes)) }.sortedByDescending { it.timestamp },
        appointments = dao.appointmentsOnce().sortedBy { it.dateTime },
        reports = dao.reportsOnce().map { it.copy(rawText = c.dec(it.rawText)) }.sortedByDescending { it.reportDate },
        results = dao.resultsOnce(),
        timeline = dao.timelineOnce().map { it.copy(description = c.dec(it.description)) }.sortedByDescending { it.date },
        doseLogs = dao.doseLogsOnce()
    )
}

data class HealthSnapshot(
    val profile: ProfileEntity?,
    val records: List<MedicalRecordEntity>,
    val medicines: List<MedicineEntity>,
    val measurements: List<MeasurementEntity>,
    val symptoms: List<SymptomEntity>,
    val appointments: List<AppointmentEntity>,
    val reports: List<LabReportEntity>,
    val results: List<LabResultEntity>,
    val timeline: List<TimelineEventEntity>,
    val doseLogs: List<DoseLogEntity>
) {
    fun recordsOf(type: String) = records.filter { it.type == type }
    val conditions get() = recordsOf(RecordType.CONDITION).map { it.title }
    val allergies get() = recordsOf(RecordType.ALLERGY).map { it.title }
    val activeMedicines get() = medicines.filter { it.active }
    fun latest(type: String) = measurements.firstOrNull { it.type == type }
}

package com.medilife.ai.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    // Profile
    @Query("SELECT * FROM profile WHERE id = 1")
    fun profile(): Flow<ProfileEntity?>
    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun profileOnce(): ProfileEntity?
    @Upsert suspend fun upsertProfile(p: ProfileEntity)

    // Records
    @Query("SELECT * FROM medical_records ORDER BY type, id")
    fun records(): Flow<List<MedicalRecordEntity>>
    @Query("SELECT * FROM medical_records") suspend fun recordsOnce(): List<MedicalRecordEntity>
    @Upsert suspend fun upsertRecord(r: MedicalRecordEntity): Long
    @Delete suspend fun deleteRecord(r: MedicalRecordEntity)

    // Timeline
    @Query("SELECT * FROM timeline ORDER BY date DESC")
    fun timeline(): Flow<List<TimelineEventEntity>>
    @Query("SELECT * FROM timeline") suspend fun timelineOnce(): List<TimelineEventEntity>
    @Upsert suspend fun upsertTimeline(e: TimelineEventEntity): Long
    @Delete suspend fun deleteTimeline(e: TimelineEventEntity)

    // Medicines
    @Query("SELECT * FROM medicines ORDER BY active DESC, name")
    fun medicines(): Flow<List<MedicineEntity>>
    @Query("SELECT * FROM medicines") suspend fun medicinesOnce(): List<MedicineEntity>
    @Query("SELECT * FROM medicines WHERE id = :id") suspend fun medicine(id: Long): MedicineEntity?
    @Query("SELECT * FROM medicines WHERE id = :id") fun medicineFlow(id: Long): Flow<MedicineEntity?>
    @Upsert suspend fun upsertMedicine(m: MedicineEntity): Long
    @Query("DELETE FROM medicines WHERE id = :id") suspend fun deleteMedicine(id: Long)

    // Dose logs
    @Query("SELECT * FROM dose_logs WHERE scheduledAt >= :from ORDER BY scheduledAt")
    fun doseLogsSince(from: Long): Flow<List<DoseLogEntity>>
    @Query("SELECT * FROM dose_logs WHERE medicineId = :medId ORDER BY scheduledAt DESC LIMIT 60")
    fun doseLogsFor(medId: Long): Flow<List<DoseLogEntity>>
    @Query("SELECT * FROM dose_logs") suspend fun doseLogsOnce(): List<DoseLogEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDoseLog(l: DoseLogEntity)
    @Query("DELETE FROM dose_logs WHERE medicineId = :medId AND scheduledAt = :at")
    suspend fun clearDoseLog(medId: Long, at: Long)
    @Query("DELETE FROM dose_logs WHERE medicineId = :medId") suspend fun deleteDoseLogsFor(medId: Long)

    // Measurements
    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun measurements(): Flow<List<MeasurementEntity>>
    @Query("SELECT * FROM measurements") suspend fun measurementsOnce(): List<MeasurementEntity>
    @Upsert suspend fun upsertMeasurement(m: MeasurementEntity): Long
    @Delete suspend fun deleteMeasurement(m: MeasurementEntity)

    // Symptoms
    @Query("SELECT * FROM symptoms ORDER BY timestamp DESC")
    fun symptoms(): Flow<List<SymptomEntity>>
    @Query("SELECT * FROM symptoms") suspend fun symptomsOnce(): List<SymptomEntity>
    @Upsert suspend fun upsertSymptom(s: SymptomEntity): Long
    @Delete suspend fun deleteSymptom(s: SymptomEntity)

    // Appointments
    @Query("SELECT * FROM appointments ORDER BY dateTime")
    fun appointments(): Flow<List<AppointmentEntity>>
    @Query("SELECT * FROM appointments") suspend fun appointmentsOnce(): List<AppointmentEntity>
    @Upsert suspend fun upsertAppointment(a: AppointmentEntity): Long
    @Delete suspend fun deleteAppointment(a: AppointmentEntity)

    // Lab reports
    @Query("SELECT * FROM lab_reports ORDER BY reportDate DESC")
    fun reports(): Flow<List<LabReportEntity>>
    @Query("SELECT * FROM lab_reports") suspend fun reportsOnce(): List<LabReportEntity>
    @Query("SELECT * FROM lab_reports WHERE id = :id") fun reportFlow(id: Long): Flow<LabReportEntity?>
    @Insert suspend fun insertReport(r: LabReportEntity): Long
    @Query("DELETE FROM lab_reports WHERE id = :id") suspend fun deleteReport(id: Long)
    @Query("SELECT * FROM lab_results WHERE reportId = :reportId ORDER BY id")
    fun resultsFor(reportId: Long): Flow<List<LabResultEntity>>
    @Query("SELECT * FROM lab_results ORDER BY reportDate")
    fun allResults(): Flow<List<LabResultEntity>>
    @Query("SELECT * FROM lab_results") suspend fun resultsOnce(): List<LabResultEntity>
    @Insert suspend fun insertResults(r: List<LabResultEntity>)

    @Transaction
    suspend fun insertReportWithResults(report: LabReportEntity, results: List<LabResultEntity>): Long {
        val id = insertReport(report)
        insertResults(results.map { it.copy(reportId = id, reportDate = report.reportDate) })
        return id
    }

    // Chat
    @Query("SELECT * FROM chat_messages ORDER BY timestamp")
    fun chat(): Flow<List<ChatMessageEntity>>
    @Insert suspend fun insertChat(m: ChatMessageEntity): Long
    @Query("DELETE FROM chat_messages") suspend fun clearChat()

    // Demo data handling — demo rows are flagged and never mixed with user rows.
    @Query("DELETE FROM profile WHERE isDemo = 1") suspend fun delDemoProfile()
    @Query("DELETE FROM medical_records WHERE isDemo = 1") suspend fun delDemoRecords()
    @Query("DELETE FROM timeline WHERE isDemo = 1") suspend fun delDemoTimeline()
    @Query("DELETE FROM dose_logs WHERE isDemo = 1 OR medicineId IN (SELECT id FROM medicines WHERE isDemo = 1)") suspend fun delDemoDoseLogs()
    @Query("DELETE FROM medicines WHERE isDemo = 1") suspend fun delDemoMedicines()
    @Query("DELETE FROM measurements WHERE isDemo = 1") suspend fun delDemoMeasurements()
    @Query("DELETE FROM symptoms WHERE isDemo = 1") suspend fun delDemoSymptoms()
    @Query("DELETE FROM appointments WHERE isDemo = 1") suspend fun delDemoAppointments()
    @Query("DELETE FROM lab_reports WHERE isDemo = 1") suspend fun delDemoReports()
    @Query("SELECT COUNT(*) FROM medicines WHERE isDemo = 1") fun demoMedicineCount(): Flow<Int>

    @Transaction
    suspend fun deleteAllDemo() {
        delDemoDoseLogs(); delDemoMedicines(); delDemoProfile(); delDemoRecords(); delDemoTimeline()
        delDemoMeasurements(); delDemoSymptoms(); delDemoAppointments(); delDemoReports()
    }
}

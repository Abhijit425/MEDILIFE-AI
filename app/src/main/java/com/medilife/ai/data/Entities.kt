package com.medilife.ai.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val age: Int? = null,
    val gender: String = "",
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bloodGroup: String = "",
    val photoUri: String = "",
    val doctorName: String = "",
    val doctorSpecialty: String = "",
    val doctorPhone: String = "",
    val clinic: String = "",
    val emergencyName: String = "",
    val emergencyRelation: String = "",
    val emergencyPhone: String = "",
    val isDemo: Boolean = false
)

object RecordType {
    const val CONDITION = "CONDITION"
    const val ALLERGY = "ALLERGY"
    const val SURGERY = "SURGERY"
    const val FAMILY = "FAMILY"
    const val PAST_MEDICINE = "PAST_MEDICINE"
    val all = listOf(CONDITION, ALLERGY, SURGERY, FAMILY, PAST_MEDICINE)
    fun label(t: String) = when (t) {
        CONDITION -> "Medical conditions"
        ALLERGY -> "Allergies"
        SURGERY -> "Previous surgeries"
        FAMILY -> "Family history"
        PAST_MEDICINE -> "Previous medicines"
        else -> t
    }
}

/** title/details are stored encrypted (Android Keystore AES-GCM). */
@Entity(tableName = "medical_records")
data class MedicalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val details: String = "",
    val date: Long? = null,
    val isDemo: Boolean = false
)

@Entity(tableName = "timeline")
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val isDemo: Boolean = false
)

object FoodRelation {
    const val BEFORE = "Before food"
    const val AFTER = "After food"
    const val WITH = "With food"
    const val ANY = "No food instruction"
    val all = listOf(BEFORE, AFTER, WITH, ANY)
}

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val strength: String = "",
    val dosage: String = "",
    val form: String = "Tablet",
    val frequency: String = "",
    /** Comma separated HH:mm times, e.g. "08:00,20:00". Empty = as needed. */
    val times: String = "",
    val startDate: Long,
    val endDate: Long? = null,
    val expiryDate: Long? = null,
    val instructions: String = "",
    val foodRelation: String = FoodRelation.ANY,
    val prescribedBy: String = "",
    val remindersOn: Boolean = true,
    val active: Boolean = true,
    val isDemo: Boolean = false
)

object DoseStatus {
    const val TAKEN = "TAKEN"
    const val SKIPPED = "SKIPPED"
    const val PENDING = "PENDING"
}

@Entity(
    tableName = "dose_logs",
    indices = [Index(value = ["medicineId", "scheduledAt"], unique = true)]
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicineId: Long,
    val scheduledAt: Long,
    val status: String,
    val loggedAt: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false
)

object MeasureType {
    const val HEART_RATE = "HEART_RATE"
    const val BLOOD_PRESSURE = "BLOOD_PRESSURE"
    const val SPO2 = "SPO2"
    const val WEIGHT = "WEIGHT"
    const val STEPS = "STEPS"
    const val SLEEP = "SLEEP"
    const val GLUCOSE = "GLUCOSE"
    const val TEMPERATURE = "TEMPERATURE"
    val all = listOf(HEART_RATE, BLOOD_PRESSURE, SPO2, WEIGHT, STEPS, SLEEP, GLUCOSE, TEMPERATURE)
    fun label(t: String) = when (t) {
        HEART_RATE -> "Heart rate"
        BLOOD_PRESSURE -> "Blood pressure"
        SPO2 -> "Oxygen (SpO₂)"
        WEIGHT -> "Weight"
        STEPS -> "Daily activity"
        SLEEP -> "Sleep"
        GLUCOSE -> "Blood glucose"
        TEMPERATURE -> "Temperature"
        else -> t
    }
    fun unit(t: String) = when (t) {
        HEART_RATE -> "bpm"
        BLOOD_PRESSURE -> "mmHg"
        SPO2 -> "%"
        WEIGHT -> "kg"
        STEPS -> "steps"
        SLEEP -> "h"
        GLUCOSE -> "mg/dL"
        TEMPERATURE -> "°C"
        else -> ""
    }
}

object DataSource {
    const val MANUAL = "MANUAL"
    const val DEVICE = "DEVICE"
}

@Entity(tableName = "measurements", indices = [Index("type"), Index("timestamp")])
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val value: Double,
    val value2: Double? = null,
    val unit: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = DataSource.MANUAL,
    val note: String = "",
    val isDemo: Boolean = false
)

@Entity(tableName = "symptoms")
data class SymptomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val severity: Int,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val doctor: String,
    val purpose: String = "",
    val location: String = "",
    val dateTime: Long,
    val notes: String = "",
    val isDemo: Boolean = false
)

@Entity(tableName = "lab_reports")
data class LabReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val labName: String = "",
    val reportDate: Long,
    val rawText: String = "",
    val sourceType: String = "Manual",
    val isDemo: Boolean = false
)

object LabFlag {
    const val LOW = "LOW"
    const val HIGH = "HIGH"
    const val NORMAL = "IN RANGE"
    const val UNKNOWN = "NO RANGE"
}

@Entity(
    tableName = "lab_results",
    foreignKeys = [ForeignKey(
        entity = LabReportEntity::class,
        parentColumns = ["id"],
        childColumns = ["reportId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("reportId"), Index("testName")]
)
data class LabResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportId: Long,
    val testName: String,
    val value: Double? = null,
    val valueText: String = "",
    val unit: String = "",
    val refLow: Double? = null,
    val refHigh: Double? = null,
    val refText: String = "",
    val flag: String = LabFlag.UNKNOWN,
    val reportDate: Long,
    val isDemo: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    /** RULE = offline rule engine, LLM = on-device language model, SYSTEM = app notice. */
    val source: String = "RULE",
    val emergency: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

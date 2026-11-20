package com.medilife.ai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class, MedicalRecordEntity::class, TimelineEventEntity::class,
        MedicineEntity::class, DoseLogEntity::class, MeasurementEntity::class,
        SymptomEntity::class, AppointmentEntity::class, LabReportEntity::class,
        LabResultEntity::class, ChatMessageEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): HealthDao

    companion object {
        const val NAME = "medilife.db"
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}

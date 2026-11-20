package com.medilife.ai.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.medilife.ai.AppContainer
import com.medilife.ai.data.AppSettings
import com.medilife.ai.data.AppointmentEntity
import com.medilife.ai.data.ChatMessageEntity
import com.medilife.ai.data.DoseLogEntity
import com.medilife.ai.data.DoseSchedule
import com.medilife.ai.data.HealthSnapshot
import com.medilife.ai.data.LabReportEntity
import com.medilife.ai.data.LabResultEntity
import com.medilife.ai.data.MeasurementEntity
import com.medilife.ai.data.MedicalRecordEntity
import com.medilife.ai.data.MedicineEntity
import com.medilife.ai.data.ProfileEntity
import com.medilife.ai.data.SymptomEntity
import com.medilife.ai.data.TimelineEventEntity
import com.medilife.ai.engine.AiSource
import com.medilife.ai.engine.LabParser
import com.medilife.ai.engine.ParsedLabRow
import com.medilife.ai.engine.SafetyAlert
import com.medilife.ai.engine.SafetyEngine
import com.medilife.ai.engine.WellnessScore
import com.medilife.ai.engine.WellnessScoreCalc
import com.medilife.ai.services.BackupManager
import com.medilife.ai.services.Exporters
import com.medilife.ai.services.OcrService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class OcrState(
    val running: Boolean = false,
    val rawText: String = "",
    val rows: List<ParsedLabRow> = emptyList(),
    val sourceType: String = "",
    val error: String? = null
)

@OptIn(FlowPreview::class)
class MediViewModel(private val c: AppContainer) : ViewModel() {
    private val repo = c.repo
    private fun <T> StateFlowOf(flow: kotlinx.coroutines.flow.Flow<T>, initial: T): StateFlow<T> =
        flow.stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    val settings: StateFlow<AppSettings?> = StateFlowOf(c.settings.settings.map { it }, null)
    val demoActive = StateFlowOf(repo.demoActive, false)
    val chat = StateFlowOf(repo.chat, emptyList())
    val reports = StateFlowOf(repo.reports, emptyList())

    @Suppress("UNCHECKED_CAST")
    val snapshot: StateFlow<HealthSnapshot?> = StateFlowOf(
        combine(
            repo.profile, repo.records, repo.medicines, repo.measurements, repo.symptoms,
            repo.appointments, repo.reports, repo.allResults, repo.timeline, repo.doseLogsSince(0)
        ) { a ->
            HealthSnapshot(
                profile = a[0] as ProfileEntity?,
                records = a[1] as List<MedicalRecordEntity>,
                medicines = a[2] as List<MedicineEntity>,
                measurements = a[3] as List<MeasurementEntity>,
                symptoms = a[4] as List<SymptomEntity>,
                appointments = a[5] as List<AppointmentEntity>,
                reports = a[6] as List<LabReportEntity>,
                results = a[7] as List<LabResultEntity>,
                timeline = a[8] as List<TimelineEventEntity>,
                doseLogs = a[9] as List<DoseLogEntity>
            )
        }, null
    )

    val alerts: StateFlow<List<SafetyAlert>> = StateFlowOf(
        snapshot.map { s -> s?.let { SafetyEngine.analyze(it.medicines, it.allergies, it.conditions) } ?: emptyList() }, emptyList()
    )
    val wellness: StateFlow<WellnessScore?> = StateFlowOf(snapshot.map { s -> s?.let { WellnessScoreCalc.compute(it) } }, null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()
    fun toast(s: String) { _messages.tryEmit(s) }

    val thinking = MutableStateFlow(false)
    val ocr = MutableStateFlow(OcrState())

    init {
        viewModelScope.launch {
            combine(repo.medicines, c.settings.settings) { meds, st -> meds to st.remindersEnabled }
                .debounce(600).distinctUntilChanged()
                .collect { (meds, on) -> c.reminders.rescheduleAll(meds, on) }
        }
        viewModelScope.launch {
            c.settings.settings.map { it.hydrationReminders }.distinctUntilChanged().collect { c.reminders.setHydration(it) }
        }
    }

    private fun io(block: suspend () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try { block() } catch (e: Exception) { _messages.tryEmit("Something went wrong: ${e.message ?: e.javaClass.simpleName}") }
    }

    // ---- Profile & history ----
    fun saveProfile(p: ProfileEntity) = io { repo.saveProfile(p.copy(isDemo = false)); toast("Profile saved") }
    fun saveRecord(r: MedicalRecordEntity) = io { repo.saveRecord(r) }
    fun deleteRecord(r: MedicalRecordEntity) = io { repo.deleteRecord(r) }
    fun saveTimeline(e: TimelineEventEntity) = io { repo.saveTimeline(e) }
    fun deleteTimeline(e: TimelineEventEntity) = io { repo.deleteTimeline(e) }

    // ---- Medicines ----
    fun saveMedicine(m: MedicineEntity) = io { repo.saveMedicine(m); toast("${m.name} saved") }
    fun deleteMedicine(id: Long) = io { repo.deleteMedicine(id); toast("Medicine removed") }
    fun setDose(medId: Long, at: Long, status: String?) = io {
        if (status == null) repo.clearDose(medId, at) else repo.logDose(medId, at, status)
    }
    fun doseLogsFor(id: Long) = repo.doseLogsFor(id)
    fun medicineFlow(id: Long) = repo.medicineFlow(id)

    // ---- Health ----
    fun saveMeasurement(m: MeasurementEntity) = io { repo.saveMeasurement(m); toast("Reading saved") }
    fun deleteMeasurement(m: MeasurementEntity) = io { repo.deleteMeasurement(m) }
    fun saveSymptom(s: SymptomEntity) = io { repo.saveSymptom(s); toast("Symptom logged") }
    fun deleteSymptom(s: SymptomEntity) = io { repo.deleteSymptom(s) }
    fun saveAppointment(a: AppointmentEntity) = io { repo.saveAppointment(a); toast("Appointment saved") }
    fun deleteAppointment(a: AppointmentEntity) = io { repo.deleteAppointment(a) }

    // ---- Reports ----
    fun report(id: Long) = repo.report(id)
    fun resultsFor(id: Long) = repo.resultsFor(id)
    fun deleteReport(id: Long) = io { repo.deleteReport(id) }

    fun runOcr(ctx: Context, uri: Uri, isPdf: Boolean) {
        ocr.value = OcrState(running = true, sourceType = if (isPdf) "PDF" else "Image")
        viewModelScope.launch {
            try {
                val text = if (isPdf) OcrService.fromPdfUri(ctx, uri) else OcrService.fromImageUri(ctx, uri)
                ocr.value = OcrState(rawText = text, rows = LabParser.parse(text), sourceType = if (isPdf) "PDF" else "Image")
            } catch (e: Exception) {
                ocr.value = OcrState(error = "Couldn't read this file: ${e.message}")
            }
        }
    }

    fun runOcrBitmap(bmp: android.graphics.Bitmap) {
        ocr.value = OcrState(running = true, sourceType = "Camera")
        viewModelScope.launch {
            try {
                val text = OcrService.fromBitmap(bmp)
                ocr.value = OcrState(rawText = text, rows = LabParser.parse(text), sourceType = "Camera")
            } catch (e: Exception) { ocr.value = OcrState(error = "Couldn't read the photo: ${e.message}") }
        }
    }

    fun resetOcr() { ocr.value = OcrState() }

    fun saveParsedReport(title: String, lab: String, date: Long, rows: List<ParsedLabRow>) = io {
        val st = ocr.value
        repo.saveReport(
            LabReportEntity(title = title.ifBlank { "Lab report" }, labName = lab, reportDate = date, rawText = st.rawText, sourceType = st.sourceType.ifBlank { "Manual" }),
            rows.map { LabParser.toEntity(it, date) }
        )
        ocr.value = OcrState()
        toast("Report saved")
    }

    // ---- Medi AI ----
    fun send(text: String) {
        if (text.isBlank() || thinking.value) return
        thinking.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.addChat(ChatMessageEntity(role = "USER", text = text))
                val snap = repo.snapshot()
                val r = c.ai.reply(text, snap, c.settings.current())
                repo.addChat(ChatMessageEntity(role = "ASSISTANT", text = r.text, source = r.source, emergency = r.emergency))
            } catch (e: Exception) {
                repo.addChat(ChatMessageEntity(role = "ASSISTANT", text = "Sorry — I couldn't process that (${e.message}).", source = AiSource.SYSTEM))
            } finally { thinking.value = false }
        }
    }
    fun clearChat() = io { repo.clearChat() }

    // ---- Settings & data ----
    fun setTheme(v: String) = io { c.settings.setTheme(v) }
    fun setBiometric(v: Boolean) = io { c.settings.setBiometric(v) }
    fun setReminders(v: Boolean) = io { c.settings.setReminders(v) }
    fun setHydration(v: Boolean) = io { c.settings.setHydration(v) }
    fun setReduceMotion(v: Boolean) = io { c.settings.setReduceMotion(v) }
    fun setEmergencyNumber(v: String) = io { c.settings.setEmergencyNumber(v) }
    fun setLlmEnabled(v: Boolean) = io {
        c.settings.setLlmEnabled(v)
        if (v) {
            val path = c.settings.current().llmModelPath
            if (!c.llm.isAvailable(path)) toast("No on-device model installed — Medi AI will keep using its offline rule engine.")
            else if (!c.llm.ensureLoaded(path)) toast("Model couldn't be loaded on this device: ${c.llm.lastError}. Using the rule engine.")
            else toast("On-device model loaded")
        } else c.llm.unload()
    }
    fun llmAvailable(path: String) = c.llm.isAvailable(path)

    fun importModel(ctx: Context, uri: Uri) = io {
        toast("Copying model… this can take a minute")
        val name = (uri.lastPathSegment ?: "model.task").substringAfterLast('/').substringAfterLast(':')
        val dest = File(c.llm.modelDir(), name.ifBlank { "model.task" })
        ctx.contentResolver.openInputStream(uri)?.use { input -> dest.outputStream().use { input.copyTo(it, 1 shl 20) } }
        c.settings.setLlmPath(dest.absolutePath)
        toast("Model installed (${dest.length() / 1_000_000} MB). Enable it to use on-device AI.")
    }

    fun removeModel() = io {
        c.llm.unload()
        val p = c.settings.current().llmModelPath
        if (p.isNotBlank()) File(p).delete()
        c.settings.setLlmPath(""); c.settings.setLlmEnabled(false)
        toast("Model removed")
    }

    fun completeOnboarding(loadDemo: Boolean) = io {
        if (loadDemo) repo.loadDemoData()
        c.settings.setOnboardingDone(true)
    }
    fun loadDemo() = io { repo.loadDemoData(); toast("Demo data loaded (clearly marked DEMO)") }
    fun removeDemo() = io { repo.removeDemoData(); toast("All demo data removed") }
    fun clearAll() = io {
        repo.clearAll()
        c.reminders.cancelAll()
        c.llm.unload()
        c.llm.modelDir().listFiles()?.forEach { it.delete() }
        c.settings.clear()
        toast("All MediLife data deleted from this device")
    }

    fun exportPdf(ctx: Context, uri: Uri, doctor: Boolean) = io {
        val s = repo.snapshot()
        ctx.contentResolver.openOutputStream(uri)?.use { Exporters.writePdf(s, it, if (doctor) "Doctor Visit Summary" else "Medical History") }
        toast("PDF exported")
    }

    suspend fun doctorSummary(): String = withContext(Dispatchers.IO) { Exporters.doctorSummaryText(repo.snapshot()) }

    fun backup(ctx: Context, uri: Uri, pass: String) = io {
        ctx.contentResolver.openOutputStream(uri)?.use { BackupManager.export(repo, pass.toCharArray(), it) }
        toast("Encrypted backup saved")
    }

    fun restore(ctx: Context, uri: Uri, pass: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            ctx.contentResolver.openInputStream(uri)?.use { BackupManager.restore(repo, pass.toCharArray(), it) }
            toast("Backup restored")
        } catch (e: javax.crypto.AEADBadTagException) {
            toast("Wrong passphrase or damaged backup file")
        } catch (e: Exception) {
            toast("Restore failed: ${e.message}")
        }
    }

    fun todayDoses(s: HealthSnapshot) = DoseSchedule.dosesFor(java.time.LocalDate.now(), s.medicines, s.doseLogs)

    companion object {
        fun factory(c: AppContainer): ViewModelProvider.Factory = viewModelFactory { initializer { MediViewModel(c) } }
    }
}

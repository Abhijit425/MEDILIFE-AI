package com.medilife.ai.services

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.medilife.ai.data.FoodRelation
import com.medilife.ai.data.HealthSnapshot
import com.medilife.ai.data.LabFlag
import com.medilife.ai.data.MeasureType
import com.medilife.ai.data.RecordType
import com.medilife.ai.engine.AlertLevel
import com.medilife.ai.engine.MediAiEngine
import com.medilife.ai.engine.SafetyEngine
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporters {
    private val df = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    private fun d(ms: Long) = df.format(Date(ms))

    /** Plain-text sections shared by the PDF export and the doctor summary. */
    fun sections(s: HealthSnapshot, forDoctor: Boolean): List<Pair<String, List<String>>> {
        val out = mutableListOf<Pair<String, List<String>>>()
        val p = s.profile
        out += "Patient" to listOfNotNull(
            p?.name?.ifBlank { null }?.let { "Name: $it" },
            p?.age?.let { "Age: $it" }, p?.gender?.ifBlank { null }?.let { "Gender: $it" },
            p?.bloodGroup?.ifBlank { null }?.let { "Blood group: $it" },
            p?.heightCm?.let { "Height: ${it.toInt()} cm" }, (s.latest(MeasureType.WEIGHT)?.value ?: p?.weightKg)?.let { "Weight: ${"%.1f".format(it)} kg" },
            p?.emergencyName?.ifBlank { null }?.let { "Emergency contact: $it (${p?.emergencyRelation.orEmpty()}) ${p?.emergencyPhone.orEmpty()}" },
            p?.doctorName?.ifBlank { null }?.let { "Doctor: $it ${p?.doctorSpecialty.orEmpty()} ${p?.doctorPhone.orEmpty()}".trim() }
        ).ifEmpty { listOf("No profile recorded") }
        RecordType.all.forEach { t ->
            val l = s.recordsOf(t)
            out += RecordType.label(t) to l.map { "${it.title}${if (it.details.isNotBlank()) " — ${it.details}" else ""}" }.ifEmpty { listOf("None recorded") }
        }
        out += "Current medicines" to s.activeMedicines.map {
            "${it.name} ${it.strength} — ${it.dosage.ifBlank { "dose not recorded" }}, ${it.frequency.ifBlank { it.times }}" +
                (if (it.foodRelation != FoodRelation.ANY) ", ${it.foodRelation.lowercase()}" else "") + (if (it.prescribedBy.isNotBlank()) " (by ${it.prescribedBy})" else "")
        }.ifEmpty { listOf("None recorded") }
        out += "Recent readings" to MeasureType.all.mapNotNull { s.latest(it) }.map {
            "${MeasureType.label(it.type)}: ${MediAiEngine.fmtMeasure(it.type, it.value, it.value2)} on ${d(it.timestamp)} (${if (it.source == "DEVICE") "device" else "manual entry"})"
        }.ifEmpty { listOf("None recorded") }
        out += "Lab results (latest report)" to (s.reports.firstOrNull()?.let { r ->
            listOf("${r.title} — ${d(r.reportDate)}") + s.results.filter { it.reportId == r.id }.map {
                "${it.testName}: ${it.valueText} ${it.unit} ${if (it.refText.isNotBlank()) "(range ${it.refText})" else ""} ${if (it.flag == LabFlag.HIGH || it.flag == LabFlag.LOW) "[${it.flag}]" else ""}".trim()
            }
        } ?: listOf("None recorded"))
        val sym = s.symptoms.filter { it.timestamp > System.currentTimeMillis() - 30L * 86_400_000 }
        out += "Symptoms (last 30 days)" to sym.groupBy { it.name }.map { (n, l) -> "$n — ${l.size}×, avg severity ${"%.1f".format(l.map { it.severity }.average())}/10" }.ifEmpty { listOf("None logged") }
        if (forDoctor) {
            out += "MediLife safety flags (not comprehensive)" to SafetyEngine.analyze(s.medicines, s.allergies, s.conditions)
                .filter { it.level != AlertLevel.INFO }.map { "${it.title}: ${it.why}" }.ifEmpty { listOf("None") }
        } else {
            out += "Medical timeline" to s.timeline.map { "${d(it.date)} — ${it.title}${if (it.description.isNotBlank()) ": ${it.description}" else ""}" }.ifEmpty { listOf("No events") }
        }
        return out
    }

    fun doctorSummaryText(s: HealthSnapshot): String = buildString {
        append("MEDILIFE AI – HEALTH SUMMARY FOR DOCTOR VISIT\nGenerated ${d(System.currentTimeMillis())} from patient-entered data.\n")
        sections(s, true).forEach { (h, l) -> append("\n$h\n"); l.forEach { append("• $it\n") } }
        append("\nNote: compiled by the patient in MediLife AI. Values are self-reported unless marked as device data.")
    }

    fun writePdf(s: HealthSnapshot, out: OutputStream, title: String = "Medical History") {
        val doc = PdfDocument()
        val w = 595; val h = 842; val margin = 40f
        val titleP = Paint().apply { textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(10, 60, 110); isAntiAlias = true }
        val headP = Paint().apply { textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.rgb(0, 120, 140); isAntiAlias = true }
        val bodyP = Paint().apply { textSize = 10.5f; color = Color.rgb(30, 30, 30); isAntiAlias = true }
        val smallP = Paint().apply { textSize = 8.5f; color = Color.GRAY; isAntiAlias = true }
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create())
            y = margin
            page!!.canvas.drawText("MediLife AI · $title · page $pageNo · confidential", margin, h - 20f, smallP)
        }
        fun line(text: String, p: Paint, gap: Float) {
            val maxW = w - margin * 2
            val words = text.split(" ")
            var cur = ""
            val wrapped = mutableListOf<String>()
            for (wd in words) {
                val t = if (cur.isEmpty()) wd else "$cur $wd"
                if (p.measureText(t) > maxW) { wrapped += cur; cur = wd } else cur = t
            }
            if (cur.isNotEmpty()) wrapped += cur
            for (l in wrapped) {
                if (y + gap > h - 40) newPage()
                y += gap
                page!!.canvas.drawText(l, margin, y, p)
            }
        }
        newPage()
        line("MediLife AI — $title", titleP, 24f)
        line("Exported ${d(System.currentTimeMillis())}. Contains sensitive health information — store securely.", smallP, 14f)
        sections(s, title.contains("Doctor")).forEach { (head, items) ->
            y += 8
            line(head, headP, 18f)
            items.forEach { line("•  $it", bodyP, 14f) }
        }
        page?.let { doc.finishPage(it) }
        doc.writeTo(out)
        doc.close()
    }
}

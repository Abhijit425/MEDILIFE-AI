package com.medilife.ai

import com.medilife.ai.data.DoseLogEntity
import com.medilife.ai.data.DoseSchedule
import com.medilife.ai.data.DoseStatus
import com.medilife.ai.data.LabFlag
import com.medilife.ai.data.MedicineEntity
import com.medilife.ai.engine.AlertLevel
import com.medilife.ai.engine.EmergencyDetector
import com.medilife.ai.engine.HealthTipsEngine
import com.medilife.ai.engine.LabGlossary
import com.medilife.ai.engine.LabParser
import com.medilife.ai.engine.MedicineKnowledgeBase
import com.medilife.ai.engine.SafetyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EnginesTest {
    private val now = System.currentTimeMillis()
    private fun med(id: Long, name: String, strength: String = "10 mg", dosage: String = "1 tablet", times: String = "09:00", expiry: Long? = null) =
        MedicineEntity(id = id, name = name, strength = strength, dosage = dosage, frequency = "Once daily", times = times, startDate = now - 10 * 86_400_000L, expiryDate = expiry)

    @Test fun knowledgeBaseFindsGenericsAndBrands() {
        assertEquals("paracetamol", MedicineKnowledgeBase.find("Dolo 650")?.key)
        assertEquals("pantoprazole", MedicineKnowledgeBase.find("Pan-D")?.key)
        assertEquals("metformin", MedicineKnowledgeBase.find("Glycomet SR")?.key)
        assertNull(MedicineKnowledgeBase.find("Unknownium"))
        assertTrue(MedicineKnowledgeBase.mentionedIn("can I take ibuprofen with aspirin?").map { it.key }.containsAll(listOf("ibuprofen", "aspirin")))
    }

    @Test fun detectsWarfarinNsaidInteraction() {
        val alerts = SafetyEngine.analyze(listOf(med(1, "Warfarin"), med(2, "Ibuprofen")), emptyList(), emptyList())
        assertTrue(alerts.any { it.category == "Interaction" && it.level == AlertLevel.HIGH })
    }

    @Test fun detectsDuplicateParacetamol() {
        val alerts = SafetyEngine.analyze(listOf(med(1, "Combiflam"), med(2, "Dolo 650")), emptyList(), emptyList())
        assertTrue(alerts.any { it.title.contains("Same active ingredient") })
    }

    @Test fun detectsPenicillinAllergyAndExpiry() {
        val alerts = SafetyEngine.analyze(listOf(med(1, "Amoxicillin"), med(2, "Cetirizine", expiry = now - 86_400_000)), listOf("Penicillin"), emptyList())
        assertTrue(alerts.any { it.category == "Allergy" && 1L in it.medicineIds })
        assertTrue(alerts.any { it.category == "Expired" && 2L in it.medicineIds })
    }

    @Test fun flagsMissingDoseAndUnknownMedicine() {
        val alerts = SafetyEngine.analyze(listOf(med(1, "Zzzmed", strength = "", dosage = "")), emptyList(), emptyList())
        assertTrue(alerts.any { it.category == "Incomplete" })
        assertTrue(alerts.any { it.category == "Unchecked" })
    }

    @Test fun nsaidConditionCaution() {
        val alerts = SafetyEngine.analyze(listOf(med(1, "Diclofenac")), emptyList(), listOf("Chronic kidney disease"))
        assertTrue(alerts.any { it.category == "Condition" })
    }

    @Test fun emergencyDetection() {
        assertNotNull(EmergencyDetector.detect("I have crushing chest pain and sweating"))
        assertNotNull(EmergencyDetector.detect("her face drooping and slurred speech"))
        assertNotNull(EmergencyDetector.detect("my throat is swelling after peanuts"))
        assertNull(EmergencyDetector.detect("what are the benefits of walking"))
        assertNull(EmergencyDetector.detect("explain my blood test report"))
    }

    @Test fun labParserReadsPrintedRangesOnly() {
        val text = """
            Patient Name: Test   Age: 45 Years
            HbA1c   7.1   %   4.0 - 5.6
            Hemoglobin   12.1 g/dL   13.0-17.0
            LDL Cholesterol   112 mg/dL   < 100
            TSH   2.1   uIU/mL
        """.trimIndent()
        val rows = LabParser.parse(text)
        val hba1c = rows.first { it.testName.contains("HbA1c", true) }
        assertEquals(7.1, hba1c.value!!, 0.001)
        assertEquals(LabFlag.HIGH, LabParser.flagFor(hba1c.value, hba1c.refLow, hba1c.refHigh))
        val hb = rows.first { it.testName.contains("Hemoglobin", true) }
        assertEquals(LabFlag.LOW, LabParser.flagFor(hb.value, hb.refLow, hb.refHigh))
        val ldl = rows.first { it.testName.contains("LDL", true) }
        assertEquals(100.0, ldl.refHigh!!, 0.001)
        val tsh = rows.first { it.testName.contains("TSH", true) }
        assertEquals(LabFlag.UNKNOWN, LabParser.flagFor(tsh.value, tsh.refLow, tsh.refHigh))
        assertFalse(rows.any { it.testName.contains("Age", true) })
        assertNotNull(LabGlossary.find("SGPT"))
    }

    @Test fun doseScheduleAndAdherence() {
        val m = med(1, "Metformin", times = "08:00,20:00")
        val today = LocalDate.now()
        val doses = DoseSchedule.dosesFor(today, listOf(m), emptyList())
        assertEquals(2, doses.size)
        val yAt = DoseSchedule.epoch(today.minusDays(1), LocalTime.of(8, 0))
        val logs = listOf(DoseLogEntity(medicineId = 1, scheduledAt = yAt, status = DoseStatus.TAKEN))
        val adh = DoseSchedule.weeklyAdherence(listOf(m), logs)
        assertNotNull(adh)
        assertTrue(adh!! in 1..100)
        assertFalse(DoseSchedule.isActiveOn(m.copy(active = false), today))
    }

    @Test fun tipsRespectRestrictions() {
        val kidney = HealthTipsEngine.eligible(listOf("Chronic kidney disease"), emptyList())
        assertFalse(kidney.any { it.title == "Drink regularly" || it.title == "Potassium-rich fruits" })
        val nutAllergy = HealthTipsEngine.eligible(emptyList(), listOf("Peanut"))
        assertFalse(nutAllergy.any { it.title == "A handful of nuts" })
        assertTrue(HealthTipsEngine.eligible(listOf("Type 2 diabetes"), emptyList()).any { it.personalized })
    }
}

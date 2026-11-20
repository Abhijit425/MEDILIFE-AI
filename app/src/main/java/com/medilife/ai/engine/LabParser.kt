package com.medilife.ai.engine

import com.medilife.ai.data.LabFlag
import com.medilife.ai.data.LabResultEntity

data class LabTestInfo(val key: String, val name: String, val aliases: List<String>, val meaning: String, val highNote: String, val lowNote: String)

object LabGlossary {
    val tests = listOf(
        LabTestInfo("hb", "Hemoglobin", listOf("hemoglobin", "haemoglobin", "hb", "hgb"), "The oxygen-carrying protein in red blood cells.", "Can be raised by dehydration, smoking or high altitude, among others.", "Low levels (anaemia) can come from iron or vitamin deficiency, blood loss or chronic illness."),
        LabTestInfo("wbc", "White blood cells", listOf("wbc", "total leucocyte count", "tlc", "white blood cell", "total wbc count", "leukocytes"), "Infection-fighting cells.", "Often seen with infection, inflammation or stress; occasionally other causes.", "Can occur with some viral infections, medicines or marrow problems."),
        LabTestInfo("rbc", "Red blood cells", listOf("rbc", "red blood cell", "rbc count", "total rbc"), "The number of red blood cells.", "May reflect dehydration or other causes.", "May reflect anaemia."),
        LabTestInfo("plt", "Platelets", listOf("platelet", "platelets", "platelet count", "plt"), "Cells that help blood clot.", "Can rise with inflammation, iron deficiency or other causes.", "Low counts raise bleeding risk; causes include viral infections (e.g. dengue) and medicines."),
        LabTestInfo("hct", "Hematocrit (PCV)", listOf("hematocrit", "haematocrit", "pcv", "hct", "packed cell volume"), "Proportion of blood made of red cells.", "May indicate dehydration.", "May indicate anaemia."),
        LabTestInfo("mcv", "MCV", listOf("mcv", "mean corpuscular volume"), "Average size of red blood cells.", "Large cells can relate to B12/folate deficiency or alcohol.", "Small cells often relate to iron deficiency or thalassaemia trait."),
        LabTestInfo("hba1c", "HbA1c", listOf("hba1c", "glycated hemoglobin", "glycosylated hemoglobin", "a1c", "glycated haemoglobin"), "Average blood sugar over roughly the past 2–3 months.", "Above-range values suggest blood sugar has been running high; doctors use it to diagnose and monitor diabetes.", "Rarely clinically important; can be affected by anaemia or blood loss."),
        LabTestInfo("fbs", "Fasting glucose", listOf("fasting glucose", "fasting blood sugar", "fbs", "glucose fasting", "fasting plasma glucose", "fpg", "blood sugar fasting"), "Blood sugar after at least 8 hours without food.", "Raised values can point to prediabetes or diabetes and need doctor review.", "Low values can cause shakiness, sweating, confusion."),
        LabTestInfo("ppbs", "Post-meal glucose", listOf("post prandial", "postprandial", "ppbs", "pp blood sugar", "glucose pp", "2 hr glucose"), "Blood sugar about 2 hours after a meal.", "Raised values suggest the body is not clearing sugar well after meals.", "Low values are uncommon."),
        LabTestInfo("rbs", "Random glucose", listOf("random blood sugar", "rbs", "random glucose", "glucose random"), "Blood sugar at any time of day.", "High values need interpretation with symptoms and other tests.", "Low values can cause symptoms of hypoglycaemia."),
        LabTestInfo("tc", "Total cholesterol", listOf("total cholesterol", "cholesterol total", "serum cholesterol", "cholesterol"), "All cholesterol in the blood.", "Higher levels add to heart and blood vessel risk.", "Rarely a concern."),
        LabTestInfo("ldl", "LDL cholesterol", listOf("ldl", "ldl cholesterol", "ldl-c", "low density lipoprotein"), "'Bad' cholesterol that can build up in arteries.", "Higher LDL increases cardiovascular risk; targets depend on your overall risk.", "Low LDL is generally not a concern."),
        LabTestInfo("hdl", "HDL cholesterol", listOf("hdl", "hdl cholesterol", "hdl-c", "high density lipoprotein"), "'Good' cholesterol that helps remove cholesterol.", "Higher HDL is generally favourable.", "Low HDL adds to cardiovascular risk; activity can help raise it."),
        LabTestInfo("tg", "Triglycerides", listOf("triglycerides", "triglyceride", "tg", "serum triglycerides"), "A type of blood fat.", "Raised by sugar, refined carbs, alcohol, and uncontrolled diabetes.", "Rarely a concern."),
        LabTestInfo("vldl", "VLDL cholesterol", listOf("vldl", "vldl cholesterol"), "A lipoprotein that carries triglycerides.", "Often rises with triglycerides.", "Rarely a concern."),
        LabTestInfo("creat", "Creatinine", listOf("creatinine", "serum creatinine", "s. creatinine", "s creatinine"), "A muscle waste product cleared by the kidneys.", "Higher values may suggest reduced kidney filtering; hydration, muscle mass and medicines also affect it.", "Often linked to low muscle mass."),
        LabTestInfo("urea", "Urea / BUN", listOf("urea", "blood urea", "bun", "blood urea nitrogen", "serum urea"), "A protein waste product cleared by the kidneys.", "Can rise with dehydration, high protein intake or kidney issues.", "Can be low with low protein intake or liver issues."),
        LabTestInfo("egfr", "eGFR", listOf("egfr", "estimated gfr", "gfr"), "Estimated kidney filtering rate, calculated from creatinine, age and sex.", "Higher values are generally fine.", "Lower values suggest reduced kidney function and are interpreted by doctors with other tests over time."),
        LabTestInfo("ua", "Uric acid", listOf("uric acid", "serum uric acid"), "A breakdown product of purines.", "High levels can lead to gout or kidney stones.", "Rarely a concern."),
        LabTestInfo("na", "Sodium", listOf("sodium", "na+", "serum sodium"), "Key salt controlling fluid balance.", "Often relates to dehydration.", "Can relate to medicines (e.g. diuretics), fluid excess or hormonal causes."),
        LabTestInfo("k", "Potassium", listOf("potassium", "k+", "serum potassium"), "Salt important for heart and muscle function.", "High potassium can affect heart rhythm and needs prompt medical review.", "Low potassium can cause weakness and rhythm issues."),
        LabTestInfo("cl", "Chloride", listOf("chloride", "cl-", "serum chloride"), "Salt involved in fluid and acid balance.", "Usually interpreted with sodium.", "Usually interpreted with sodium."),
        LabTestInfo("ca", "Calcium", listOf("calcium", "serum calcium", "total calcium"), "Mineral for bones, nerves and muscles.", "Can relate to parathyroid issues, supplements or other causes.", "Can relate to vitamin D deficiency or low albumin."),
        LabTestInfo("alt", "ALT (SGPT)", listOf("alt", "sgpt", "alanine aminotransferase", "alt (sgpt)", "sgpt (alt)"), "A liver enzyme.", "Raised values suggest liver cell irritation — causes include fatty liver, alcohol, medicines and hepatitis.", "Not usually a concern."),
        LabTestInfo("ast", "AST (SGOT)", listOf("ast", "sgot", "aspartate aminotransferase", "ast (sgot)", "sgot (ast)"), "An enzyme found in liver and muscle.", "Can rise with liver or muscle injury.", "Not usually a concern."),
        LabTestInfo("alp", "Alkaline phosphatase", listOf("alkaline phosphatase", "alp"), "An enzyme from liver and bone.", "Can rise with bile-duct or bone conditions; normally higher in growing children.", "Rarely a concern."),
        LabTestInfo("bil", "Bilirubin (total)", listOf("bilirubin total", "total bilirubin", "bilirubin", "serum bilirubin"), "A yellow pigment from red-cell breakdown processed by the liver.", "Raised values can cause jaundice; causes include liver, bile duct or blood conditions, or harmless Gilbert's syndrome.", "Not a concern."),
        LabTestInfo("alb", "Albumin", listOf("albumin", "serum albumin"), "Main blood protein made by the liver.", "Usually due to dehydration.", "Can relate to nutrition, liver or kidney conditions."),
        LabTestInfo("tp", "Total protein", listOf("total protein", "serum protein"), "All proteins in the blood.", "Interpreted with albumin and globulin.", "Interpreted with albumin and globulin."),
        LabTestInfo("tsh", "TSH", listOf("tsh", "thyroid stimulating hormone", "s.tsh", "ultrasensitive tsh"), "Pituitary hormone that controls the thyroid.", "High TSH usually suggests an underactive thyroid (or under-replacement).", "Low TSH usually suggests an overactive thyroid (or over-replacement)."),
        LabTestInfo("t3", "T3", listOf("t3", "total t3", "free t3", "ft3", "triiodothyronine"), "An active thyroid hormone.", "Interpreted with TSH.", "Interpreted with TSH."),
        LabTestInfo("t4", "T4", listOf("t4", "total t4", "free t4", "ft4", "thyroxine"), "The main thyroid hormone.", "Interpreted with TSH.", "Interpreted with TSH."),
        LabTestInfo("vitd", "Vitamin D (25-OH)", listOf("vitamin d", "25 oh vitamin d", "25-hydroxy vitamin d", "vit d", "25(oh)d", "vitamin d total"), "Vitamin D stores.", "Very high levels usually come from excess supplements.", "Low levels are common; doctors may advise sunlight, diet or supplements."),
        LabTestInfo("b12", "Vitamin B12", listOf("vitamin b12", "b12", "cobalamin", "vit b12"), "Vitamin needed for nerves and blood.", "Rarely a concern.", "Low levels can cause anaemia and nerve symptoms; long-term metformin can lower B12."),
        LabTestInfo("fe", "Serum iron", listOf("serum iron", "iron"), "Iron in the blood.", "Interpreted with ferritin and other iron tests.", "May suggest iron deficiency."),
        LabTestInfo("ferritin", "Ferritin", listOf("ferritin", "serum ferritin"), "The body's iron stores.", "Can rise with inflammation, liver disease or iron overload.", "Low ferritin is a strong sign of iron deficiency."),
        LabTestInfo("crp", "CRP", listOf("crp", "c-reactive protein", "c reactive protein", "hs-crp", "hscrp"), "A general marker of inflammation.", "Rises with infection or inflammation; not specific to one cause.", "Not a concern."),
        LabTestInfo("esr", "ESR", listOf("esr", "erythrocyte sedimentation rate"), "A general marker of inflammation.", "Non-specific; interpreted with symptoms and other tests.", "Not usually a concern.")
    )

    private val index = tests.flatMap { t -> t.aliases.map { it to t } }.sortedByDescending { it.first.length }

    fun find(name: String): LabTestInfo? {
        val n = name.lowercase().replace(Regex("[^a-z0-9+\\-() .]"), " ").replace(Regex("\\s+"), " ").trim()
        index.firstOrNull { it.first == n }?.let { return it.second }
        return index.firstOrNull { (k, _) -> Regex("(^|[^a-z0-9])" + Regex.escape(k) + "($|[^a-z0-9])").containsMatchIn(n) }?.second
    }

    fun mentionedIn(text: String): LabTestInfo? = find(text)
}

data class ParsedLabRow(
    val testName: String,
    val valueText: String,
    val value: Double?,
    val unit: String,
    val refLow: Double?,
    val refHigh: Double?,
    val refText: String,
    val known: Boolean
)

/**
 * Extracts test rows from OCR text. It deliberately only compares values with the
 * reference range printed on the report — it never invents a range.
 */
object LabParser {
    private val num = "(\\d{1,6}(?:[.,]\\d{1,4})?)"
    private val unitRx = "((?:[a-zA-Zµμ%]+[a-zA-Z0-9µμ/^.*%]*(?:/[a-zA-Z0-9.^µμ]+)?)|%)"
    private val rangeRx = Regex("$num\\s*(?:-|–|—|to)\\s*$num")
    private val ltRx = Regex("(?:<|≤|less than|upto|up to)\\s*$num", RegexOption.IGNORE_CASE)
    private val gtRx = Regex("(?:>|≥|more than|greater than)\\s*$num", RegexOption.IGNORE_CASE)
    private val rowRx = Regex("^\\s*([A-Za-z][A-Za-z0-9 ()\\-/.,']{1,60}?)\\s*[:\\-]?\\s+([<>]?\\s*$num)\\s*(?:$unitRx)?(.*)$")

    private fun d(s: String) = s.replace(",", ".").toDoubleOrNull()

    fun flagFor(v: Double?, lo: Double?, hi: Double?): String = when {
        v == null || (lo == null && hi == null) -> LabFlag.UNKNOWN
        lo != null && v < lo -> LabFlag.LOW
        hi != null && v > hi -> LabFlag.HIGH
        else -> LabFlag.NORMAL
    }

    fun parse(text: String): List<ParsedLabRow> {
        val rows = mutableListOf<ParsedLabRow>()
        for (raw in text.lines()) {
            val line = raw.replace('\t', ' ').trim()
            if (line.length < 4) continue
            val m = rowRx.find(line) ?: continue
            val name = m.groupValues[1].trim().trimEnd(':', '-', '.', ',')
            val lower = name.lowercase()
            if (lower.matches(Regex(".*\\b(age|date|page|phone|mobile|ref|id|no|sample|report|patient|years?|yrs|time)\\b.*"))) continue
            val valueText = m.groupValues[2].replace(" ", "")
            val value = d(valueText.trimStart('<', '>'))
            val unit = m.groupValues[4].trim()
            val rest = m.groupValues[5]
            var lo: Double? = null; var hi: Double? = null; var refText = ""
            rangeRx.find(rest)?.let { r -> lo = d(r.groupValues[1]); hi = d(r.groupValues[2]); refText = r.value.trim() }
                ?: ltRx.find(rest)?.let { r -> hi = d(r.groupValues[1]); refText = r.value.trim() }
                ?: gtRx.find(rest)?.let { r -> lo = d(r.groupValues[1]); refText = r.value.trim() }
            val known = LabGlossary.find(name) != null
            // Keep a row only when it looks like a real lab test: known name, or a unit plus a range.
            if (!known && (unit.isBlank() || refText.isBlank())) continue
            rows += ParsedLabRow(LabGlossary.find(name)?.name?.takeIf { known && name.length <= 4 } ?: name, valueText, value, unit, lo, hi, refText, known)
        }
        return rows.distinctBy { it.testName.lowercase() + it.valueText }
    }

    fun toEntity(r: ParsedLabRow, reportDate: Long) = LabResultEntity(
        reportId = 0, testName = r.testName, value = r.value, valueText = r.valueText, unit = r.unit,
        refLow = r.refLow, refHigh = r.refHigh, refText = r.refText, flag = flagFor(r.value, r.refLow, r.refHigh), reportDate = reportDate
    )

    fun explain(r: LabResultEntity): String {
        val info = LabGlossary.find(r.testName)
        val base = info?.meaning ?: "MediLife's offline glossary doesn't have an explanation for this test yet."
        val flagNote = when (r.flag) {
            LabFlag.HIGH -> "Above the range printed on your report. " + (info?.highNote ?: "")
            LabFlag.LOW -> "Below the range printed on your report. " + (info?.lowNote ?: "")
            LabFlag.NORMAL -> "Within the range printed on your report."
            else -> "No reference range was found on the report, so MediLife can't compare it. Check the printed range or ask your doctor."
        }
        return "$base\n$flagNote\nResults must be read with your age, sex, symptoms, medicines and history — only a clinician can interpret them as a diagnosis."
    }
}

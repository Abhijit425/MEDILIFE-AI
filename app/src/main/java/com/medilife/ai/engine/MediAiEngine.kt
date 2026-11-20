package com.medilife.ai.engine

import com.medilife.ai.data.AppSettings
import com.medilife.ai.data.DoseSchedule
import com.medilife.ai.data.FoodRelation
import com.medilife.ai.data.HealthSnapshot
import com.medilife.ai.data.LabFlag
import com.medilife.ai.data.MeasureType
import com.medilife.ai.data.RecordType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AiReply(val text: String, val source: String, val emergency: Boolean = false)

object AiSource {
    const val RULE = "RULE"
    const val LLM = "LLM"
    const val SYSTEM = "SYSTEM"
    fun label(s: String) = when (s) {
        LLM -> "On-device AI model"
        SYSTEM -> "MediLife"
        else -> "Offline rule-based engine"
    }
}

private data class OtcTopic(
    val key: String,
    val triggers: List<String>,
    val label: String,
    val warningSigns: List<String>,
    val optionsInfo: String,
    val selfCare: String,
    val seeDoctor: String,
    val usesNsaidOrParacetamol: Boolean = false
)

/**
 * Offline, rule-based health information assistant. Optionally hands open-ended
 * questions to a user-installed on-device model — and labels which one answered.
 */
class MediAiEngine(private val llm: LocalLlm) {
    private var pendingOtc: OtcTopic? = null
    private val df = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    private val dtf = SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault())
    private fun d(ms: Long) = df.format(Date(ms))

    private val otcTopics = listOf(
        OtcTopic("headache", listOf("headache", "head ache", "migraine", "head pain"), "headache",
            listOf("sudden, severe 'worst ever' headache", "fever with stiff neck, rash or confusion", "headache after a head injury", "weakness, numbness, vision or speech changes", "headache with pregnancy, or in someone on blood thinners"),
            "For ordinary tension-type headaches, pharmacists commonly discuss simple pain relievers such as paracetamol or an anti-inflammatory (NSAID) like ibuprofen. Which — if any — suits you depends on your conditions, allergies and current medicines.",
            "Drink water, eat regular meals, rest your eyes from screens, relax neck and shoulder muscles, and sleep on a regular schedule.",
            "See a doctor if headaches are frequent, getting worse, wake you from sleep, or you need painkillers on more than 2–3 days a week.", true),
        OtcTopic("fever", listOf("fever", "temperature", "feverish"), "fever",
            listOf("temperature of 39.4 °C (103 °F) or higher, or fever lasting more than 3 days", "stiff neck, rash that doesn't fade under pressure, or confusion", "difficulty breathing or chest pain", "severe dehydration (very little urine, dizziness)", "fever in a baby under 3 months"),
            "For fever, pharmacists commonly discuss paracetamol, and sometimes ibuprofen, to improve comfort. Fever itself is part of fighting infection.",
            "Rest, drink plenty of fluids, wear light clothing. In India, consider dengue/malaria testing for high fever during the monsoon — ask your doctor.",
            "See a doctor if fever lasts more than 2–3 days, keeps returning, or you have diabetes, kidney, heart or immune conditions.", true),
        OtcTopic("bodyache", listOf("body pain", "body ache", "back pain", "muscle pain", "joint pain", "sprain", "toothache", "tooth pain", "period pain", "cramps"), "pain",
            listOf("pain after a fall or injury with swelling or deformity", "back pain with leg weakness, numbness, or loss of bladder/bowel control", "chest, jaw or arm pain", "severe abdominal pain"),
            "For short-term muscle or joint pain, pharmacists commonly discuss paracetamol, an oral NSAID, or a topical (gel) NSAID, depending on your health.",
            "Gentle movement, warm or cold packs, and posture breaks often help.",
            "See a doctor if pain lasts more than a week, is severe, or keeps you from normal activities.", true),
        OtcTopic("acidity", listOf("acidity", "heartburn", "acid reflux", "gas", "indigestion", "gastric"), "acidity / heartburn",
            listOf("chest pain or pressure (could be the heart)", "trouble swallowing or food sticking", "vomiting blood or black stools", "unintended weight loss"),
            "For occasional heartburn, pharmacists commonly discuss antacids or acid-reducing medicines. Some of these affect absorption of other medicines.",
            "Smaller meals, avoiding late-night meals, raising the head of the bed and limiting fried/spicy food, alcohol and smoking can help.",
            "See a doctor if symptoms occur more than twice a week or persist despite these measures."),
        OtcTopic("allergy", listOf("sneezing", "runny nose", "hay fever", "itchy eyes", "allergic rhinitis", "hives", "itching"), "allergy symptoms",
            listOf("swelling of lips, tongue, face or throat", "wheezing or difficulty breathing", "dizziness or fainting after a trigger"),
            "For sneezing and itchy eyes, pharmacists commonly discuss non-drowsy antihistamines or saline nasal sprays.",
            "Rinse your nose with saline, keep windows closed on high-pollen days, wash bedding regularly.",
            "See a doctor if symptoms don't improve, affect sleep, or come with asthma symptoms."),
        OtcTopic("cold", listOf("cold", "cough", "sore throat", "blocked nose", "flu"), "cold / cough",
            listOf("difficulty breathing or chest pain", "cough with blood", "high fever beyond 3 days", "symptoms lasting more than 3 weeks"),
            "For colds, pharmacists may discuss saline sprays, lozenges or pain/fever relievers. Many combination cold remedies contain paracetamol or decongestants that aren't suitable for everyone (e.g. high blood pressure).",
            "Rest, warm fluids, honey in warm water (not for babies under 1), steam inhalation and salt-water gargles.",
            "See a doctor if you have asthma/COPD, symptoms worsen after improving, or you are breathless."),
        OtcTopic("diarrhea", listOf("diarrhea", "diarrhoea", "loose motion", "loose stools", "stomach upset", "vomiting"), "diarrhoea / stomach upset",
            listOf("blood in stool or black stools", "signs of dehydration (very little urine, dizziness, dry mouth)", "high fever", "severe abdominal pain", "lasting more than 2 days"),
            "Oral rehydration solution (ORS) is the most important first step. Pharmacists may discuss other options depending on the cause.",
            "Sip ORS frequently, eat light foods (rice, banana, curd if tolerated), and wash hands carefully.",
            "See a doctor promptly if you have diabetes, kidney disease, are elderly, or can't keep fluids down.")
    )

    suspend fun reply(message: String, s: HealthSnapshot, settings: AppSettings): AiReply {
        val msg = message.trim()
        val t = msg.lowercase()

        EmergencyDetector.detect(t)?.let { e ->
            pendingOtc = null
            return AiReply(
                "⚠️ ${e.title}\n\n${e.advice}\n\nEmergency number: ${settings.emergencyNumber}. Use the red SOS button on the Emergency screen to call.\n\nMedi AI cannot assess emergencies — please get help now.",
                AiSource.RULE, emergency = true
            )
        }

        pendingOtc?.let { topic ->
            val yes = Regex("^(yes|yeah|yep|y|haan|ha|some|one of them|i do|i have)\\b").containsMatchIn(t)
            val no = Regex("^(no|nope|nah|n|none|nahi|not really|none of them|i don'?t)\\b").containsMatchIn(t)
            if (yes || no) {
                pendingOtc = null
                return AiReply(if (yes) otcEscalate(topic) else otcEducate(topic, s), AiSource.RULE)
            }
            pendingOtc = null
        }

        // Safety-critical intents always use the deterministic rule engine.
        otcTopics.firstOrNull { topic -> isMedicineRequest(t) && topic.triggers.any { t.contains(it) } }?.let {
            pendingOtc = it
            return AiReply(otcAsk(it, s), AiSource.RULE)
        }
        if (isInteractionQuestion(t)) return AiReply(interaction(msg, s), AiSource.RULE)

        val rule = ruleAnswer(msg, t, s)

        if (settings.llmEnabled && rule.second && llm.ensureLoaded(settings.llmModelPath)) {
            val out = llm.generate(buildPrompt(msg, s, rule.first))
            if (out != null) return AiReply(
                out + "\n\n— Generated on your device by a local language model. It can be wrong; it is not medical advice.",
                AiSource.LLM
            )
        }
        return AiReply(rule.first, AiSource.RULE)
    }

    private fun isMedicineRequest(t: String) = Regex("(what|which|any|suggest|recommend|give me|can i take|should i take).{0,40}(medicine|medication|tablet|pill|drug|take)|medicine for|tablet for|pill for|something for").containsMatchIn(t)
    private fun isInteractionQuestion(t: String) =
        Regex("(together|with|and|plus|\\+).*").containsMatchIn(t) && Regex("(interact|together|can i take|safe to take|mix|combine|along with)").containsMatchIn(t) &&
            MedicineKnowledgeBase.mentionedIn(t).isNotEmpty()

    /** Returns (answer, isOpenEnded). Open-ended answers may be enriched by a local LLM. */
    private fun ruleAnswer(msg: String, t: String, s: HealthSnapshot): Pair<String, Boolean> {
        val mentionedMeds = MedicineKnowledgeBase.mentionedIn(t)
        return when {
            Regex("^(hi|hello|hey|namaste|good (morning|afternoon|evening))\\b").containsMatchIn(t) && t.length < 30 -> greet(s) to false
            Regex("(who are you|what are you|are you (an? )?(ai|real)|how do you work|which model)").containsMatchIn(t) -> about() to false
            t.contains("thank") && t.length < 40 -> "You're welcome. Take care — and I'm here whenever you have another question." to false
            Regex("(summar|my (medical )?history|explain my (medical )?history|health records|my records|my profile)").containsMatchIn(t) -> summary(s) to false
            Regex("(question|ask).{0,25}doctor").containsMatchIn(t) -> doctorQuestions(s) to true
            Regex("(precaution|careful|avoid).{0,40}(medicine|medication|tablet|drug)").containsMatchIn(t) || t.contains("precautions for my") -> precautions(s) to false
            Regex("side.?effect").containsMatchIn(t) -> sideEffects(mentionedMeds, s) to false
            mentionedMeds.isNotEmpty() -> mentionedMeds.joinToString("\n\n") { explainMedicine(it, s) } + "\n\n" + MedicineKnowledgeBase.DISCLAIMER to false
            Regex("(explain|tell me about|what is|what are) (my )?(medicine|medicines|medication|tablets)").containsMatchIn(t) || t.contains("my medicine") ->
                explainMyMedicines(s) to false
            Regex("\\b\\d{2,3}\\s*/\\s*\\d{2,3}\\b").containsMatchIn(t) || t.contains("blood pressure") || Regex("\\bbp\\b").containsMatchIn(t) -> bloodPressure(t, s) to false
            Regex("(sugar|glucose)").containsMatchIn(t) && !t.contains("hba1c") -> glucose(t, s) to false
            Regex("(heart rate|pulse)").containsMatchIn(t) -> heartRate(s) to false
            Regex("(oxygen|spo2|saturation)").containsMatchIn(t) -> spo2(s) to false
            Regex("(what (is|does)|meaning of|explain|mean)").containsMatchIn(t) && LabGlossary.mentionedIn(t) != null -> labTerm(LabGlossary.mentionedIn(t)!!, s) to false
            Regex("(blood test|lab|report|test result)").containsMatchIn(t) -> latestReport(s) to false
            t.contains("routine") || t.contains("daily plan") || t.contains("schedule for my day") -> routine(s) to true
            Regex("(symptom|journal|trend)").containsMatchIn(t) -> symptomTrends(s) to true
            Regex("\\b(tip|advice|healthy|wellness|diet|food|eat|exercise|sleep|water|hydrat)").containsMatchIn(t) -> tips(t, s) to true
            otcTopics.any { topic -> topic.triggers.any { t.contains(it) } } -> {
                val topic = otcTopics.first { topic -> topic.triggers.any { t.contains(it) } }
                symptomEducation(topic) to true
            }
            else -> fallback() to true
        }
    }

    // ---------------- Handlers ----------------

    private fun greet(s: HealthSnapshot): String {
        val name = s.profile?.name?.takeIf { it.isNotBlank() }?.substringBefore(" ") ?: "there"
        return "Hello $name! I'm Medi AI. I can explain your medicines, records, readings and lab reports in simple language, suggest questions for your doctor, and share wellness tips. What would you like to know?"
    }

    private fun about() = "I'm Medi AI, MediLife's health information assistant. By default I run entirely offline on a rule-based engine with a curated medical library — no internet and no cloud AI. If you install a compatible on-device language model in Settings, some open-ended answers can be generated locally, and each reply is labelled with which engine produced it. I don't diagnose, prescribe or change doses."

    private fun fallback() = "I can help with things like:\n• \"Explain my medical history\"\n• \"What are the precautions for my current medicines?\"\n• \"Explain metformin in simple language\"\n• \"Can I take ibuprofen with aspirin?\"\n• \"What does 135/88 mean?\"\n• \"Explain my blood test report\" or \"What is HbA1c?\"\n• \"Give me a healthy daily routine\"\n• \"What questions should I ask my doctor?\"\n\nIf you have a new or worrying symptom, please contact a doctor."

    private fun summary(s: HealthSnapshot): String {
        val p = s.profile
        if (p == null && s.records.isEmpty() && s.medicines.isEmpty()) return "Your health record is empty so far. Add your profile, conditions, allergies and medicines in the Profile tab and I'll summarise them here."
        return buildString {
            append("Here's a summary of your health records:\n")
            if (p != null) {
                append("\n👤 ${p.name.ifBlank { "Profile" }}")
                listOfNotNull(p.age?.let { "$it years" }, p.gender.ifBlank { null }, p.bloodGroup.ifBlank { null }?.let { "Blood group $it" }).takeIf { it.isNotEmpty() }?.let { append(" — ${it.joinToString(", ")}") }
                bmi(s)?.let { append("\nBMI ${"%.1f".format(it)} (${bmiCategory(it)}, from recorded height and weight)") }
            }
            RecordType.all.forEach { type ->
                val l = s.recordsOf(type)
                if (l.isNotEmpty()) append("\n\n${RecordType.label(type)}:\n" + l.joinToString("\n") { "• ${it.title}" + (if (it.details.isNotBlank()) " — ${it.details}" else "") })
            }
            val meds = s.activeMedicines
            if (meds.isNotEmpty()) append("\n\nCurrent medicines:\n" + meds.joinToString("\n") { "• ${it.name} ${it.strength} — ${it.dosage.ifBlank { "dose not recorded" }}, ${it.frequency.ifBlank { "frequency not recorded" }}" })
            val readings = listOf(MeasureType.BLOOD_PRESSURE, MeasureType.HEART_RATE, MeasureType.GLUCOSE, MeasureType.WEIGHT).mapNotNull { s.latest(it) }
            if (readings.isNotEmpty()) append("\n\nLatest readings:\n" + readings.joinToString("\n") { "• ${MeasureType.label(it.type)}: ${fmtMeasure(it.type, it.value, it.value2)} (${d(it.timestamp)}, ${if (it.source == "DEVICE") "device" else "entered manually"})" })
            s.reports.firstOrNull()?.let { r ->
                val out = s.results.filter { it.reportId == r.id && (it.flag == LabFlag.HIGH || it.flag == LabFlag.LOW) }
                append("\n\nLatest lab report: ${r.title} (${d(r.reportDate)})")
                if (out.isNotEmpty()) append(" — outside printed range: " + out.joinToString { "${it.testName} ${it.valueText} ${it.unit} (${it.flag.lowercase()})" })
            }
            s.appointments.firstOrNull { it.dateTime > System.currentTimeMillis() }?.let { append("\n\nNext appointment: ${it.doctor} on ${dtf.format(Date(it.dateTime))}") }
            append("\n\nThis summary is built from what you've entered. You can share it with your doctor from Appointments → Doctor summary.")
        }
    }

    private fun bmi(s: HealthSnapshot): Double? {
        val p = s.profile ?: return null
        val h = p.heightCm ?: return null
        val w = s.latest(MeasureType.WEIGHT)?.value ?: p.weightKg ?: return null
        return if (h > 50) w / ((h / 100) * (h / 100)) else null
    }

    private fun bmiCategory(b: Double) = when {
        b < 18.5 -> "below the usual adult range"
        b < 25 -> "within the usual adult range"
        b < 30 -> "above the usual range — overweight category"
        else -> "obesity category"
    }

    private fun explainMedicine(info: MedicineInfo, s: HealthSnapshot): String {
        val mine = s.medicines.firstOrNull { MedicineKnowledgeBase.find(it.name)?.key == info.key }
        return buildString {
            append("💊 ${info.displayName}\nType: ${info.drugClass}${if (info.prescriptionOnly) " (prescription medicine)" else ""}")
            append("\n\nUsed for: ${info.uses}")
            append("\nHow it works: ${info.howItWorks}")
            append("\nCommon side effects: ${info.sideEffects.joinToString("; ")}")
            append("\nGood to know: ${info.precautions.joinToString(" ")}")
            if (mine != null) {
                append("\n\nYour prescription: ${mine.name} ${mine.strength}, ${mine.dosage.ifBlank { "dose not recorded" }}, ${mine.frequency.ifBlank { "frequency not recorded" }}")
                if (mine.foodRelation != FoodRelation.ANY) append(", ${mine.foodRelation.lowercase()}")
                if (mine.instructions.isNotBlank()) append(". Doctor's note: ${mine.instructions}")
                append(". Always follow your doctor's instructions for your dose.")
            }
        }
    }

    private fun explainMyMedicines(s: HealthSnapshot): String {
        val meds = s.activeMedicines
        if (meds.isEmpty()) return "You haven't added any current medicines yet. Add them in the Medicines tab and I can explain each one in simple language."
        return meds.joinToString("\n\n") { m ->
            MedicineKnowledgeBase.find(m.name)?.let { "• ${m.name}: ${it.uses} ${it.howItWorks}" }
                ?: "• ${m.name}: not in MediLife's offline library — your pharmacist can explain it."
        } + "\n\nAsk me about any one of them for more detail. " + MedicineKnowledgeBase.DISCLAIMER
    }

    private fun precautions(s: HealthSnapshot): String {
        val meds = s.activeMedicines
        if (meds.isEmpty()) return "You don't have any active medicines recorded. Add them in the Medicines tab to see precautions."
        val alerts = SafetyEngine.analyze(s.medicines, s.allergies, s.conditions).filter { it.level != AlertLevel.INFO }
        return buildString {
            append("Precautions for your current medicines:\n")
            meds.forEach { m ->
                val info = MedicineKnowledgeBase.find(m.name)
                append("\n• ${m.name}")
                if (m.foodRelation != FoodRelation.ANY) append(" — take ${m.foodRelation.lowercase()} as prescribed")
                append(": ")
                append(info?.precautions?.take(2)?.joinToString(" ") ?: "not in the offline library; ask your pharmacist.")
            }
            if (alerts.isNotEmpty()) append("\n\nSafety alerts found:\n" + alerts.joinToString("\n") { "⚠️ ${it.title} — ${it.why}" })
            append("\n\nDon't stop or change any prescribed medicine without talking to your doctor. ${SafetyEngine.NOT_COMPREHENSIVE}")
        }
    }

    private fun sideEffects(meds: List<MedicineInfo>, s: HealthSnapshot): String {
        val list = meds.ifEmpty { s.activeMedicines.mapNotNull { MedicineKnowledgeBase.find(it.name) } }
        if (list.isEmpty()) return "Tell me which medicine you mean (for example, \"side effects of amlodipine\")."
        return list.joinToString("\n\n") { "${it.displayName} — common side effects: ${it.sideEffects.joinToString("; ")}." } +
            "\n\nThis isn't a complete list. Contact your doctor if a side effect is severe or worrying, and seek emergency help for swelling of the face or throat, or difficulty breathing. Don't stop a prescribed medicine on your own."
    }

    private fun interaction(msg: String, s: HealthSnapshot): String {
        val mentioned = MedicineKnowledgeBase.mentionedIn(msg)
        val pairs = mutableListOf<Pair<MedicineInfo, MedicineInfo>>()
        if (mentioned.size >= 2) {
            for (i in mentioned.indices) for (j in i + 1 until mentioned.size) pairs += mentioned[i] to mentioned[j]
        } else if (mentioned.size == 1) {
            s.activeMedicines.mapNotNull { MedicineKnowledgeBase.find(it.name) }.filter { it.key != mentioned[0].key }.forEach { pairs += mentioned[0] to it }
        }
        if (pairs.isEmpty()) return "Tell me the two medicines you want to check, e.g. \"Can I take ibuprofen with aspirin?\". ${SafetyEngine.NOT_COMPREHENSIVE}"
        val sb = StringBuilder("Interaction check (offline library):\n")
        var any = false
        pairs.forEach { (a, b) ->
            val hits = SafetyEngine.checkPair(a, b)
            if (hits.isEmpty()) sb.append("\n• ${a.displayName} + ${b.displayName}: no interaction found in MediLife's small offline dataset.")
            else { any = true; hits.forEach { (lvl, tw) -> sb.append("\n⚠️ ${lvl.label}: ${a.displayName} + ${b.displayName} — ${tw.first}. ${tw.second}") } }
        }
        val allergyHits = mentioned.filter { info -> s.allergies.any { a -> val al = a.lowercase(); info.ingredients.any { al.contains(it) } || MedicineKnowledgeBase.allergyGroups.any { (kw, tags) -> al.contains(kw) && info.tags.any { it in tags } } } }
        allergyHits.forEach { any = true; sb.append("\n⚠️ Allergy: ${it.displayName} matches an allergy in your profile.") }
        sb.append(if (any) "\n\nPlease ask your pharmacist or doctor before taking these together. Don't stop a prescribed medicine on your own." else "\n\nNo alert does not mean it's safe for you.")
        sb.append("\n\n${SafetyEngine.NOT_COMPREHENSIVE}")
        return sb.toString()
    }

    private fun bpCategory(sys: Int, dia: Int): String = when {
        sys > 180 || dia > 120 -> "very high (a 'hypertensive crisis' range). If you also have chest pain, breathlessness, weakness, vision or speech changes, call emergency services. Otherwise, re-check after 5 minutes of rest and contact your doctor promptly."
        sys >= 140 || dia >= 90 -> "in the 'stage 2 high blood pressure' range of the ACC/AHA categories."
        sys >= 130 || dia >= 80 -> "in the 'stage 1 high blood pressure' range of the ACC/AHA categories."
        sys >= 120 -> "in the 'elevated' range of the ACC/AHA categories."
        sys < 90 || dia < 60 -> "on the low side. Low readings matter mainly if you feel dizzy, faint or unwell — tell your doctor if so."
        else -> "in the 'normal' range of the ACC/AHA categories."
    }

    private fun bloodPressure(t: String, s: HealthSnapshot): String {
        val m = Regex("\\b(\\d{2,3})\\s*/\\s*(\\d{2,3})\\b").find(t)
        val (sys, dia, from) = if (m != null) Triple(m.groupValues[1].toInt(), m.groupValues[2].toInt(), "The reading you gave")
        else s.latest(MeasureType.BLOOD_PRESSURE)?.let { Triple(it.value.toInt(), (it.value2 ?: 0.0).toInt(), "Your latest logged reading (${d(it.timestamp)})") }
            ?: return "I don't see a blood pressure reading. Type it like \"135/85\", or log one from the Health tab."
        val recent = s.measurements.filter { it.type == MeasureType.BLOOD_PRESSURE }.take(7)
        val trend = if (recent.size >= 3) "\n\nYour last ${recent.size} logged readings average ${recent.map { it.value }.average().toInt()}/${recent.mapNotNull { it.value2 }.average().toInt()} mmHg." else ""
        return "$from, $sys/$dia mmHg, is ${bpCategory(sys, dia)}\n\nThe top number (systolic) is the pressure when your heart beats; the bottom (diastolic) is the pressure between beats. One reading isn't a diagnosis — BP varies with activity, stress, caffeine and technique. Doctors look at repeated readings taken seated after 5 minutes' rest.$trend\n\nYour personal target is set by your doctor."
    }

    private fun glucose(t: String, s: HealthSnapshot): String {
        val n = Regex("\\b(\\d{2,3})\\b").find(t)?.groupValues?.get(1)?.toIntOrNull()
        val latest = s.latest(MeasureType.GLUCOSE)
        val v = n ?: latest?.value?.toInt() ?: return "I don't see a glucose reading. Type it like \"my fasting sugar is 112\" or log one in the Health tab."
        val fasting = t.contains("fasting") || (n == null && latest?.note?.lowercase()?.contains("fast") == true)
        val cat = when {
            v < 70 -> "below 70 mg/dL, which is considered low blood sugar. If you feel shaky, sweaty or confused, follow the hypoglycaemia plan your doctor gave you and seek help if you don't improve."
            fasting && v < 100 -> "in the usual fasting range (70–99 mg/dL)."
            fasting && v < 126 -> "in the 100–125 mg/dL fasting band that is often called the 'prediabetes' range on repeated tests."
            fasting -> "126 mg/dL or more fasting. On repeated testing this is in the diabetes range; if you already have diabetes, your doctor sets your personal targets."
            v >= 300 -> "very high. If you feel unwell, very thirsty, are vomiting or drowsy, seek medical care urgently."
            v >= 200 -> "high for a non-fasting reading. Discuss repeated high readings with your doctor."
            else -> "a non-fasting value; these are interpreted against time since your last meal."
        }
        val diabetic = s.conditions.any { it.lowercase().contains("diabet") }
        return "A glucose of $v mg/dL is $cat" + (if (diabetic) "\n\nBecause diabetes is in your profile, your doctor's personal targets matter more than general ranges." else "") +
            "\n\nOne reading doesn't diagnose anything — HbA1c and repeated tests give the fuller picture."
    }

    private fun heartRate(s: HealthSnapshot): String {
        val hr = s.latest(MeasureType.HEART_RATE) ?: return "No heart rate logged yet. For most resting adults, 60–100 beats per minute is the usual range; fit people may be lower."
        val v = hr.value.toInt()
        val note = when { v < 50 -> "below the usual resting range — fine for some fit people, but tell a doctor if you feel dizzy or faint."; v <= 100 -> "within the usual resting adult range (60–100 bpm) or close to it."; else -> "above 100 bpm. If at rest and with chest pain, breathlessness or fainting, seek urgent care; otherwise mention it to your doctor." }
        return "Your latest heart rate was $v bpm (${d(hr.timestamp)}, ${if (hr.source == "DEVICE") "from a device" else "entered manually"}). That's $note Caffeine, stress, fever, activity and some medicines all affect pulse."
    }

    private fun spo2(s: HealthSnapshot): String {
        val o = s.latest(MeasureType.SPO2) ?: return "No oxygen saturation logged yet. For most healthy people, SpO₂ is 95–100%. Readings of 92% or less, or any reading with breathlessness, need medical advice."
        val v = o.value.toInt()
        return "Your latest SpO₂ was $v% (${d(o.timestamp)}). " + when { v >= 95 -> "That's in the usual range."; v >= 93 -> "That's slightly below the usual range — re-check with warm hands and stay still. If it stays low or you're breathless, contact a doctor."; else -> "That's low. If it stays low on re-check, or you have breathlessness or chest pain, seek medical care urgently." } +
            " Some conditions (e.g. COPD) have different targets set by a doctor."
    }

    private fun labTerm(info: LabTestInfo, s: HealthSnapshot): String {
        val mine = s.results.filter { LabGlossary.find(it.testName)?.key == info.key }.sortedByDescending { it.reportDate }
        return buildString {
            append("🧪 ${info.name}: ${info.meaning}\n\nHigher than range: ${info.highNote}\nLower than range: ${info.lowNote}")
            if (mine.isNotEmpty()) {
                append("\n\nYour results: " + mine.take(4).joinToString("; ") { "${it.valueText} ${it.unit} on ${d(it.reportDate)} (${it.flag.lowercase()})" })
                if (mine.size >= 2) {
                    val a = mine[1].value; val b = mine[0].value
                    if (a != null && b != null) append("\nChange since previous: ${if (b > a) "up" else if (b < a) "down" else "no change"} (${"%.2f".format(b - a)} ${mine[0].unit}).")
                }
            }
            append("\n\nReference ranges differ between labs, ages and sexes — always use the range printed on your report and your doctor's interpretation.")
        }
    }

    private fun latestReport(s: HealthSnapshot): String {
        val r = s.reports.firstOrNull() ?: return "You haven't added a lab report yet. Go to Health → Reports to import a PDF or photo; MediLife reads it on your device."
        val res = s.results.filter { it.reportId == r.id }
        if (res.isEmpty()) return "Your latest report \"${r.title}\" has no extracted results. Open it in Health → Reports to review the text."
        val out = res.filter { it.flag == LabFlag.HIGH || it.flag == LabFlag.LOW }
        val unknown = res.filter { it.flag == LabFlag.UNKNOWN }
        return buildString {
            append("Your latest report: ${r.title} (${d(r.reportDate)})\n")
            res.forEach { x ->
                val info = LabGlossary.find(x.testName)
                append("\n• ${x.testName}: ${x.valueText} ${x.unit} — ${x.flag.lowercase()}${if (x.refText.isNotBlank()) " (report range ${x.refText})" else ""}")
                if (info != null) append(". ${info.meaning}")
            }
            if (out.isNotEmpty()) append("\n\n${out.size} result(s) are outside the range printed on the report: ${out.joinToString { it.testName }}. Being outside a range is not a diagnosis by itself; your doctor will look at these with your symptoms, history and medicines.")
            else append("\n\nAll results with a printed range are within it.")
            if (unknown.isNotEmpty()) append("\n\nNo printed range found for: ${unknown.joinToString { it.testName }} — I haven't compared these.")
            append("\n\nQuestions you could ask your doctor: What do these results mean for me? Do any need repeating or further tests? Should anything change in my treatment plan?")
        }
    }

    private fun routine(s: HealthSnapshot): String {
        val c = s.conditions.joinToString(" ").lowercase()
        val doses = DoseSchedule.dosesFor(java.time.LocalDate.now(), s.medicines, s.doseLogs)
        return buildString {
            append("A balanced daily routine (general wellness, adjust with your doctor):\n")
            append("\n🌅 Morning: wake at a fixed time, glass of water")
            if (c.contains("kidney") || c.contains("heart failure")) append(" (within any fluid limit your doctor set)")
            append(", 10 min of daylight, light stretching.")
            if (c.contains("hypertension") || c.contains("blood pressure")) append(" Check BP seated after 5 minutes' rest a few times a week.")
            if (c.contains("diabet")) append(" Check fasting sugar if your doctor advised.")
            append("\n🍽 Meals: regular times; half-plate vegetables, a protein (dal, eggs, paneer, fish), whole grains.")
            if (c.contains("diabet")) append(" Keep meal timing consistent with your diabetes medicines; a 10–15 min walk after meals helps.")
            append("\n🚶 Activity: build towards ~30 min of moderate activity most days")
            if (c.contains("heart") || c.contains("coronary")) append(" — at the intensity your doctor approves")
            append(", plus breaks from sitting every hour.")
            append("\n🧘 Afternoon: 5 minutes of slow breathing or a short walk to reset.")
            append("\n🌙 Evening: light dinner 2–3 hours before bed, screens dimmed an hour before sleep, aim for 7–9 hours.")
            if (doses.isNotEmpty()) append("\n\n💊 Your medicine times today: " + doses.joinToString { "${it.medicine.name} at ${it.time}" })
            append("\n\nThis is general wellness information, not an individual medical plan.")
        }
    }

    private fun doctorQuestions(s: HealthSnapshot): String {
        val q = mutableListOf(
            "Are my current medicines still the right ones and the right doses for me?",
            "What side effects should I watch for, and when should I call you?",
            "Which tests do I need next, and when?"
        )
        s.conditions.forEach { q += "How well controlled is my ${it.lowercase()}, and what targets should I aim for?" }
        if (s.allergies.isNotEmpty()) q += "Please note my allergies: ${s.allergies.joinToString()}."
        s.reports.firstOrNull()?.let { r ->
            s.results.filter { it.reportId == r.id && (it.flag == LabFlag.HIGH || it.flag == LabFlag.LOW) }.forEach { q += "My ${it.testName} was ${it.valueText} ${it.unit} (${it.flag.lowercase()}). What does that mean for me?" }
        }
        val symptoms = s.symptoms.filter { it.timestamp > System.currentTimeMillis() - 30L * 86_400_000 }.groupBy { it.name }
        symptoms.filter { it.value.size >= 2 }.forEach { (n, l) -> q += "I've logged $n ${l.size} times this month — should we look into it?" }
        SafetyEngine.analyze(s.medicines, s.allergies, s.conditions).filter { it.level == AlertLevel.HIGH }.forEach { q += "MediLife flagged: ${it.title}. Is this a concern for me?" }
        return "Questions to take to your doctor:\n\n" + q.distinct().mapIndexed { i, x -> "${i + 1}. $x" }.joinToString("\n") + "\n\nTip: Appointments → Doctor summary creates a one-page summary you can share."
    }

    private fun symptomTrends(s: HealthSnapshot): String {
        val recent = s.symptoms.filter { it.timestamp > System.currentTimeMillis() - 30L * 86_400_000 }
        if (recent.isEmpty()) return "No symptoms logged in the last 30 days. Use Health → Symptom journal to log them; patterns help your doctor."
        return "Symptoms in the last 30 days:\n" + recent.groupBy { it.name }.entries.sortedByDescending { it.value.size }.joinToString("\n") { (n, l) ->
            "• $n — ${l.size} time(s), average severity ${"%.1f".format(l.map { it.severity }.average())}/10, last on ${d(l.maxOf { it.timestamp })}"
        } + "\n\nIf a symptom is new, getting worse or frequent, please discuss it with a doctor."
    }

    private fun tips(t: String, s: HealthSnapshot): String {
        val eligible = HealthTipsEngine.eligible(s.conditions, s.allergies)
        val cat = HealthTipsEngine.categories.firstOrNull { c -> c.lowercase().split(" ").first().let { t.contains(it) } }
            ?: when { t.contains("food") || t.contains("diet") || t.contains("eat") -> "Nutrition"; t.contains("water") || t.contains("hydrat") -> "Hydration"; else -> null }
        val pick = (if (cat != null) eligible.filter { it.category == cat } else eligible.filter { it.personalized }.ifEmpty { eligible }).take(4)
        return "Wellness tips" + (cat?.let { " — $it" } ?: "") + ":\n\n" + pick.joinToString("\n\n") { "• ${it.title}: ${it.body}" } +
            "\n\nThese are general wellness suggestions filtered against the conditions and allergies in your profile — not individual medical advice."
    }

    private fun symptomEducation(topic: OtcTopic) = "About ${topic.label}:\n\nSelf-care: ${topic.selfCare}\n\nGet medical help urgently if you notice: ${topic.warningSigns.joinToString("; ")}.\n\n${topic.seeDoctor}\n\nThis is general information, not a diagnosis."

    // ---------------- OTC flow (5 steps) ----------------

    private fun otcAsk(topic: OtcTopic, s: HealthSnapshot): String = buildString {
        append("I can't prescribe or choose a medicine for you, but I can help you prepare to ask a pharmacist or doctor.\n\n")
        append("Step 1 — warning signs. Do you have any of these with your ${topic.label}?\n")
        topic.warningSigns.forEach { append("• $it\n") }
        append("\nStep 2 — what I checked in your records:\n")
        append(recordCheck(topic, s))
        append("\n\nReply \"yes\" if any warning sign applies, or \"no\" if none do.")
    }

    private fun recordCheck(topic: OtcTopic, s: HealthSnapshot): String {
        val lines = mutableListOf<String>()
        lines += "Allergies: " + (s.allergies.joinToString().ifBlank { "none recorded" })
        lines += "Conditions: " + (s.conditions.joinToString().ifBlank { "none recorded" })
        lines += "Current medicines: " + (s.activeMedicines.joinToString { it.name }.ifBlank { "none recorded" })
        if (topic.usesNsaidOrParacetamol) {
            val infos = s.activeMedicines.mapNotNull { MedicineKnowledgeBase.find(it.name) }
            val al = s.allergies.joinToString(" ").lowercase()
            if (listOf("nsaid", "aspirin", "ibuprofen", "diclofenac").any { al.contains(it) }) lines += "⚠️ You have an allergy recorded that relates to NSAIDs/aspirin — tell the pharmacist."
            val cond = s.conditions.joinToString(" ").lowercase()
            listOf("kidney", "ulcer", "heart", "hypertension", "blood pressure", "asthma").filter { cond.contains(it) }.takeIf { it.isNotEmpty() }?.let {
                lines += "⚠️ Your conditions (${it.joinToString()}) can make NSAID painkillers less suitable — mention them."
            }
            if (cond.contains("liver")) lines += "⚠️ Liver disease is recorded — paracetamol limits may differ for you."
            if (infos.any { it.tags.any { t -> t in setOf("ANTICOAGULANT", "ANTIPLATELET", "SSRI", "CORTICOSTEROID") } })
                lines += "⚠️ You take a medicine that increases bleeding risk with NSAIDs — tell the pharmacist."
            if (infos.any { "paracetamol" in it.ingredients }) lines += "⚠️ You already take a paracetamol-containing product — avoid doubling up."
            if (infos.any { it.tags.any { t -> t in setOf("ARB", "ACE_INHIBITOR", "DIURETIC") } }) lines += "⚠️ NSAIDs can interact with your blood-pressure/diuretic medicine."
        }
        return lines.joinToString("\n") { "• $it" }
    }

    private fun otcEscalate(topic: OtcTopic) =
        "Thank you. Because one of those warning signs applies, your ${topic.label} needs professional assessment rather than self-treatment. Please contact a doctor today — or call emergency services if symptoms are severe, sudden or getting worse quickly.\n\nUse the Emergency screen for one-tap calling."

    private fun otcEducate(topic: OtcTopic, s: HealthSnapshot) = buildString {
        append("Thanks — no warning signs reported.\n\nStep 3 — general information: ${topic.optionsInfo}\n\n")
        append("Self-care that often helps: ${topic.selfCare}\n\n")
        append("Step 4 — when to get checked: ${topic.seeDoctor}\n\n")
        append("Step 5 — next step: please ask a pharmacist or your doctor which option, if any, is suitable for you. Show them this list:\n")
        append(recordCheck(topic, s))
        append("\n\nI don't suggest specific doses — follow the product label and your pharmacist's advice.")
    }

    // ---------------- Local LLM prompt ----------------

    private fun buildPrompt(question: String, s: HealthSnapshot, ruleNotes: String): String {
        val ctx = buildString {
            s.profile?.let { p -> append("Age: ${p.age ?: "unknown"}; sex: ${p.gender.ifBlank { "not given" }}. ") }
            append("Conditions: ${s.conditions.joinToString().ifBlank { "none recorded" }}. ")
            append("Allergies: ${s.allergies.joinToString().ifBlank { "none recorded" }}. ")
            append("Current medicines: ${s.activeMedicines.joinToString { "${it.name} ${it.strength}" }.ifBlank { "none" }}.")
        }
        return """<start_of_turn>user
You are Medi AI, a health information assistant running fully offline inside the MediLife app.
Rules: give general educational information only. Never diagnose. Never prescribe, recommend prescription medicines, or give or change doses. Never tell the user to stop a prescribed medicine. Suggest a doctor or pharmacist for personal decisions. If symptoms could be serious, advise urgent medical care. Answer in plain English in under 170 words.
User health context: $ctx
Notes from MediLife's verified offline library (prefer these facts): ${ruleNotes.take(1200)}
Question: $question<end_of_turn>
<start_of_turn>model
"""
    }

    companion object {
        fun fmtMeasure(type: String, v: Double, v2: Double?): String = when (type) {
            MeasureType.BLOOD_PRESSURE -> "${v.toInt()}/${(v2 ?: 0.0).toInt()} mmHg"
            MeasureType.STEPS -> "${v.toInt()} steps"
            MeasureType.HEART_RATE -> "${v.toInt()} bpm"
            MeasureType.SPO2 -> "${v.toInt()}%"
            MeasureType.GLUCOSE -> "${v.toInt()} mg/dL"
            MeasureType.SLEEP -> "${"%.1f".format(v)} h"
            MeasureType.WEIGHT -> "${"%.1f".format(v)} kg"
            MeasureType.TEMPERATURE -> "${"%.1f".format(v)} °C"
            else -> v.toString()
        }
    }
}

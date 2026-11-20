package com.medilife.ai.engine

import com.medilife.ai.data.MedicineEntity

enum class AlertLevel(val label: String, val rank: Int) {
    HIGH("High", 3), MODERATE("Moderate", 2), INFO("Info", 1)
}

data class SafetyAlert(
    val level: AlertLevel,
    val category: String,
    val title: String,
    val why: String,
    val action: String,
    val medicineIds: List<Long> = emptyList()
)

private data class InteractionRule(
    val a: Set<String>, val b: Set<String>,
    val level: AlertLevel, val title: String, val why: String
)

/**
 * Local, offline, rule-based medication safety checks over a curated dataset.
 * It cannot be comprehensive: anything it does not know about is reported as unchecked.
 */
object SafetyEngine {
    const val NOT_COMPREHENSIVE =
        "These checks use a small curated offline dataset and are NOT comprehensive. No alert does not mean a combination is safe. Always confirm with your pharmacist or doctor."

    private val rules = listOf(
        InteractionRule(setOf("ANTICOAGULANT"), setOf("NSAID", "NSAID_ASPIRIN"), AlertLevel.HIGH, "Blood thinner + NSAID/aspirin",
            "Both increase bleeding risk; together they can cause serious stomach or other bleeding."),
        InteractionRule(setOf("ANTICOAGULANT"), setOf("ANTIPLATELET"), AlertLevel.HIGH, "Anticoagulant + antiplatelet",
            "Combining clot-preventing medicines markedly raises bleeding risk. Sometimes this is intended by a cardiologist, but it needs close supervision."),
        InteractionRule(setOf("ANTIPLATELET", "NSAID_ASPIRIN"), setOf("NSAID"), AlertLevel.MODERATE, "Antiplatelet + NSAID",
            "Raises the chance of stomach bleeding, and ibuprofen can interfere with low-dose aspirin's heart protection."),
        InteractionRule(setOf("SSRI"), setOf("NSAID", "NSAID_ASPIRIN", "ANTIPLATELET", "ANTICOAGULANT"), AlertLevel.MODERATE, "SSRI + bleeding-risk medicine",
            "SSRIs affect platelet serotonin; combined with NSAIDs, aspirin or blood thinners, bleeding risk rises."),
        InteractionRule(setOf("SSRI"), setOf("OPIOID"), AlertLevel.HIGH, "SSRI + tramadol",
            "Both raise serotonin; together they can cause serotonin syndrome and lower the seizure threshold."),
        InteractionRule(setOf("PDE5"), setOf("NITRATE"), AlertLevel.HIGH, "PDE5 inhibitor + nitrate",
            "Both widen blood vessels; together they can cause a dangerous drop in blood pressure."),
        InteractionRule(setOf("ACE_INHIBITOR", "ARB"), setOf("K_SPARING", "POTASSIUM"), AlertLevel.MODERATE, "ACE inhibitor/ARB + potassium-raising medicine",
            "Both can raise blood potassium; high potassium can affect heart rhythm. Doctors monitor blood tests when this is used."),
        InteractionRule(setOf("K_SPARING"), setOf("POTASSIUM"), AlertLevel.HIGH, "Potassium-sparing diuretic + potassium supplement",
            "This combination can cause dangerously high potassium levels."),
        InteractionRule(setOf("ACE_INHIBITOR", "ARB", "DIURETIC"), setOf("NSAID"), AlertLevel.MODERATE, "BP/diuretic medicine + NSAID",
            "NSAIDs can reduce the effect of blood-pressure medicines and strain the kidneys, especially with diuretics."),
        InteractionRule(setOf("CYP3A4_INHIBITOR"), setOf("STATIN"), AlertLevel.HIGH, "Clarithromycin + statin",
            "Clarithromycin raises statin levels, increasing the risk of muscle damage."),
        InteractionRule(setOf("THYROID"), setOf("CALCIUM_IRON", "ANTACID"), AlertLevel.INFO, "Levothyroxine + calcium/iron/antacid",
            "These reduce levothyroxine absorption. Pharmacists usually advise separating them by about 4 hours."),
        InteractionRule(setOf("FLUOROQUINOLONE"), setOf("CALCIUM_IRON", "ANTACID"), AlertLevel.MODERATE, "Ciprofloxacin + calcium/iron/antacid",
            "Minerals bind ciprofloxacin and can make the antibiotic less effective unless doses are separated."),
        InteractionRule(setOf("ANTICOAGULANT"), setOf("FLUOROQUINOLONE", "MACROLIDE"), AlertLevel.MODERATE, "Warfarin + antibiotic",
            "Some antibiotics raise INR (warfarin effect). Extra INR checks are often needed."),
        InteractionRule(setOf("SULFONYLUREA", "INSULIN"), setOf("FLUOROQUINOLONE"), AlertLevel.MODERATE, "Diabetes medicine + fluoroquinolone",
            "Fluoroquinolones can cause both low and high blood sugar in people on diabetes medicines."),
        InteractionRule(setOf("SULFONYLUREA", "INSULIN"), setOf("BETA_BLOCKER"), AlertLevel.INFO, "Diabetes medicine + beta-blocker",
            "Beta-blockers can hide warning signs of low blood sugar such as a racing heart."),
        InteractionRule(setOf("CORTICOSTEROID"), setOf("NSAID", "NSAID_ASPIRIN"), AlertLevel.MODERATE, "Steroid + NSAID",
            "Together they raise the risk of stomach ulcers and bleeding."),
        InteractionRule(setOf("ANTIPLATELET"), setOf("PPI"), AlertLevel.INFO, "Clopidogrel + PPI",
            "Omeprazole may reduce clopidogrel's activation. Discuss the choice of stomach protection with your doctor — do not stop either on your own.")
    )

    private fun isClopidogrelPpiPair(a: MedicineInfo, b: MedicineInfo): Boolean {
        val keys = setOf(a.key, b.key)
        return "clopidogrel" in keys && ("omeprazole" in keys || "pantoprazole" in keys)
    }

    fun checkPair(x: MedicineInfo, y: MedicineInfo): List<Pair<AlertLevel, Pair<String, String>>> {
        val out = mutableListOf<Pair<AlertLevel, Pair<String, String>>>()
        for (r in rules) {
            val hit = (x.tags.any { it in r.a } && y.tags.any { it in r.b }) || (y.tags.any { it in r.a } && x.tags.any { it in r.b })
            if (!hit) continue
            if (r.title.startsWith("Clopidogrel + PPI") && !isClopidogrelPpiPair(x, y)) continue
            // pantoprazole has a weaker effect on clopidogrel
            val level = if (r.title.startsWith("Clopidogrel + PPI") && (x.key == "pantoprazole" || y.key == "pantoprazole")) AlertLevel.INFO else r.level
            out += level to (r.title to r.why)
        }
        if (x.key != y.key) {
            val shared = x.ingredients.intersect(y.ingredients.toSet())
            if (shared.isNotEmpty()) out += AlertLevel.HIGH to ("Same active ingredient: ${shared.joinToString()}" to
                "Both products contain ${shared.joinToString()}. Taking them together can exceed the safe daily amount.")
            if (x.tags.contains("NSAID") && y.tags.contains("NSAID") && shared.isEmpty())
                out += AlertLevel.MODERATE to ("Two NSAIDs" to "Using two anti-inflammatory painkillers together increases stomach, kidney and heart side effects without much extra benefit.")
        }
        return out
    }

    fun analyze(
        medicines: List<MedicineEntity>,
        allergies: List<String>,
        conditions: List<String>,
        now: Long = System.currentTimeMillis()
    ): List<SafetyAlert> {
        val alerts = mutableListOf<SafetyAlert>()
        val active = medicines.filter { it.active }
        val matched = active.map { it to MedicineKnowledgeBase.find(it.name) }

        // 1. Duplicate entries
        active.groupBy { MedicineKnowledgeBase.normalize(it.name) }.filter { it.value.size > 1 }.forEach { (n, list) ->
            alerts += SafetyAlert(AlertLevel.MODERATE, "Duplicate", "Duplicate entry: ${list.first().name}",
                "\"$n\" is recorded ${list.size} times as an active medicine.",
                "Check whether this is the same prescription entered twice, and remove or end the extra entry.", list.map { it.id })
        }

        // 2. Interactions / duplicate ingredients among library-matched medicines
        val known = matched.filter { it.second != null }
        for (i in known.indices) for (j in i + 1 until known.size) {
            val (ma, ia) = known[i]; val (mb, ib) = known[j]
            if (ia!!.key == ib!!.key) {
                if (MedicineKnowledgeBase.normalize(ma.name) != MedicineKnowledgeBase.normalize(mb.name))
                    alerts += SafetyAlert(AlertLevel.HIGH, "Duplicate", "Same medicine under two names",
                        "${ma.name} and ${mb.name} both appear to be ${ia.displayName}.",
                        "Ask your pharmacist to confirm before taking both.", listOf(ma.id, mb.id))
                continue
            }
            checkPair(ia, ib).forEach { (lvl, tw) ->
                alerts += SafetyAlert(lvl, "Interaction", "${tw.first}: ${ma.name} + ${mb.name}", tw.second,
                    "Do not stop or change either medicine on your own. Discuss this combination with your doctor or pharmacist.", listOf(ma.id, mb.id))
            }
        }

        // 3. Allergy matches
        val allergyText = allergies.map { it.lowercase() }
        for ((m, info) in matched) {
            if (info == null) continue
            for (a in allergyText) {
                val ingredientHit = info.ingredients.any { a.contains(it) } || info.names.any { it.length > 3 && a.contains(it) }
                val groupHit = MedicineKnowledgeBase.allergyGroups.any { (kw, tags) -> a.contains(kw) && info.tags.any { it in tags } }
                if (ingredientHit || groupHit) {
                    alerts += SafetyAlert(AlertLevel.HIGH, "Allergy", "Allergy match: ${m.name}",
                        "Your recorded allergy \"$a\" matches ${info.displayName} (${info.drugClass}).",
                        "Do not take this medicine until your doctor or pharmacist confirms it is safe for you. If you have swelling, wheeze or trouble breathing, seek emergency care.", listOf(m.id))
                    break
                }
            }
        }

        // 4. Condition-specific cautions
        val condText = conditions.map { it.lowercase() }
        for ((m, info) in matched) {
            info ?: continue
            info.conditionCautions.forEach { (kw, msg) ->
                val hit = condText.firstOrNull { c ->
                    c.contains(kw) || (kw == "hypertension" && (c.contains("blood pressure") || c.contains("bp"))) ||
                        (kw == "kidney" && (c.contains("ckd") || c.contains("renal"))) ||
                        (kw == "heart" && (c.contains("cardiac") || c.contains("coronary")))
                }
                if (hit != null) alerts += SafetyAlert(AlertLevel.MODERATE, "Condition", "${m.name} and your condition \"$hit\"", msg,
                    "Confirm with your doctor that this medicine suits your condition.", listOf(m.id))
            }
        }

        // 5. Missing dosage information
        for (m in active) {
            val missing = buildList {
                if (m.strength.isBlank()) add("strength")
                if (m.dosage.isBlank()) add("dose")
                if (m.frequency.isBlank() && m.times.isBlank()) add("frequency")
            }
            if (missing.isNotEmpty()) alerts += SafetyAlert(AlertLevel.INFO, "Incomplete", "Missing details: ${m.name}",
                "No ${missing.joinToString(", ")} recorded. Reminders and safety checks are less reliable without them.",
                "Copy the details exactly from your prescription or medicine label.", listOf(m.id))
        }

        // 6. Expiry / course end
        for (m in medicines) {
            m.expiryDate?.let { if (it < now) alerts += SafetyAlert(AlertLevel.HIGH, "Expired", "Expired: ${m.name}",
                "The recorded expiry date has passed. Expired medicines may be less effective or unsafe.",
                "Don't use it. Ask your pharmacist about safe disposal and a replacement.", listOf(m.id)) }
            if (m.active) m.endDate?.let { if (it < now) alerts += SafetyAlert(AlertLevel.INFO, "Course ended", "Course end date passed: ${m.name}",
                "The planned end date has passed but the medicine is still marked active.",
                "If your doctor ended it, mark it inactive. If you're unsure whether to continue, ask your doctor — don't decide alone.", listOf(m.id)) }
        }

        // 7. Supervision-required medicines
        for ((m, info) in matched) {
            info?.supervision?.let { alerts += SafetyAlert(AlertLevel.INFO, "Supervision", "Doctor supervision: ${m.name}", it,
                "Keep follow-up appointments and blood tests as your doctor arranged.", listOf(m.id)) }
        }

        // 8. Not in offline library
        for ((m, info) in matched) if (info == null) alerts += SafetyAlert(AlertLevel.INFO, "Unchecked", "Not in offline library: ${m.name}",
            "MediLife's offline dataset has no information about \"${m.name}\", so it was not checked for interactions or allergies.",
            "Ask your pharmacist to review it with your other medicines.", listOf(m.id))

        return alerts.distinctBy { it.title + it.why }.sortedByDescending { it.level.rank }
    }
}

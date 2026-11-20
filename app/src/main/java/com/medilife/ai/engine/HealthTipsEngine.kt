package com.medilife.ai.engine

import java.time.LocalDate
import java.time.temporal.IsoFields

data class HealthTip(
    val category: String,
    val title: String,
    val body: String,
    val avoidIfConditions: List<String> = emptyList(),
    val avoidIfAllergies: List<String> = emptyList(),
    val forConditions: List<String> = emptyList()
) {
    val personalized get() = forConditions.isNotEmpty()
}

object HealthTipsEngine {
    val categories = listOf("Heart health", "Respiratory health", "Kidney health", "Liver health", "Mental wellness", "Nutrition", "Hydration", "Sleep", "Exercise", "Preventive healthcare")

    private val fluidRestricted = listOf("heart failure", "kidney", "ckd", "renal", "dialysis", "cirrhosis")
    private val heartRisk = listOf("heart", "cardiac", "coronary", "angina", "arrhythmia")

    val tips = listOf(
        HealthTip("Heart health", "Move a little, often", "Breaking up long sitting periods with 2–3 minutes of walking every hour supports circulation and blood sugar."),
        HealthTip("Heart health", "Watch hidden salt", "Pickles, papads, namkeen, instant noodles and sauces carry a lot of salt. Tasting before adding salt is an easy first step."),
        HealthTip("Heart health", "Know your numbers", "Knowing your blood pressure, cholesterol and blood sugar helps you and your doctor track heart risk over time."),
        HealthTip("Heart health", "Try brisk interval walks", "Alternate 1 minute of brisk walking with 2 minutes of easy walking for 20 minutes.", avoidIfConditions = heartRisk + listOf("copd")),
        HealthTip("Respiratory health", "Breathe through the nose", "Slow nasal breathing — in for 4, out for 6 — can calm the body and is easy to practise anywhere."),
        HealthTip("Respiratory health", "Check air quality", "On high-pollution days, move exercise indoors and keep windows closed during peak traffic hours."),
        HealthTip("Respiratory health", "Carry your reliever", "If you have asthma, keep your reliever inhaler with you and check its expiry date.", forConditions = listOf("asthma")),
        HealthTip("Kidney health", "Go easy on painkillers", "Regular use of NSAID painkillers can strain the kidneys. Ask a pharmacist before using them often."),
        HealthTip("Kidney health", "Mind your BP and sugar", "High blood pressure and diabetes are leading causes of kidney disease — keeping them controlled protects your kidneys.", forConditions = listOf("diabetes", "hypertension", "blood pressure")),
        HealthTip("Kidney health", "Potassium-rich fruits", "Bananas, oranges and coconut water are rich in potassium and good for most people.", avoidIfConditions = fluidRestricted),
        HealthTip("Liver health", "Alcohol-free days", "Giving your liver several alcohol-free days each week reduces its workload."),
        HealthTip("Liver health", "Read paracetamol labels", "Many cold and flu remedies contain paracetamol. Check labels so you never take two at once."),
        HealthTip("Liver health", "Trim sugary drinks", "Sugary drinks contribute to fatty liver. Swap one a day for water, buttermilk or lemon water without sugar."),
        HealthTip("Mental wellness", "Name it to tame it", "Writing down what's bothering you for 5 minutes can reduce how intense it feels."),
        HealthTip("Mental wellness", "Daylight in the morning", "10–15 minutes of morning daylight helps set your body clock and lift mood."),
        HealthTip("Mental wellness", "Connect with someone", "A short call with a friend or family member is good for mental health — and theirs."),
        HealthTip("Nutrition", "Half-plate vegetables", "Aim to fill half your plate with vegetables and salad at lunch and dinner."),
        HealthTip("Nutrition", "Choose whole grains", "Brown rice, millets (ragi, jowar, bajra) and whole-wheat roti release energy more slowly than refined grains."),
        HealthTip("Nutrition", "A handful of nuts", "A small handful of unsalted nuts is a heart-friendly snack.", avoidIfAllergies = listOf("nut", "peanut", "almond", "cashew", "walnut")),
        HealthTip("Nutrition", "Add curd or buttermilk", "Plain curd or buttermilk adds protein and probiotics to meals.", avoidIfAllergies = listOf("milk", "dairy", "lactose")),
        HealthTip("Nutrition", "Pair carbs with protein", "Adding dal, eggs, paneer or legumes to carb-heavy meals can soften sugar spikes.", forConditions = listOf("diabetes", "prediabetes")),
        HealthTip("Nutrition", "Consistent meal times", "If you take diabetes medicine, regular meal times help avoid low blood sugar. Follow your doctor's plan.", forConditions = listOf("diabetes")),
        HealthTip("Hydration", "Drink regularly", "Sip water through the day; pale-yellow urine is a simple sign of good hydration.", avoidIfConditions = fluidRestricted),
        HealthTip("Hydration", "Follow your fluid plan", "If your doctor has set a fluid limit, measure drinks across the day so you stay within it.", forConditions = fluidRestricted),
        HealthTip("Hydration", "Water before tea", "Have a glass of water before your morning tea or coffee.", avoidIfConditions = fluidRestricted),
        HealthTip("Sleep", "Fixed wake-up time", "Waking at the same time every day — even weekends — is one of the strongest sleep habits."),
        HealthTip("Sleep", "Screens off earlier", "Dimming screens 30–60 minutes before bed helps your brain wind down."),
        HealthTip("Sleep", "Caffeine cut-off", "Try having your last tea or coffee before 2 pm."),
        HealthTip("Exercise", "150 minutes a week", "Adults are generally advised to aim for about 150 minutes of moderate activity a week, built up gradually.", avoidIfConditions = heartRisk),
        HealthTip("Exercise", "Gentle and approved", "With a heart condition, ask your doctor which activities and intensity are right for you before starting new exercise.", forConditions = heartRisk),
        HealthTip("Exercise", "Strength twice a week", "Simple strength moves — sit-to-stand, wall push-ups — help muscles, bones and blood sugar."),
        HealthTip("Exercise", "Walk after meals", "A 10–15 minute walk after meals helps lower blood sugar spikes.", forConditions = listOf("diabetes", "prediabetes")),
        HealthTip("Preventive healthcare", "Annual check-up", "A yearly check-up helps catch silent problems like high BP, sugar or cholesterol early."),
        HealthTip("Preventive healthcare", "Diabetic eye and foot checks", "People with diabetes benefit from yearly eye screening and daily foot checks for cuts or sores.", forConditions = listOf("diabetes")),
        HealthTip("Preventive healthcare", "Home BP log", "Measure BP seated after 5 minutes' rest, arm at heart level, and log it — your doctor can use the trend.", forConditions = listOf("hypertension", "blood pressure")),
        HealthTip("Preventive healthcare", "Vaccines for adults", "Ask your doctor whether flu, pneumococcal or other adult vaccines are recommended for you.")
    )

    val weeklyChallenges = listOf(
        "Walk 7,000+ steps on 5 days this week (adjust to what your doctor advises).",
        "Add one extra serving of vegetables to every lunch.",
        "Be in bed with lights off by the same time every night.",
        "Log your blood pressure or weight on 4 days this week.",
        "Swap one sugary drink a day for water or buttermilk.",
        "Practise 5 minutes of slow breathing every evening.",
        "Take every scheduled medicine dose on time for 7 days.",
        "Spend 10 minutes outdoors in morning daylight each day."
    )

    fun eligible(conditions: List<String>, allergies: List<String>): List<HealthTip> {
        val c = conditions.map { it.lowercase() }
        val a = allergies.map { it.lowercase() }
        return tips.filter { tip ->
            val blockedByCondition = tip.avoidIfConditions.any { kw -> c.any { it.contains(kw) } }
            val blockedByAllergy = tip.avoidIfAllergies.any { kw -> a.any { it.contains(kw) } }
            val relevant = tip.forConditions.isEmpty() || tip.forConditions.any { kw -> c.any { it.contains(kw) } }
            !blockedByCondition && !blockedByAllergy && relevant
        }
    }

    fun dailyTip(conditions: List<String>, allergies: List<String>, date: LocalDate = LocalDate.now()): HealthTip {
        val list = eligible(conditions, allergies)
        val personal = list.filter { it.personalized }
        // Alternate: personalized tip every other day when available.
        val pool = if (personal.isNotEmpty() && date.dayOfYear % 2 == 0) personal else list
        return pool[date.dayOfYear % pool.size]
    }

    fun weeklyChallenge(date: LocalDate = LocalDate.now()) =
        weeklyChallenges[date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) % weeklyChallenges.size]
}

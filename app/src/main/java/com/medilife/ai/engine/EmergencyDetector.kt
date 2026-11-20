package com.medilife.ai.engine

data class EmergencyAlert(val title: String, val advice: String)

/** Rule-based red-flag detection. Runs before any other response logic. */
object EmergencyDetector {
    private data class Rule(val title: String, val patterns: List<Regex>, val advice: String)

    private fun r(vararg p: String) = p.map { Regex(it, RegexOption.IGNORE_CASE) }

    private val rules = listOf(
        Rule("Possible heart emergency", r("chest (pain|tight|pressure|heaviness)", "pain.*(left arm|jaw).*", "heart attack", "crushing pain"),
            "Chest pain, pressure or tightness — especially with sweating, breathlessness, or pain spreading to the arm, jaw or back — can be a heart attack. Call emergency services now. Do not drive yourself."),
        Rule("Severe breathing difficulty", r("can'?t breathe", "cannot breathe", "unable to breathe", "struggling to breathe", "severe(ly)? (breathless|short of breath|breathing)", "gasping", "lips? (are |turning )?blue", "choking"),
            "Severe difficulty breathing, blue lips or choking is an emergency. Call emergency services now."),
        Rule("Possible stroke", r("face (drooping|droop|numb)", "slurred speech", "can'?t speak", "weakness (in|on) one side", "one side (of my )?(body|face)", "sudden (numbness|weakness|confusion|vision loss)", "stroke", "worst headache"),
            "Think BE FAST: Balance, Eyes, Face drooping, Arm weakness, Speech difficulty — Time to call emergency services. Note the time symptoms started."),
        Rule("Unconsciousness or seizure", r("unconscious", "not responding", "unresponsive", "passed out", "fainted and", "seizure", "convulsion", "\\bfits?\\b"),
            "If someone is unconscious, unresponsive or having a seizure, call emergency services. Place them on their side if breathing, and do not put anything in their mouth."),
        Rule("Severe allergic reaction", r("anaphyla", "(throat|tongue|lips?|face) (is )?(swelling|swollen|closing)", "swelling.*(throat|tongue|lips)", "hives.*(breath|wheez)", "severe allergic"),
            "Swelling of the face, lips, tongue or throat, wheezing or collapse after a trigger can be anaphylaxis. Use a prescribed adrenaline auto-injector if available and call emergency services now."),
        Rule("Heavy bleeding", r("bleeding (heavily|won'?t stop|a lot)", "vomiting blood", "coughing (up )?blood", "black tarry stool", "blood in vomit"),
            "Heavy or uncontrolled bleeding, vomiting blood or black tarry stools need urgent medical care. Call emergency services or go to the nearest emergency department."),
        Rule("Possible overdose or poisoning", r("overdose", "took too many", "poison", "swallowed (bleach|chemical|pesticide)"),
            "Possible overdose or poisoning needs urgent help. Call emergency services or a poison helpline now. Keep the medicine packet to show responders."),
        Rule("You deserve support right now", r("suicid", "kill myself", "end my life", "self.?harm", "hurt myself", "don'?t want to live"),
            "If you're thinking about ending your life or harming yourself, please reach out now — call your local emergency number or, in India, Tele-MANAS on 14416 (free, 24×7). You don't have to go through this alone.")
    )

    fun detect(text: String): EmergencyAlert? {
        val t = text.lowercase()
        if (Regex("\\b(no|not|without|never had|denies)\\b.{0,12}(chest pain|breathing|stroke|seizure)").containsMatchIn(t) &&
            !Regex("chest pain now|having chest pain").containsMatchIn(t)) return null
        return rules.firstOrNull { rule -> rule.patterns.any { it.containsMatchIn(t) } }?.let { EmergencyAlert(it.title, it.advice) }
    }

    val warningSigns = listOf(
        "Chest pain, pressure or tightness",
        "Severe shortness of breath or blue lips",
        "Face drooping, arm weakness, slurred speech (stroke)",
        "Unconsciousness, unresponsiveness or a seizure",
        "Swelling of face, lips, tongue or throat after a trigger",
        "Heavy bleeding, vomiting blood or black stools",
        "Sudden severe headache — the worst of your life",
        "Thoughts of self-harm"
    )
}

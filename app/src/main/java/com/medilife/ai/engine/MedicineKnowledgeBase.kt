package com.medilife.ai.engine

/**
 * Curated offline medicine library. General educational content only — it is not
 * a substitute for the product leaflet, a pharmacist or a doctor, and it is not
 * comprehensive.
 */
data class MedicineInfo(
    val key: String,
    val displayName: String,
    val names: List<String>,
    val ingredients: List<String>,
    val drugClass: String,
    val tags: Set<String>,
    val uses: String,
    val howItWorks: String,
    val sideEffects: List<String>,
    val precautions: List<String>,
    val interactions: List<String>,
    val storage: String = "Store below 25–30 °C in a dry place, away from direct sunlight and out of reach of children.",
    val missedDose: String = MedicineKnowledgeBase.STANDARD_MISSED,
    val prescriptionOnly: Boolean = true,
    val supervision: String? = null,
    /** condition keyword -> caution text */
    val conditionCautions: Map<String, String> = emptyMap()
)

object MedicineKnowledgeBase {
    const val STANDARD_MISSED =
        "Check the patient leaflet or ask your pharmacist. A common rule is to take the missed dose when you remember unless it is nearly time for the next one — never take a double dose to make up for a missed one."
    const val DISCLAIMER =
        "General educational information from MediLife's offline library. It is not complete and does not replace your doctor, pharmacist or the medicine leaflet."

    private val nsaidKidney = "kidney" to "NSAIDs can reduce kidney function; people with kidney disease should only use them if a doctor agrees."
    private val nsaidBp = "hypertension" to "NSAIDs can raise blood pressure and reduce the effect of BP medicines."
    private val nsaidHeart = "heart" to "NSAIDs may increase cardiovascular risk and fluid retention in heart conditions."
    private val nsaidUlcer = "ulcer" to "NSAIDs can cause stomach bleeding, especially with a history of ulcers."
    private val nsaidAsthma = "asthma" to "Some people with asthma are sensitive to NSAIDs/aspirin."
    private val nsaidCautions = mapOf(nsaidKidney, nsaidBp, nsaidHeart, nsaidUlcer, nsaidAsthma)

    val all: List<MedicineInfo> = listOf(
        MedicineInfo(
            "paracetamol", "Paracetamol (acetaminophen)", listOf("paracetamol", "acetaminophen", "crocin", "dolo", "calpol", "panadol", "tylenol", "pacimol"),
            listOf("paracetamol"), "Analgesic / antipyretic", setOf("ANALGESIC"),
            "Relief of mild to moderate pain and fever.",
            "Acts mainly in the brain to reduce pain signals and lower fever; it has little anti-inflammatory effect.",
            listOf("Usually well tolerated at recommended doses", "Rarely skin rash", "Liver damage in overdose"),
            listOf("Many combination products (cold remedies, Combiflam) also contain paracetamol — avoid taking two at once.", "Do not exceed the daily maximum on the label.", "Regular alcohol use and liver disease increase the risk of liver harm."),
            listOf("Warfarin: regular high use may increase INR.", "Other paracetamol-containing products: overdose risk."),
            prescriptionOnly = false,
            conditionCautions = mapOf("liver" to "Paracetamol is processed by the liver; people with liver disease should ask a doctor about safe limits.")
        ),
        MedicineInfo(
            "ibuprofen", "Ibuprofen", listOf("ibuprofen", "brufen", "advil", "ibugesic", "nurofen"),
            listOf("ibuprofen"), "NSAID (non-steroidal anti-inflammatory)", setOf("NSAID"),
            "Pain, fever and inflammation (e.g. headache, dental pain, muscle and joint pain).",
            "Blocks COX enzymes that make prostaglandins, which drive pain, inflammation and fever.",
            listOf("Indigestion, heartburn, nausea", "Stomach ulcer or bleeding (less common)", "Raised blood pressure, fluid retention", "Reduced kidney function"),
            listOf("Take after food.", "Avoid in late pregnancy.", "Use the lowest effective dose for the shortest time."),
            listOf("Blood thinners (warfarin, aspirin, clopidogrel): bleeding risk.", "BP medicines (ARBs, ACE inhibitors, diuretics): reduced effect and kidney strain.", "SSRIs and steroids: stomach bleeding risk."),
            prescriptionOnly = false, conditionCautions = nsaidCautions
        ),
        MedicineInfo(
            "combiflam", "Ibuprofen + Paracetamol (Combiflam)", listOf("combiflam", "ibuprofen + paracetamol", "ibuprofen paracetamol"),
            listOf("ibuprofen", "paracetamol"), "Combination analgesic (NSAID + paracetamol)", setOf("NSAID", "ANALGESIC"),
            "Short-term relief of pain and fever.",
            "Combines an NSAID (anti-inflammatory) with paracetamol (central pain/fever relief).",
            listOf("Indigestion, nausea", "Stomach bleeding (uncommon)", "Liver harm in paracetamol overdose"),
            listOf("Contains paracetamol — do not combine with other paracetamol products.", "Take after food."),
            listOf("All ibuprofen and paracetamol interactions apply."),
            prescriptionOnly = false, conditionCautions = nsaidCautions
        ),
        MedicineInfo(
            "diclofenac", "Diclofenac", listOf("diclofenac", "voveran", "voltaren", "dynapar"),
            listOf("diclofenac"), "NSAID", setOf("NSAID"),
            "Pain and inflammation, e.g. arthritis, sprains, back pain.",
            "Blocks prostaglandin production (COX inhibition).",
            listOf("Indigestion, stomach pain", "Stomach bleeding", "Raised blood pressure", "Liver enzyme changes"),
            listOf("Higher cardiovascular risk than some other NSAIDs — doctors avoid it in heart disease.", "Take with or after food."),
            listOf("Blood thinners, SSRIs, steroids: bleeding risk.", "BP medicines and diuretics: kidney strain."),
            conditionCautions = nsaidCautions
        ),
        MedicineInfo(
            "aspirin", "Aspirin", listOf("aspirin", "ecosprin", "disprin", "loprin", "acetylsalicylic acid"),
            listOf("aspirin"), "Antiplatelet / NSAID", setOf("ANTIPLATELET", "NSAID_ASPIRIN"),
            "Low dose: prevents clots in people with heart or stroke risk (only when prescribed). Higher doses: pain and fever.",
            "Irreversibly blocks platelet COX-1, making platelets less sticky.",
            listOf("Indigestion", "Easy bruising, bleeding", "Stomach ulcer/bleeding"),
            listOf("Do not stop prescribed low-dose aspirin without talking to your doctor.", "Not for children under 16 (Reye's syndrome).", "Tell dentists/surgeons you take it."),
            listOf("Ibuprofen may reduce its protective effect.", "Warfarin, clopidogrel, SSRIs: bleeding risk."),
            supervision = "Low-dose aspirin for heart/stroke prevention should be supervised by a doctor.",
            conditionCautions = mapOf(nsaidUlcer, nsaidAsthma)
        ),
        MedicineInfo(
            "clopidogrel", "Clopidogrel", listOf("clopidogrel", "clopilet", "plavix", "deplatt"),
            listOf("clopidogrel"), "Antiplatelet", setOf("ANTIPLATELET"),
            "Prevents clots after heart attack, stents or stroke.",
            "Blocks the P2Y12 receptor on platelets so they clump less. It is activated by the liver enzyme CYP2C19.",
            listOf("Bleeding, bruising", "Stomach upset", "Rash"),
            listOf("Never stop suddenly, especially after a stent — contact your cardiologist first.", "Tell any surgeon or dentist."),
            listOf("Omeprazole/esomeprazole may reduce its activation.", "NSAIDs, warfarin, SSRIs: bleeding risk."),
            supervision = "Requires cardiologist/doctor supervision."
        ),
        MedicineInfo(
            "warfarin", "Warfarin", listOf("warfarin", "warf", "coumadin", "uniwarfin"),
            listOf("warfarin"), "Anticoagulant (vitamin K antagonist)", setOf("ANTICOAGULANT"),
            "Prevents and treats blood clots (e.g. atrial fibrillation, DVT, valve replacements).",
            "Blocks vitamin K–dependent clotting factors made in the liver.",
            listOf("Bleeding (gums, nose, urine, stools)", "Bruising"),
            listOf("Needs regular INR blood tests.", "Keep vitamin K intake (green leafy vegetables) consistent.", "Many medicines, antibiotics and supplements change its effect."),
            listOf("NSAIDs, aspirin, clopidogrel: serious bleeding risk.", "Antibiotics (e.g. ciprofloxacin, macrolides) may raise INR.", "Paracetamol at high regular doses may raise INR."),
            missedDose = "Do not double up. Note the missed dose and tell the clinic that monitors your INR — follow their instructions.",
            supervision = "Requires INR monitoring and doctor supervision."
        ),
        MedicineInfo(
            "metformin", "Metformin", listOf("metformin", "glycomet", "glucophage", "obimet", "gluconorm"),
            listOf("metformin"), "Biguanide (diabetes)", setOf("BIGUANIDE", "DIABETES"),
            "Type 2 diabetes; sometimes PCOS (as prescribed).",
            "Reduces glucose production by the liver and improves insulin sensitivity.",
            listOf("Nausea, diarrhoea, stomach upset (often settles)", "Metallic taste", "Low vitamin B12 with long-term use", "Lactic acidosis (rare, serious)"),
            listOf("Take with or after meals to reduce stomach upset.", "Your doctor may pause it before contrast scans or surgery.", "Limit alcohol."),
            listOf("Alcohol: higher lactic acidosis risk.", "Iodinated contrast dye: may need temporary pause."),
            conditionCautions = mapOf("kidney" to "Metformin dosing depends on kidney function (eGFR); your doctor monitors this.")
        ),
        MedicineInfo(
            "glimepiride", "Glimepiride", listOf("glimepiride", "amaryl", "glimy", "glimisave"),
            listOf("glimepiride"), "Sulfonylurea (diabetes)", setOf("SULFONYLUREA", "DIABETES", "SULFA_LIKE"),
            "Type 2 diabetes.",
            "Stimulates the pancreas to release more insulin.",
            listOf("Low blood sugar (hypoglycaemia)", "Weight gain", "Nausea"),
            listOf("Take with breakfast; do not skip meals after taking it.", "Learn hypoglycaemia signs: sweating, shaking, confusion."),
            listOf("Fluoroquinolones: blood sugar swings.", "Beta-blockers may mask low-sugar symptoms.", "Alcohol: hypoglycaemia risk.")
        ),
        MedicineInfo(
            "insulin", "Insulin", listOf("insulin", "lantus", "glargine", "novorapid", "humalog", "mixtard", "huminsulin", "tresiba", "aspart", "lispro"),
            listOf("insulin"), "Insulin (diabetes)", setOf("INSULIN", "DIABETES"),
            "Type 1 and type 2 diabetes.",
            "Replaces or supplements the body's own insulin, moving glucose into cells.",
            listOf("Low blood sugar", "Injection-site reactions", "Weight gain"),
            listOf("Doses are individual — only change them as your diabetes team instructs.", "Carry a fast-acting sugar source.", "Rotate injection sites."),
            listOf("Beta-blockers may mask low-sugar symptoms.", "Steroids can raise blood sugar."),
            storage = "Unopened: refrigerate at 2–8 °C, never freeze. In-use pen: room temperature (below 25–30 °C) for the period on the leaflet.",
            missedDose = "Missed insulin needs a plan from your diabetes team — check your glucose and follow their instructions; do not guess a dose.",
            supervision = "Requires doctor supervision and glucose monitoring."
        ),
        MedicineInfo(
            "amlodipine", "Amlodipine", listOf("amlodipine", "amlong", "stamlo", "amlip", "norvasc", "amlokind"),
            listOf("amlodipine"), "Calcium-channel blocker", setOf("CCB", "BP"),
            "High blood pressure and angina.",
            "Relaxes blood vessel walls by blocking calcium entry into muscle cells.",
            listOf("Ankle swelling", "Flushing, headache", "Dizziness", "Palpitations"),
            listOf("Do not stop suddenly without advice.", "Rise slowly if you feel dizzy."),
            listOf("Simvastatin (high doses): increased statin levels.", "Other BP medicines: additive lowering of BP.")
        ),
        MedicineInfo(
            "telmisartan", "Telmisartan", listOf("telmisartan", "telma", "telmikind", "micardis"),
            listOf("telmisartan"), "Angiotensin receptor blocker (ARB)", setOf("ARB", "BP"),
            "High blood pressure; cardiovascular protection.",
            "Blocks angiotensin II receptors, relaxing blood vessels.",
            listOf("Dizziness", "High potassium", "Changes in kidney function"),
            listOf("Not for use in pregnancy.", "Your doctor may check potassium and kidney tests."),
            listOf("Potassium supplements, spironolactone: high potassium.", "NSAIDs: kidney strain and reduced BP control.")
        ),
        MedicineInfo(
            "losartan", "Losartan", listOf("losartan", "losar", "cozaar", "repace"),
            listOf("losartan"), "Angiotensin receptor blocker (ARB)", setOf("ARB", "BP"),
            "High blood pressure; kidney protection in diabetes.",
            "Blocks angiotensin II receptors, relaxing blood vessels.",
            listOf("Dizziness", "High potassium", "Changes in kidney function"),
            listOf("Not for use in pregnancy.", "Blood tests for potassium/kidney function as advised."),
            listOf("Potassium supplements, spironolactone: high potassium.", "NSAIDs: kidney strain.")
        ),
        MedicineInfo(
            "ramipril", "Ramipril", listOf("ramipril", "cardace", "ramistar", "altace"),
            listOf("ramipril"), "ACE inhibitor", setOf("ACE_INHIBITOR", "BP"),
            "High blood pressure, heart failure, after heart attack.",
            "Blocks the enzyme that makes angiotensin II, relaxing blood vessels.",
            listOf("Dry cough", "Dizziness", "High potassium", "Angioedema (rare, urgent)"),
            listOf("Facial, lip or tongue swelling needs emergency care.", "Not for use in pregnancy."),
            listOf("Potassium supplements, spironolactone: high potassium.", "NSAIDs: kidney strain.")
        ),
        MedicineInfo(
            "metoprolol", "Metoprolol", listOf("metoprolol", "metolar", "betaloc", "seloken", "lopressor"),
            listOf("metoprolol"), "Beta-blocker", setOf("BETA_BLOCKER", "BP"),
            "High blood pressure, angina, heart rhythm problems, heart failure.",
            "Blocks beta-1 receptors, slowing heart rate and reducing workload.",
            listOf("Tiredness", "Cold hands and feet", "Slow pulse", "Dizziness"),
            listOf("Never stop suddenly — it can worsen angina; doctors taper it.", "Can mask signs of low blood sugar."),
            listOf("Insulin/sulfonylureas: masked hypoglycaemia.", "Verapamil/diltiazem: very slow heart rate."),
            conditionCautions = mapOf("asthma" to "Beta-blockers can worsen asthma in some people; doctors choose agents carefully.")
        ),
        MedicineInfo(
            "atorvastatin", "Atorvastatin", listOf("atorvastatin", "atorva", "lipitor", "storvas", "atocor", "tonact"),
            listOf("atorvastatin"), "Statin (cholesterol)", setOf("STATIN"),
            "Lowers LDL cholesterol and cardiovascular risk.",
            "Blocks HMG-CoA reductase, the liver enzyme that makes cholesterol.",
            listOf("Muscle aches", "Digestive upset", "Raised liver enzymes (uncommon)", "Muscle breakdown (rare, serious)"),
            listOf("Report unexplained muscle pain, weakness or dark urine.", "Avoid large amounts of grapefruit juice."),
            listOf("Clarithromycin/erythromycin: higher statin levels and muscle risk.", "Some antifungals and HIV medicines raise statin levels.")
        ),
        MedicineInfo(
            "rosuvastatin", "Rosuvastatin", listOf("rosuvastatin", "rosuvas", "crestor", "rozavel", "rosulip"),
            listOf("rosuvastatin"), "Statin (cholesterol)", setOf("STATIN"),
            "Lowers LDL cholesterol and cardiovascular risk.",
            "Blocks HMG-CoA reductase in the liver.",
            listOf("Muscle aches", "Headache", "Digestive upset"),
            listOf("Report unexplained muscle pain or weakness."),
            listOf("Antacids with aluminium/magnesium: reduced absorption (separate doses).")
        ),
        MedicineInfo(
            "omeprazole", "Omeprazole", listOf("omeprazole", "omez", "prilosec", "ocid"),
            listOf("omeprazole"), "Proton pump inhibitor (PPI)", setOf("PPI"),
            "Acid reflux, ulcers, stomach protection.",
            "Blocks the stomach's acid pump.",
            listOf("Headache", "Diarrhoea or constipation", "Long-term: low magnesium, B12; fracture risk"),
            listOf("Usually taken 30–60 min before breakfast.", "Long-term use should be reviewed with a doctor."),
            listOf("Clopidogrel: may reduce its effectiveness.", "May change absorption of some medicines.")
        ),
        MedicineInfo(
            "pantoprazole", "Pantoprazole", listOf("pantoprazole", "pan", "pantocid", "pan-d", "pantop", "protonix"),
            listOf("pantoprazole"), "Proton pump inhibitor (PPI)", setOf("PPI"),
            "Acid reflux, ulcers, stomach protection.",
            "Blocks the stomach's acid pump.",
            listOf("Headache", "Diarrhoea", "Long-term: low magnesium, B12"),
            listOf("Usually taken before breakfast.", "Long-term use should be reviewed with a doctor."),
            listOf("Fewer clopidogrel concerns than omeprazole, but tell your doctor about all medicines.")
        ),
        MedicineInfo(
            "levothyroxine", "Levothyroxine", listOf("levothyroxine", "thyronorm", "eltroxin", "thyrox", "synthroid", "thyroxine"),
            listOf("levothyroxine"), "Thyroid hormone", setOf("THYROID"),
            "Hypothyroidism (underactive thyroid).",
            "Replaces the thyroid hormone T4.",
            listOf("Usually none at the right dose", "Too high a dose: palpitations, weight loss, anxiety, sleeplessness"),
            listOf("Take on an empty stomach, ideally 30–60 min before breakfast, at the same time daily.", "TSH blood tests guide the dose."),
            listOf("Calcium, iron, antacids: reduce absorption — separate by about 4 hours."),
            supervision = "Dose is adjusted by your doctor using thyroid blood tests."
        ),
        MedicineInfo(
            "cetirizine", "Cetirizine", listOf("cetirizine", "okacet", "cetzine", "zyrtec", "alerid"),
            listOf("cetirizine"), "Antihistamine", setOf("ANTIHISTAMINE"),
            "Allergic rhinitis, hives, itching.",
            "Blocks histamine H1 receptors.",
            listOf("Drowsiness (in some people)", "Dry mouth", "Headache"),
            listOf("Avoid driving if it makes you drowsy.", "Alcohol can increase drowsiness."),
            listOf("Alcohol and sedatives: extra drowsiness."),
            prescriptionOnly = false
        ),
        MedicineInfo(
            "montelukast", "Montelukast", listOf("montelukast", "montair", "singulair", "montek"),
            listOf("montelukast"), "Leukotriene receptor antagonist", setOf("LEUKOTRIENE"),
            "Asthma prevention and allergic rhinitis.",
            "Blocks leukotrienes that cause airway inflammation.",
            listOf("Headache", "Stomach upset", "Mood or sleep changes (report these)"),
            listOf("Not a rescue inhaler for acute attacks.", "Report mood changes, nightmares or agitation to your doctor."),
            listOf("Few significant interactions in the offline library.")
        ),
        MedicineInfo(
            "salbutamol", "Salbutamol (albuterol)", listOf("salbutamol", "albuterol", "asthalin", "ventolin", "levolin", "levosalbutamol"),
            listOf("salbutamol"), "Short-acting bronchodilator", setOf("BRONCHODILATOR"),
            "Quick relief of asthma or COPD symptoms.",
            "Relaxes airway muscles by stimulating beta-2 receptors.",
            listOf("Tremor", "Fast heartbeat", "Headache"),
            listOf("Needing it more often than usual means your asthma needs review.", "If it is not helping in an attack, seek emergency care."),
            listOf("Non-selective beta-blockers may reduce its effect."),
            storage = "Store below 30 °C; do not puncture or burn the canister."
        ),
        MedicineInfo(
            "amoxicillin", "Amoxicillin", listOf("amoxicillin", "amoxycillin", "mox", "novamox", "amoxil"),
            listOf("amoxicillin"), "Penicillin antibiotic", setOf("PENICILLIN", "ANTIBIOTIC"),
            "Bacterial infections (ear, throat, chest, urinary, dental).",
            "Stops bacteria building their cell walls.",
            listOf("Diarrhoea, nausea", "Rash", "Allergic reactions (can be severe)"),
            listOf("Do not take if allergic to penicillin.", "Complete the course as prescribed.", "Antibiotics do not treat viral colds."),
            listOf("Warfarin: possible INR change.", "Methotrexate: increased levels.")
        ),
        MedicineInfo(
            "augmentin", "Amoxicillin + Clavulanate", listOf("augmentin", "amoxiclav", "co-amoxiclav", "clavam", "moxclav"),
            listOf("amoxicillin", "clavulanate"), "Penicillin antibiotic combination", setOf("PENICILLIN", "ANTIBIOTIC"),
            "Bacterial infections where resistance is suspected.",
            "Amoxicillin kills bacteria; clavulanate protects it from bacterial enzymes.",
            listOf("Diarrhoea", "Nausea", "Rash", "Liver enzyme changes (rare)"),
            listOf("Do not take if allergic to penicillin.", "Take at the start of a meal.", "Complete the course."),
            listOf("Warfarin: possible INR change.")
        ),
        MedicineInfo(
            "azithromycin", "Azithromycin", listOf("azithromycin", "azithral", "azee", "zithromax", "azax"),
            listOf("azithromycin"), "Macrolide antibiotic", setOf("MACROLIDE", "ANTIBIOTIC"),
            "Respiratory, skin and some other bacterial infections.",
            "Blocks bacterial protein production.",
            listOf("Diarrhoea, nausea, stomach pain", "Heart rhythm changes (QT) — rare"),
            listOf("Complete the course.", "Tell your doctor about heart rhythm problems."),
            listOf("Warfarin: possible INR change.", "Other QT-prolonging medicines.")
        ),
        MedicineInfo(
            "clarithromycin", "Clarithromycin", listOf("clarithromycin", "claribid", "klacid"),
            listOf("clarithromycin"), "Macrolide antibiotic", setOf("MACROLIDE", "ANTIBIOTIC", "CYP3A4_INHIBITOR"),
            "Respiratory infections, H. pylori treatment.",
            "Blocks bacterial protein production.",
            listOf("Taste changes", "Nausea, diarrhoea", "Heart rhythm changes (rare)"),
            listOf("Many interactions — always tell the prescriber about every medicine."),
            listOf("Statins (atorvastatin, simvastatin): muscle damage risk.", "Warfarin: raised INR.")
        ),
        MedicineInfo(
            "ciprofloxacin", "Ciprofloxacin", listOf("ciprofloxacin", "ciplox", "cifran", "cipro"),
            listOf("ciprofloxacin"), "Fluoroquinolone antibiotic", setOf("FLUOROQUINOLONE", "ANTIBIOTIC"),
            "Urinary, gut and some other bacterial infections.",
            "Blocks bacterial DNA replication enzymes.",
            listOf("Nausea, diarrhoea", "Tendon pain or rupture (rare)", "Dizziness", "Blood sugar changes"),
            listOf("Stop and seek advice if tendon pain develops.", "Separate from antacids, calcium, iron and dairy."),
            listOf("Calcium/iron/antacids: reduced absorption.", "Warfarin: raised INR.", "Diabetes medicines: blood sugar swings.")
        ),
        MedicineInfo(
            "sertraline", "Sertraline", listOf("sertraline", "zoloft", "serta", "daxid"),
            listOf("sertraline"), "SSRI antidepressant", setOf("SSRI"),
            "Depression, anxiety disorders, OCD, PTSD.",
            "Increases serotonin availability in the brain.",
            listOf("Nausea", "Sleep changes", "Sexual side effects", "Initial anxiety"),
            listOf("Takes 2–6 weeks for full effect.", "Do not stop suddenly — doctors taper.", "Seek help urgently for thoughts of self-harm."),
            listOf("NSAIDs, aspirin, warfarin: bleeding risk.", "Tramadol and other serotonergic medicines: serotonin syndrome."),
            supervision = "Should be started, adjusted and stopped under a doctor's supervision."
        ),
        MedicineInfo(
            "escitalopram", "Escitalopram", listOf("escitalopram", "nexito", "lexapro", "cipralex", "stalopam"),
            listOf("escitalopram"), "SSRI antidepressant", setOf("SSRI"),
            "Depression and anxiety disorders.",
            "Increases serotonin availability in the brain.",
            listOf("Nausea", "Sleep changes", "Sexual side effects"),
            listOf("Do not stop suddenly.", "Seek help urgently for thoughts of self-harm."),
            listOf("NSAIDs, aspirin, warfarin: bleeding risk.", "Tramadol: serotonin syndrome risk."),
            supervision = "Should be started, adjusted and stopped under a doctor's supervision."
        ),
        MedicineInfo(
            "tramadol", "Tramadol", listOf("tramadol", "ultracet", "tramazac", "contramal"),
            listOf("tramadol"), "Opioid analgesic", setOf("OPIOID"),
            "Moderate to severe pain.",
            "Acts on opioid receptors and increases serotonin and noradrenaline.",
            listOf("Nausea, dizziness, drowsiness", "Constipation", "Dependence", "Seizures (rare)"),
            listOf("Avoid alcohol and driving if drowsy.", "Risk of dependence — use only as prescribed."),
            listOf("SSRIs/SNRIs: serotonin syndrome and seizure risk.", "Sedatives, alcohol: breathing suppression."),
            supervision = "Controlled analgesic — use only under prescription."
        ),
        MedicineInfo(
            "prednisolone", "Prednisolone", listOf("prednisolone", "wysolone", "omnacortil", "prednisone"),
            listOf("prednisolone"), "Corticosteroid", setOf("CORTICOSTEROID"),
            "Inflammatory and autoimmune conditions, severe allergies, asthma flares.",
            "Suppresses inflammation and immune activity.",
            listOf("Raised blood sugar", "Mood and sleep changes", "Increased appetite", "Stomach irritation", "Long-term: bone thinning, infections"),
            listOf("Take in the morning with food.", "Do not stop suddenly after more than a few weeks — doctors taper.", "Can raise blood sugar in diabetes."),
            listOf("NSAIDs: stomach bleeding risk.", "Diabetes medicines: higher glucose."),
            supervision = "Long courses need doctor supervision and tapering.",
            conditionCautions = mapOf("diabetes" to "Steroids can raise blood glucose — monitor more closely as advised.")
        ),
        MedicineInfo(
            "spironolactone", "Spironolactone", listOf("spironolactone", "aldactone", "spiractin"),
            listOf("spironolactone"), "Potassium-sparing diuretic", setOf("K_SPARING", "DIURETIC"),
            "Heart failure, high blood pressure, fluid retention, some hormonal conditions.",
            "Blocks aldosterone, so the kidneys lose salt and water but keep potassium.",
            listOf("High potassium", "Breast tenderness", "Dizziness"),
            listOf("Potassium levels need checking.", "Avoid potassium supplements and salt substitutes unless advised."),
            listOf("ACE inhibitors/ARBs, potassium supplements: high potassium.", "NSAIDs: kidney strain.")
        ),
        MedicineInfo(
            "furosemide", "Furosemide", listOf("furosemide", "lasix", "frusemide"),
            listOf("furosemide"), "Loop diuretic", setOf("DIURETIC"),
            "Fluid overload in heart, kidney or liver disease; high blood pressure.",
            "Makes the kidneys excrete more salt and water.",
            listOf("Frequent urination", "Low potassium/sodium", "Dehydration, dizziness"),
            listOf("Usually taken in the morning.", "Blood tests for salts as advised."),
            listOf("NSAIDs: reduced effect and kidney strain.")
        ),
        MedicineInfo(
            "sildenafil", "Sildenafil", listOf("sildenafil", "viagra", "penegra", "manforce"),
            listOf("sildenafil"), "PDE5 inhibitor", setOf("PDE5"),
            "Erectile dysfunction; pulmonary hypertension (different dosing).",
            "Relaxes blood vessels by blocking PDE5.",
            listOf("Headache, flushing", "Low blood pressure", "Visual changes"),
            listOf("Never combine with nitrates (e.g. angina sprays/tablets)."),
            listOf("Nitrates: dangerous drop in blood pressure.")
        ),
        MedicineInfo(
            "nitrate", "Nitrates (isosorbide / nitroglycerin)", listOf("isosorbide", "isosorbide mononitrate", "isosorbide dinitrate", "nitroglycerin", "glyceryl trinitrate", "sorbitrate", "nitrocontin", "monotrate", "gtn"),
            listOf("nitrate"), "Nitrate (angina)", setOf("NITRATE"),
            "Prevention and relief of angina chest pain.",
            "Releases nitric oxide, widening blood vessels and reducing heart workload.",
            listOf("Headache", "Dizziness, low blood pressure", "Flushing"),
            listOf("Chest pain not relieved as instructed needs emergency care.", "Sit down before using a spray/tablet under the tongue."),
            listOf("Sildenafil/tadalafil: dangerous drop in blood pressure.")
        ),
        MedicineInfo(
            "vitamin_d", "Vitamin D3 (cholecalciferol)", listOf("vitamin d", "vitamin d3", "cholecalciferol", "uprise", "calcirol", "d3 must", "d-rise"),
            listOf("cholecalciferol"), "Vitamin supplement", setOf("SUPPLEMENT"),
            "Vitamin D deficiency; bone health.",
            "Helps the body absorb calcium and supports bone and muscle function.",
            listOf("Rare at normal doses", "Very high doses: high calcium (nausea, confusion, kidney stones)"),
            listOf("High-dose weekly products should follow the prescribed schedule exactly."),
            listOf("Few significant interactions in the offline library."),
            prescriptionOnly = false
        ),
        MedicineInfo(
            "calcium", "Calcium carbonate", listOf("calcium", "calcium carbonate", "shelcal", "calcimax", "tums", "ccm"),
            listOf("calcium"), "Mineral supplement / antacid", setOf("CALCIUM_IRON", "ANTACID"),
            "Calcium supplementation; heartburn relief.",
            "Provides calcium; neutralises stomach acid.",
            listOf("Constipation", "Bloating"),
            listOf("Separate from thyroid medicine and some antibiotics."),
            listOf("Levothyroxine, ciprofloxacin: reduced absorption."),
            prescriptionOnly = false
        ),
        MedicineInfo(
            "iron", "Iron (ferrous salts)", listOf("iron", "ferrous sulfate", "ferrous sulphate", "ferrous fumarate", "livogen", "autrin", "orofer", "dexorange"),
            listOf("iron"), "Mineral supplement", setOf("CALCIUM_IRON"),
            "Iron-deficiency anaemia.",
            "Replaces iron needed to make haemoglobin.",
            listOf("Dark stools (expected)", "Constipation", "Nausea"),
            listOf("Keep away from children — iron overdose is dangerous for them.", "Separate from tea/coffee, calcium and antacids."),
            listOf("Levothyroxine, ciprofloxacin: reduced absorption."),
            prescriptionOnly = false
        ),
        MedicineInfo(
            "potassium", "Potassium supplements", listOf("potassium chloride", "potassium citrate", "k-cl", "kcl", "potklor"),
            listOf("potassium"), "Electrolyte supplement", setOf("POTASSIUM"),
            "Low potassium.",
            "Replaces potassium.",
            listOf("Stomach upset", "High potassium if overused"),
            listOf("Use only as prescribed; levels need monitoring."),
            listOf("ACE inhibitors, ARBs, spironolactone: high potassium.")
        )
    )

    private val nameIndex: List<Pair<String, MedicineInfo>> =
        all.flatMap { info -> info.names.map { normalize(it) to info } }.sortedByDescending { it.first.length }

    fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9+ ]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Finds the best library match for a medicine name (generic or common brand). */
    fun find(name: String): MedicineInfo? {
        val n = normalize(name)
        if (n.isBlank()) return null
        nameIndex.firstOrNull { it.first == n }?.let { return it.second }
        return nameIndex.firstOrNull { (k, _) ->
            Regex("(^|\\s)" + Regex.escape(k) + "($|\\s)").containsMatchIn(n)
        }?.second
    }

    /** All library medicines mentioned anywhere in free text. */
    fun mentionedIn(text: String): List<MedicineInfo> {
        val n = " " + normalize(text) + " "
        return nameIndex.filter { (k, _) -> k.length >= 3 && n.contains(" $k ") }.map { it.second }.distinct()
    }

    /** Allergy keyword groups → tags/ingredients they cover. */
    val allergyGroups: Map<String, Set<String>> = mapOf(
        "penicillin" to setOf("PENICILLIN"),
        "amoxicillin" to setOf("PENICILLIN"),
        "ampicillin" to setOf("PENICILLIN"),
        "beta-lactam" to setOf("PENICILLIN"),
        "nsaid" to setOf("NSAID", "NSAID_ASPIRIN"),
        "aspirin" to setOf("NSAID_ASPIRIN", "NSAID"),
        "ibuprofen" to setOf("NSAID"),
        "diclofenac" to setOf("NSAID"),
        "sulfa" to setOf("SULFA_LIKE"),
        "sulpha" to setOf("SULFA_LIKE"),
        "sulfonamide" to setOf("SULFA_LIKE"),
        "macrolide" to setOf("MACROLIDE"),
        "erythromycin" to setOf("MACROLIDE"),
        "quinolone" to setOf("FLUOROQUINOLONE"),
        "fluoroquinolone" to setOf("FLUOROQUINOLONE"),
        "statin" to setOf("STATIN"),
        "opioid" to setOf("OPIOID"),
        "codeine" to setOf("OPIOID"),
        "morphine" to setOf("OPIOID")
    )
}

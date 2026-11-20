# MediLife AI — Premium Personal Health Companion for Android

MediLife AI is a local-first Android health companion written in Kotlin with Jetpack Compose (Material 3). It keeps a medical history, schedules medicine reminders, runs offline medicine-safety checks, reads lab reports with on-device OCR, and includes **Medi AI**, a conversational assistant that runs without any API keys or cloud services.

> MediLife AI gives general health information and organisation tools. It does **not** diagnose, prescribe or replace professional medical advice.

## Download the APK

Every push to `main` builds a signed release APK with GitHub Actions:

- **Releases** page → latest `MediLife AI build N` → `MediLifeAI-v1.0.0-release.apk`
- or **Actions** → latest run → artifact `MediLifeAI-release-apk`

Inside the build, the APK is produced at:

```
app/build/outputs/apk/release/app-release.apk
```

### Install on your phone
1. Download the APK on your Android phone (Android 10 or newer).
2. Open it and allow "Install unknown apps" for your browser/file manager when asked.
3. Open **MediLife AI** → choose *Start with my own data* or *Explore with demo data*.
4. Allow notifications so medicine reminders can appear. On Vivo/Oppo/Xiaomi phones, also set MediLife's battery usage to "No restrictions" so reminders aren't delayed.

## Build locally (Android Studio)
1. Install Android Studio (Ladybug or newer) with JDK 17 and Android SDK 35.
2. Open this folder → let Gradle sync.
3. Run on a device, or `./gradlew assembleRelease`.

## Features

| Area | What's implemented |
|---|---|
| Dashboard | Time-based greeting, live date/time, profile photo, health overview (heart rate, BP, SpO₂, weight, BMI, activity, sleep, glucose) with **MANUAL / DEVICE** source badges, today's doses with Taken/Skip, next reminder, weekly adherence ring, daily tip, safety alerts, symptom trend, upcoming check-up, animated *Wellness habits score* (explicitly **not** clinical) |
| Medical profile | Name, age, gender, height, weight, blood group, doctor, emergency contact, conditions, allergies, surgeries, family history, previous medicines, editable medical timeline |
| Medicines | Add/edit with strength, dose as prescribed, frequency, times, start/end/expiry dates, food instructions, doctor's notes; medicine information screens (uses, how it works, side effects, precautions, interactions, storage, missed-dose guidance) |
| Safety engine | Offline rules: duplicate entries & duplicate ingredients, curated interaction pairs, allergy matching (incl. drug-class groups such as penicillins/NSAIDs), condition cautions, missing dosage info, expired packs, supervision-required medicines, "not in library" notices — each alert explains **why** |
| Medi AI | Offline intent engine with personalised templates: history summary, medicine explanations, precautions, interaction questions, BP/glucose/HR/SpO₂ interpretation, lab terms & report explanation, routines, doctor questions, symptom education, the 5-step "what should I take" flow, emergency red-flag detection. Optional on-device LLM (MediaPipe) — every reply is labelled with the engine that produced it |
| Reports | Import PDF, gallery photo or camera picture → on-device ML Kit OCR → table-row reconstruction → parsed tests, values, units and **printed** reference ranges → user review/edit → history charts and plain-English explanations |
| Health | Measurement logging with plausibility checks, trend charts, symptom journal (with emergency detection), appointments, doctor-visit summary sharing, personalised tips & weekly challenges, interactive body-system explorer |
| Reminders | WorkManager daily jobs per dose time; notification actions log Taken/Skip directly; hydration reminders |
| Privacy | No INTERNET permission (stripped even from libraries), Android-Keystore AES-GCM encryption of sensitive fields, biometric/device-credential lock, cloud backup disabled, PDF export, passphrase-encrypted backup/restore, clear demo data, clear all data |
| Demo data | Realistic sample patient flagged `isDemo` in every table, shown with **DEMO DATA** badges, never exported in backups, removable in one tap |

## Architecture

```
com.medilife.ai
├── data/        Room entities + DAO, FieldCrypto (Keystore), SettingsStore (DataStore), HealthRepository, DoseSchedule, DemoData
├── engine/      MedicineKnowledgeBase, SafetyEngine, EmergencyDetector, MediAiEngine, LocalLlm, HealthTipsEngine, LabParser/LabGlossary, WellnessScore
├── reminders/   ReminderScheduler, DoseReminderWorker, HydrationWorker, DoseActionReceiver, Notifications
├── services/    OcrService (ML Kit + PdfRenderer), Exporters (PDF + doctor summary), BackupManager
└── ui/          MediViewModel (MVVM, StateFlow), theme, components (glass cards, animated rings, charts, particles), screens
```

- **MVVM**: one `MediViewModel` exposes a combined `HealthSnapshot` StateFlow derived from Room Flows; screens are stateless Compose functions.
- **Charts**: drawn directly on Compose Canvas with animated reveal (no external chart library needed).
- **Animations**: floating particles, glowing heartbeat, animated rings, page transitions, pulsing indicators, AI orb, body scan — all disabled by *Reduce motion*.

## Optional on-device LLM
1. Get a MediaPipe-compatible model file (e.g. a Gemma 2B/3 1B `.task` file) on your phone.
2. Settings → *Medi AI engine* → **Install model file** → enable **Use on-device model**.
3. Safety-critical answers (emergencies, "what should I take", interactions) always stay rule-based. If the model can't load, MediLife says so and keeps working.

## Signing
The repo includes a demo keystore (`keystore/medilife-release.jks`) so CI can produce an installable signed APK. **Before publishing on Google Play, create your own private keystore** and provide it via the environment variables `MEDILIFE_KEYSTORE_PATH`, `MEDILIFE_KEYSTORE_PASSWORD`, `MEDILIFE_KEY_ALIAS`, `MEDILIFE_KEY_PASSWORD` (e.g. GitHub secrets).

## Limitations
- The medicine and interaction dataset is small and curated; it is **not comprehensive**.
- OCR accuracy depends on photo quality and report layout; every extracted value must be reviewed.
- No wearable integration yet; all readings are manual (and labelled so). The data model already distinguishes DEVICE readings for a future Health Connect integration.

package com.medilife.ai

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.medilife.ai.data.AppDatabase
import com.medilife.ai.data.AppSettings
import com.medilife.ai.data.DoseStatus
import com.medilife.ai.data.HealthRepository
import com.medilife.ai.data.LabReportEntity
import com.medilife.ai.data.LabResultEntity
import com.medilife.ai.data.MedicalRecordEntity
import com.medilife.ai.data.MedicineEntity
import com.medilife.ai.data.RecordType
import com.medilife.ai.engine.AiSource
import com.medilife.ai.engine.LocalLlm
import com.medilife.ai.engine.MediAiEngine
import com.medilife.ai.reminders.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseAndEngineTest {
    private lateinit var ctx: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: HealthRepository

    @Before fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = HealthRepository(db)
    }

    @After fun tearDown() = db.close()

    @Test fun encryptedRecordsRoundTrip() = runBlocking {
        repo.saveRecord(MedicalRecordEntity(type = RecordType.ALLERGY, title = "Penicillin", details = "rash"))
        val raw = db.dao().recordsOnce().first()
        assertTrue("stored value must be encrypted", raw.title.startsWith("enc1:"))
        assertEquals("Penicillin", repo.recordsOnce().first().title)
    }

    @Test fun medicineDoseAndReportCascade() = runBlocking {
        val id = repo.saveMedicine(MedicineEntity(name = "Metformin", strength = "500 mg", dosage = "1 tablet", times = "08:00", startDate = System.currentTimeMillis()))
        repo.logDose(id, 1000L, DoseStatus.TAKEN)
        assertEquals(1, db.dao().doseLogsOnce().size)
        repo.deleteMedicine(id)
        assertEquals(0, db.dao().doseLogsOnce().size)

        val rid = repo.saveReport(LabReportEntity(title = "Panel", reportDate = 1L), listOf(LabResultEntity(reportId = 0, testName = "HbA1c", value = 7.0, reportDate = 0)))
        assertEquals(1, db.dao().resultsOnce().size)
        repo.deleteReport(rid)
        assertEquals(0, db.dao().resultsOnce().size)
    }

    @Test fun demoDataIsSeparateAndRemovable() = runBlocking {
        repo.saveMedicine(MedicineEntity(name = "My real medicine", startDate = 0))
        repo.loadDemoData()
        assertTrue(repo.demoActive.first())
        assertTrue(repo.medicinesOnce().size > 1)
        repo.removeDemoData()
        val left = repo.medicinesOnce()
        assertEquals(1, left.size)
        assertEquals("My real medicine", left.first().name)
        assertFalse(repo.demoActive.first())
    }

    @Test fun mediAiAnswersOfflineAndLabelsSource() = runBlocking {
        repo.loadDemoData()
        val ai = MediAiEngine(LocalLlm(ctx))
        val st = AppSettings()
        val s = repo.snapshot()
        val summary = ai.reply("Explain my medical history", s, st)
        assertEquals(AiSource.RULE, summary.source)
        assertTrue(summary.text.contains("Type 2 diabetes"))
        val em = ai.reply("I have severe chest pain", s, st)
        assertTrue(em.emergency)
        val ask = ai.reply("What medicine should I take for my headache?", s, st)
        assertTrue(ask.text.contains("warning signs"))
        val edu = ai.reply("no", s, st)
        assertTrue(edu.text.contains("pharmacist"))
        val bp = ai.reply("What does 150/95 mean?", s, st)
        assertTrue(bp.text.contains("stage 2"))
        val inter = ai.reply("Can I take ibuprofen with aspirin together?", s, st)
        assertTrue(inter.text.contains("NOT comprehensive"))
    }

    @Test fun remindersAreScheduledWithWorkManager() {
        val sched = ReminderScheduler(ctx)
        val meds = listOf(MedicineEntity(id = 42, name = "Test", times = "08:00,20:00", startDate = 0))
        sched.rescheduleAll(meds, true)
        val infos = WorkManager.getInstance(ctx).getWorkInfosByTag(ReminderScheduler.TAG_DOSE).get().filter { !it.state.isFinished }
        assertEquals(2, infos.size)
        sched.rescheduleAll(meds, false)
        val after = WorkManager.getInstance(ctx).getWorkInfosByTag(ReminderScheduler.TAG_DOSE).get().filter { !it.state.isFinished }
        assertEquals(0, after.size)
    }
}

@RunWith(AndroidJUnit4::class)
class NavigationUiTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Before fun prepare() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<MediLifeApp>()
        app.container.settings.setOnboardingDone(true)
        app.container.settings.setBiometric(false)
        app.container.settings.setReduceMotion(true)
        app.container.repo.loadDemoData()
    }

    private fun waitFor(step: String, text: String, timeout: Long = 20_000) {
        try {
            compose.waitUntil(timeout) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            throw AssertionError("Step '$step' failed: text '$text' not shown", e)
        }
    }

    private fun tap(label: String) = compose.onAllNodesWithText(label).onFirst().performClick()

    @Test fun bottomNavigationReachesAllTabs() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("home", "Health overview", 30_000)
            tap("Medicines"); waitFor("medicines tab", "AI Medicine Safety Engine")
            tap("Health"); waitFor("health tab", "Log a measurement")
            tap("Medi AI"); waitFor("medi ai tab", "Offline rule-based engine")
            tap("Profile"); waitFor("profile tab", "Medical history & timeline")
            tap("Medical history & timeline"); waitFor("history screen", "Medical conditions")
        }
    }
}

package de.nick.waterreminderapp

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntakeStoreTest {

    private val YEAR = 2026
    private lateinit var fakeTime: FakeTimeProvider

    // today wird in @Before gesetzt – kein Instanzfeld-Problem da
    // JUnit pro Test EINE neue Klasseninstanz erzeugt.
    // Wir brauchen den Companion für einen klassenweiten, monoton steigenden Zähler.
    companion object {
        private var counter = 10
    }

    // today und yesterday als einfache Int – werden in @Before gesetzt
    private var testDay = 0
    private val yesterday get() = testDay - 1

    @Before
    fun setup() {
        counter += 10        // counter ist companion → überlebt zwischen Instanzen
        testDay = counter    // Diese Instanz nutzt diesen Tag
        fakeTime = FakeTimeProvider(
            hour      = 10,
            dayOfWeek = Calendar.MONDAY,
            dayOfYear = testDay,
            year      = YEAR
        )
    }

    private fun store() = IntakeStore(
        context      = ApplicationProvider.getApplicationContext(),
        timeProvider = fakeTime
    )

    // ── addMl ────────────────────────────────────────────────────────────

    @Test fun `addMl accumulates within same day`() = runBlocking {
        val s = store()
        s.addMl(250)
        s.addMl(250)
        assertEquals(500, s.totalMlTodayFlow.first())
    }

    @Test fun `addMl resets on new day same year`() = runBlocking {
        val s = store()
        fakeTime.advanceToDayOfYear(yesterday, YEAR)
        s.addMl(750)
        assertEquals(750, s.totalMlTodayFlow.first())

        fakeTime.advanceToDayOfYear(testDay, YEAR)
        s.addMl(250)
        assertEquals(250, s.totalMlTodayFlow.first())
    }

    @Test fun `addMl resets on year boundary`() = runBlocking {
        val s = store()
        fakeTime.advanceToDayOfYear(testDay, 2025)
        s.addMl(1000)
        assertEquals(1000, s.totalMlTodayFlow.first())

        fakeTime.advanceToDayOfYear(testDay, YEAR)
        s.addMl(100)
        assertEquals(100, s.totalMlTodayFlow.first())
    }

    // ── hasReachedGoal ───────────────────────────────────────────────────

    @Test fun `hasReachedGoal returns false when below goal`() = runBlocking {
        val s = store()
        s.addMl(1750)
        assertFalse(s.hasReachedGoal(2000))
    }

    @Test fun `hasReachedGoal returns true when exactly at goal`() = runBlocking {
        val s = store()
        s.addMl(2000)
        assertTrue(s.hasReachedGoal(2000))
    }

    @Test fun `hasReachedGoal returns true when above goal`() = runBlocking {
        val s = store()
        s.addMl(2250)
        assertTrue(s.hasReachedGoal(2000))
    }

    // ── congratsSent ─────────────────────────────────────────────────────

    @Test fun `isCongratsSentToday is false initially`() = runBlocking {
        assertFalse(store().isCongratsSentToday())
    }

    @Test fun `markCongratsSent makes isCongratsSentToday true`() = runBlocking {
        val s = store()
        s.markCongratsSent()
        assertTrue(s.isCongratsSentToday())
    }

    @Test fun `congratsSent resets on new day`() = runBlocking {
        val s = store()
        fakeTime.advanceToDayOfYear(yesterday, YEAR)
        s.markCongratsSent()
        assertTrue(s.isCongratsSentToday())

        fakeTime.advanceToDayOfYear(testDay, YEAR)
        assertFalse(s.isCongratsSentToday())
    }

    @Test fun `congratsSent resets on year boundary`() = runBlocking {
        val s = store()
        fakeTime.advanceToDayOfYear(testDay, 2025)
        s.markCongratsSent()
        assertTrue(s.isCongratsSentToday())

        fakeTime.advanceToDayOfYear(testDay, YEAR)
        assertFalse(s.isCongratsSentToday())
    }

    @Test fun `totalMlTodayFlow shows 0 after day change without writing`() = runBlocking {
        val s = store()
        fakeTime.advanceToDayOfYear(yesterday, YEAR)
        s.addMl(500)

        fakeTime.advanceToDayOfYear(testDay, YEAR)
        assertEquals(0, s.totalMlTodayFlow.first())
    }
}

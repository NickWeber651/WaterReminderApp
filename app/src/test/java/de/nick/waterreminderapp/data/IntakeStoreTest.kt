package de.nick.waterreminderapp.data

import androidx.test.core.app.ApplicationProvider
import de.nick.waterreminderapp.util.FakeTimeProvider
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

    companion object { private var counter = 10 }

    private var testDay = 0
    private val yesterday get() = testDay - 1

    @Before
    fun setup() {
        counter += 10
        testDay  = counter
        fakeTime = FakeTimeProvider(10, Calendar.MONDAY, testDay, YEAR)
    }

    private fun store() = IntakeStore(ApplicationProvider.getApplicationContext(), fakeTime)

    @Test fun `addMl accumulates within same day`() = runBlocking {
        val s = store()
        s.addMl(250); s.addMl(250)
        assertEquals(500, s.totalMlTodayFlow.first())
    }

    @Test fun `addMl resets on new day same year`() = runBlocking {
        val s = store()
        fakeTime.advanceToDayOfYear(yesterday, YEAR); s.addMl(750)
        assertEquals(750, s.totalMlTodayFlow.first())
        fakeTime.advanceToDayOfYear(testDay, YEAR); s.addMl(250)
        assertEquals(250, s.totalMlTodayFlow.first())
    }

    @Test fun `addMl resets on year boundary`() = runBlocking {
        val s = store()
        fakeTime.advanceToDayOfYear(testDay, 2025); s.addMl(1000)
        assertEquals(1000, s.totalMlTodayFlow.first())
        fakeTime.advanceToDayOfYear(testDay, YEAR); s.addMl(100)
        assertEquals(100, s.totalMlTodayFlow.first())
    }

    @Test fun `hasReachedGoal returns false when below goal`() = runBlocking {
        store().also { it.addMl(1750) }.let { assertFalse(it.hasReachedGoal(2000)) }
    }

    @Test fun `hasReachedGoal returns true when exactly at goal`() = runBlocking {
        store().also { it.addMl(2000) }.let { assertTrue(it.hasReachedGoal(2000)) }
    }

    @Test fun `hasReachedGoal returns true when above goal`() = runBlocking {
        store().also { it.addMl(2250) }.let { assertTrue(it.hasReachedGoal(2000)) }
    }

    @Test fun `isCongratsSentToday is false initially`() = runBlocking {
        assertFalse(store().isCongratsSentToday())
    }

    @Test fun `markCongratsSent makes isCongratsSentToday true`() = runBlocking {
        val s = store(); s.markCongratsSent()
        assertTrue(s.isCongratsSentToday())
    }

    @Test fun `congratsSent resets on new day`() = runBlocking {
        val s = store()
        fakeTime.advanceToDayOfYear(yesterday, YEAR); s.markCongratsSent()
        assertTrue(s.isCongratsSentToday())
        fakeTime.advanceToDayOfYear(testDay, YEAR)
        assertFalse(s.isCongratsSentToday())
    }

    @Test fun `congratsSent resets on year boundary`() = runBlocking {
        val s = store()
        fakeTime.advanceToDayOfYear(testDay, 2025); s.markCongratsSent()
        assertTrue(s.isCongratsSentToday())
        fakeTime.advanceToDayOfYear(testDay, YEAR)
        assertFalse(s.isCongratsSentToday())
    }

    @Test fun `totalMlTodayFlow shows 0 after day change`() = runBlocking {
        val s = store()
        fakeTime.advanceToDayOfYear(yesterday, YEAR); s.addMl(500)
        fakeTime.advanceToDayOfYear(testDay, YEAR)
        assertEquals(0, s.totalMlTodayFlow.first())
    }
}


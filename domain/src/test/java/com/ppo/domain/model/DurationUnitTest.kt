package com.ppo.domain.model

import com.ppo.domain.runner.RandomOrderRunner
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RandomOrderRunner::class)
class DurationUnitTest {

    @Test
    fun shouldConvertUnitsToSecondsCorrectly_forAllEnumValues() {
        assertEquals(10L, DurationUnit.seconds.toSeconds(10L))
        assertEquals(300L, DurationUnit.minutes.toSeconds(5L))
        assertEquals(7200L, DurationUnit.hours.toSeconds(2L))
        assertEquals(86400L, DurationUnit.days.toSeconds(1L))
    }

    @Test
    fun shouldHandleZeroAndNegativeValues_withoutOverflow() {
        assertEquals(0L, DurationUnit.minutes.toSeconds(0L))
        assertEquals(-600L, DurationUnit.minutes.toSeconds(-10L))
        assertEquals(-86400L, DurationUnit.days.toSeconds(-1L))
    }
}

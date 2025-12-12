package kr.skarch.territory_Plugin.tests

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for war score calculation logic
 * Tests various scenarios including edge cases
 */
class WarScoreCalculationTest {

    /**
     * Calculate war score using the formula:
     * (conquests - lost) + round((kills - deaths) / 2.0)
     * Uses HALF_UP rounding mode for consistent behavior
     */
    private fun calculateScore(conquests: Int, lost: Int, kills: Int, deaths: Int): Int {
        val stoneScore = conquests - lost
        val combatScore = BigDecimal((kills - deaths) / 2.0)
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
        return stoneScore + combatScore
    }

    @Test
    fun testBasicPositiveScore() {
        // 5 conquests, 0 lost, 10 kills, 0 deaths
        // Expected: 5 + 5 = 10
        val score = calculateScore(5, 0, 10, 0)
        assertEquals(10, score, "Basic positive score calculation failed")
    }

    @Test
    fun testWithLosses() {
        // 10 conquests, 3 lost, 20 kills, 5 deaths
        // Expected: (10-3) + round((20-5)/2) = 7 + 8 = 15
        val score = calculateScore(10, 3, 20, 5)
        assertEquals(15, score, "Score with losses calculation failed")
    }

    @Test
    fun testOddKillDifference() {
        // 5 conquests, 0 lost, 11 kills, 0 deaths
        // Expected: 5 + round(11/2) = 5 + 6 = 11
        val score = calculateScore(5, 0, 11, 0)
        assertEquals(11, score, "Odd kill difference rounding failed")
    }

    @Test
    fun testNegativeKillDifference() {
        // 10 conquests, 0 lost, 5 kills, 20 deaths
        // Expected: 10 + round(-15/2) = 10 + (-8) = 2
        val score = calculateScore(10, 0, 5, 20)
        assertEquals(2, score, "Negative kill difference calculation failed")
    }

    @Test
    fun testNegativeStoneScore() {
        // 2 conquests, 5 lost, 10 kills, 0 deaths
        // Expected: (2-5) + round(10/2) = -3 + 5 = 2
        val score = calculateScore(2, 5, 10, 0)
        assertEquals(2, score, "Negative stone score calculation failed")
    }

    @Test
    fun testTotallyNegativeScore() {
        // 0 conquests, 10 lost, 5 kills, 20 deaths
        // Expected: (0-10) + round((5-20)/2) = -10 + (-8) = -18
        val score = calculateScore(0, 10, 5, 20)
        assertEquals(-18, score, "Totally negative score calculation failed")
    }

    @Test
    fun testZeroScore() {
        // 0 conquests, 0 lost, 0 kills, 0 deaths
        // Expected: 0
        val score = calculateScore(0, 0, 0, 0)
        assertEquals(0, score, "Zero score calculation failed")
    }

    @Test
    fun testRoundingEdgeCases() {
        // Test rounding with HALF_UP: 0.5 rounds to 1
        // 0 conquests, 0 lost, 1 kill, 0 deaths
        // Expected: 0 + round(1/2) = 0 + 1 = 1
        val score1 = calculateScore(0, 0, 1, 0)
        assertEquals(1, score1, "Rounding 0.5 failed")

        // -0.5 with HALF_UP rounds to -1 (away from zero, towards negative infinity)
        // 0 conquests, 0 lost, 0 kills, 1 death
        // Expected: 0 + round(-0.5) = 0 + (-1) = -1
        val score2 = calculateScore(0, 0, 0, 1)
        assertEquals(-1, score2, "Rounding -0.5 failed")

        // Test -1.5 rounds to -2
        // 0 conquests, 0 lost, 0 kills, 3 deaths
        // Expected: 0 + round(-1.5) = 0 + (-2) = -2
        val score3 = calculateScore(0, 0, 0, 3)
        assertEquals(-2, score3, "Rounding -1.5 failed")
    }

    @Test
    fun testLargeNumbers() {
        // Test with large numbers
        // 1000 conquests, 500 lost, 2000 kills, 1000 deaths
        // Expected: (1000-500) + round((2000-1000)/2) = 500 + 500 = 1000
        val score = calculateScore(1000, 500, 2000, 1000)
        assertEquals(1000, score, "Large numbers calculation failed")
    }

    @Test
    fun testBalancedWar() {
        // Perfectly balanced war
        // 50 conquests, 50 lost, 100 kills, 100 deaths
        // Expected: (50-50) + round((100-100)/2) = 0 + 0 = 0
        val score = calculateScore(50, 50, 100, 100)
        assertEquals(0, score, "Balanced war calculation failed")
    }
}


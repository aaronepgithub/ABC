package com.aaronep.abc

import org.junit.Assert.assertEquals
import org.junit.Test

class PowerCalculationTest {

    @Test
    fun testPowerCalculation() {
        // speed = 10 m/s (~36 km/h)
        // weight = 80 kg
        // wind = 1 (0 m/s)
        val speed = 10.0
        val weight = 80.0
        val windEstimate = 1

        val rho = 1.225
        val cd = 0.9
        val area = 0.5
        val crr = 0.005
        val g = 9.81

        val windSpeed = (windEstimate - 1) * 2.0
        val dragForce = 0.5 * rho * cd * area * Math.pow(speed + windSpeed, 2.0)
        val rollingForce = crr * weight * g
        val expectedPower = (dragForce + rollingForce) * speed

        // Manual calculation check:
        // dragForce = 0.5 * 1.225 * 0.9 * 0.5 * 100 = 27.5625
        // rollingForce = 0.005 * 80 * 9.81 = 3.924
        // expectedPower = (27.5625 + 3.924) * 10 = 314.865

        assertEquals(314.865, expectedPower, 0.001)
    }
}

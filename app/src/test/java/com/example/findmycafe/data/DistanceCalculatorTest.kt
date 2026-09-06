package com.example.findmycafe.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceCalculatorTest {
    @Test fun `returns null when a coordinate is missing`() {
        assertNull(DistanceCalculator.kilometers(null, Coordinate(-6.8860, 107.6200)))
    }

    @Test fun `formats nearby cafe distance in meters`() {
        val label = DistanceCalculator.label(Coordinate(-6.8860, 107.6200), Coordinate(-6.8870, 107.6200))
        assertEquals(true, label.endsWith("m"))
    }

    @Test fun `same point has zero distance`() {
        assertEquals(0.0, DistanceCalculator.kilometers(Coordinate(-6.8860, 107.6200), Coordinate(-6.8860, 107.6200))!!, 0.001)
    }
}

package com.example

import com.example.ai.ObjectDetectionEngine
import com.example.model.DetectionCategory
import com.example.model.FilterPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testDetectionCategoriesExist() {
        val categories = DetectionCategory.entries
        assertTrue(categories.size >= 8)
        assertNotNull(DetectionCategory.ANIMALS)
        assertNotNull(DetectionCategory.VEHICLES)
        assertNotNull(DetectionCategory.FOOD)
        assertNotNull(DetectionCategory.LANDMARKS)
        assertNotNull(DetectionCategory.PEOPLE)
    }

    @Test
    fun testFilterPresetsExist() {
        val filters = FilterPreset.entries
        assertTrue(filters.contains(FilterPreset.VINTAGE))
        assertTrue(filters.contains(FilterPreset.BLACK_WHITE))
        assertTrue(filters.contains(FilterPreset.CINEMATIC))
        assertTrue(filters.contains(FilterPreset.PORTRAIT))
    }
}

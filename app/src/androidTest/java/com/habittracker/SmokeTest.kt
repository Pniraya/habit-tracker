package com.habittracker

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The smallest possible instrumented test: if this runs on the device, the device-side runner works
 * and Room mappings, migrations and Compose smoke tests become possible (TASK-025).
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @Test
    fun theAppUnderTestIsInstalled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.packageName.startsWith("com.habittracker"))
    }
}

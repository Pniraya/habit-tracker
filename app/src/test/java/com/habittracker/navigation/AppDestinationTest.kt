package com.habittracker.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the navigation contract that Gate 1 introduced: three top-level destinations,
 * stable route names, and no duplicate routes (duplicates would break restoreState).
 */
class AppDestinationTest {

    @Test
    fun routesAreUnique() {
        val routes = AppDestination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun routesMatchApprovedContract() {
        assertEquals(listOf("today", "statistics", "profile"), AppDestination.entries.map { it.route })
    }

    @Test
    fun everyDestinationHasLabel() {
        AppDestination.entries.forEach { destination ->
            assertTrue("label must not be blank for " + destination.route, destination.label.isNotBlank())
        }
    }
}

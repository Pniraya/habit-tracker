package com.habittracker.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRoutesTest {

    @Test
    fun routesAreUnique() {
        assertEquals(AppRoutes.all.size, AppRoutes.all.toSet().size)
    }

    @Test
    fun routesMatchTheApprovedContract() {
        assertEquals(listOf("today", "statistics", "profile"), AppRoutes.all)
    }

    @Test
    fun routesAreLowercaseAndNonBlank() {
        AppRoutes.all.forEach { route ->
            assertTrue("blank route", route.isNotBlank())
            assertEquals(route.lowercase(), route)
        }
    }
}

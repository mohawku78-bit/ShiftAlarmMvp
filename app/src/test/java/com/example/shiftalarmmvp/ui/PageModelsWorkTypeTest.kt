package com.example.shiftalarmmvp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageModelsWorkTypeTest {

    @Test
    fun `normalizeWorkType maps short aliases and trims suffixes`() {
        assertEquals(WORK_TYPE_DAY, normalizeWorkType("주"))
        assertEquals(WORK_TYPE_NIGHT, normalizeWorkType("야간 1팀"))
        assertEquals(WORK_TYPE_DUTY, normalizeWorkType("당직 근무"))
        assertEquals(WORK_TYPE_HOLIDAY, normalizeWorkType("공휴일 대체"))
        assertEquals(WORK_TYPE_NIGHT, normalizeWorkType("석간조"))
    }

    @Test
    fun `hasRestFamilyToken matches rest-like work types`() {
        assertTrue(hasRestFamilyToken(WORK_TYPE_REST))
        assertTrue(hasRestFamilyToken(WORK_TYPE_VACATION))
        assertFalse(hasRestFamilyToken(WORK_TYPE_OFF))
        assertFalse(hasRestFamilyToken(WORK_TYPE_DAY))
    }

    @Test
    fun `isDefaultEnabledWorkType keeps off and rest disabled by default`() {
        assertTrue(isDefaultEnabledWorkType(WORK_TYPE_DAY))
        assertTrue(isDefaultEnabledWorkType(WORK_TYPE_DUTY))
        assertTrue(isDefaultEnabledWorkType(WORK_TYPE_VACATION))
        assertFalse(isDefaultEnabledWorkType(WORK_TYPE_OFF))
        assertFalse(isDefaultEnabledWorkType(WORK_TYPE_REST))
    }

    @Test
    fun `defaultWorkTypeConfigs normalizes deduplicates and preserves default enablement`() {
        val configs = defaultWorkTypeConfigs(
            listOf("주", "주간", "야", "비번", "휴가", " ", "석간")
        )

        assertEquals(
            listOf(WORK_TYPE_DAY, WORK_TYPE_NIGHT, WORK_TYPE_OFF, WORK_TYPE_VACATION),
            configs.map { it.type }
        )
        assertEquals(
            listOf(true, true, false, true),
            configs.map { it.enabled }
        )
    }
}
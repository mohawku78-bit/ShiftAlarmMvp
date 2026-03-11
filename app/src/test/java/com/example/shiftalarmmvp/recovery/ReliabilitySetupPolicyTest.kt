package com.example.shiftalarmmvp.recovery

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliabilitySetupPolicyTest {

    private val texts = testRecoveryStrings().setup

    @Test
    fun `exact alarm is first unresolved step on Android 14`() {
        val ui = ReliabilitySetupPolicy.build(
            signals = ReliabilitySetupSignals(
                exactReady = false,
                notificationReady = false,
                batteryReady = false
            ),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals(3, ui.totalStepCount)
        assertEquals(0, ui.resolvedStepCount)
        assertEquals("정확 알람 권한", ui.primaryStep?.title)
        assertEquals(HomeReliabilityAction.OPEN_EXACT_ALARM_SETTINGS, ui.primaryStep?.action)
    }

    @Test
    fun `resolved exact alarm advances onboarding to notification step`() {
        val ui = ReliabilitySetupPolicy.build(
            signals = ReliabilitySetupSignals(
                exactReady = true,
                notificationReady = false,
                batteryReady = false
            ),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals(3, ui.totalStepCount)
        assertEquals(1, ui.resolvedStepCount)
        assertEquals("알림 권한", ui.primaryStep?.title)
        assertEquals("1/3 정확 알람: 준비됨", "${ui.steps.first().stepNumber}/${ui.steps.first().totalStepCount} ${ui.steps.first().statusText}")
    }

    @Test
    fun `pre tiramisu devices skip notification step`() {
        val ui = ReliabilitySetupPolicy.build(
            signals = ReliabilitySetupSignals(
                exactReady = true,
                notificationReady = false,
                batteryReady = false
            ),
            texts = texts,
            sdkInt = Build.VERSION_CODES.S
        )

        assertEquals(2, ui.totalStepCount)
        assertEquals("배터리 최적화 예외", ui.primaryStep?.title)
        assertTrue(ui.steps.none { it.issue == ReliabilitySetupIssue.NOTIFICATION_PERMISSION })
    }

    @Test
    fun `registered next alarm becomes shared follow up step`() {
        val ui = ReliabilitySetupPolicy.build(
            signals = ReliabilitySetupSignals(
                exactReady = true,
                notificationReady = true,
                batteryReady = true,
                nextAlarmRegisteredReady = false,
                shouldCheckAlarmRegistration = true
            ),
            texts = texts,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )

        assertEquals(4, ui.totalStepCount)
        assertEquals("다음 알람 등록", ui.primaryStep?.title)
        assertEquals(HomeReliabilityAction.RESCHEDULE_ALARMS, ui.primaryStep?.action)
    }
}
# Quick Alarm Test Mode

## Goal

Let the user verify the alarm stack without waiting for a real scheduled alarm time.

## Proposed Modes

1. Immediate ring simulation
   - Start the same ringing flow used by a real alarm.
   - Play sound, show the alarm notification, expose snooze/stop actions, and include the Galaxy Watch bridged controls.
   - Mark the result as a self-test log entry so it is not confused with a real scheduled alarm.

2. Reservation dry-run
   - Calculate the next trigger times for enabled alarms.
   - Check exact alarm permission, notification permission, battery optimization state, reboot reschedule snapshot, and Direct Boot snapshot availability.
   - Show a pass/warn/fail checklist immediately.

3. Short scheduled test
   - Offer a one-tap "ring in 10 seconds" test for the cases where we specifically want to verify `AlarmManager` delivery.
   - This is still fast, but it tests more of the real OS path than pure simulation.

## Limits

An immediate simulation can prove that the app's ringing UI, sound, notification, snooze, stop, and watch bridge paths work. It cannot fully prove that Android will deliver a future alarm after reboot, Doze, or OEM battery restrictions. For that, the app should still keep the real reboot/reschedule checks and a short scheduled test.

## Recommended UX

Place this in the reliability/settings area as "빠른 알람 테스트".

The page should show three actions:

1. "지금 울려보기" for immediate ring simulation.
2. "예약 상태 점검" for instant dry-run diagnostics.
3. "10초 뒤 실제 예약 테스트" for a fast real `AlarmManager` delivery check.

# Galaxy Watch alarm integration

This project now has a phone app module and a Wear OS companion module for alarm control.

## What it does

- Phone alarm starts in `AlarmRingingService`.
- Phone sends `/shift_alarm/alarm/start` to connected Wear OS nodes through `MessageClient`.
- Phone also writes `/shift_alarm/alarm/active` as an urgent `DataItem`, so the watch has a backup sync path if the immediate message is missed.
- Phone-to-watch start, cancel, and control-ACK messages are sent in a short three-attempt burst so a brief Bluetooth/Data Layer hiccup is less likely to lose the alarm signal.
- Watch receives the message in `WatchAlarmListenerService`.
- Watch shows a high-priority actionable notification directly. By default it does not auto-launch the watch alarm activity, start a foreground ringing service, or hold a wake lock.
- To protect battery life, the watch uses only the notification channel's soft one-shot ramp vibration pattern and leaves Stop/Snooze as notification actions.
- The watch alarm activity is available when the user taps the notification, when the side-by-side hardware-key smoke test opens it explicitly, or as a fallback if Wear OS blocks the alarm notification.
- The optional watch alarm activity uses a centered, scrollable round-screen safe column so the `스누즈` button stays inside the visible circular display area.
- The optional watch alarm activity turns the screen on when opened but does not hold `FLAG_KEEP_SCREEN_ON`, so the display can time out normally if the user does not interact right away.
- The watch alarm notification uses the `shift_alarm_watch_alarm_v6` channel and retires older `v1`/`v2`/`v3`/`v4`/`v5` channels so updated soft ramp vibration settings apply after reinstalling the watch app.
- Watch keeps separate duplicate gates for alarm start and cancel events, so a fast stop/cancel event is not rejected as a duplicate of the just-received start event.
- Phone cancellation payloads keep the original alarm occurrence timestamp, and the watch ignores cancellation events that do not match the accepted start occurrence. If cancel arrives before start for the same occurrence, the watch records it and ignores a late start for that occurrence.
- Watch cancel teardown also requires the cancel timestamp to match the currently active watch alarm occurrence, so a late cancel cannot clear a newer notification or optional alarm screen for the same alarm id.
- Watch advertises the `shift_alarm_watch_control` Wear capability. The phone preview test checks this capability, so it can distinguish "a watch is connected" from "the matching Shift Alarm watch app is installed and reachable".
- Watch sends `/shift_alarm/alarm/ack` back to the phone only after it has actually shown either notification controls or the fallback alarm screen, and the ACK includes the display path it used. If both display paths fail, the watch clears the local active alarm record instead of leaving a phantom control target.
- The full validation script includes a notification-blocked fallback smoke test: it temporarily revokes the watch notification permission, verifies that the watch opens the fallback alarm screen and reports `displayMode=fallback`, then restores the permission. A valid fallback ACK also cancels the phone-side bridge fallback notification because the watch already has a usable alarm surface.
- Watch `끄기` sends `/shift_alarm/alarm/stop` back to the phone through both message and control `DataItem`.
- Watch `스누즈` sends `/shift_alarm/alarm/snooze` back to the phone with the original snooze payload through both message and control `DataItem`.
- Watch stop/snooze controls are sent in a short three-attempt burst, and notification actions keep their receiver alive briefly with `goAsync()` so a single transient Data Layer miss is less likely to lose the control.
- Phone receives watch controls in `WearAlarmControlListenerService` and routes them to the existing phone alarm service.
- Phone sends `/shift_alarm/alarm/control_ack` back after it accepts a watch stop/snooze command, so validation logs can prove the phone actually processed the watch button.
- If the first control ACK is lost, the phone replays the ACK for a recently accepted matching watch control so watch retry bursts can still finish cleanly after the phone alarm has already stopped or snoozed.
- Watch applies a control ACK only when its alarm id and occurrence timestamp match the currently active watch alarm, so a late ACK from a previous occurrence cannot dismiss a newer alarm.
- The optional watch alarm UI waits for that control ACK before closing; if the ACK does not arrive, it re-enables the buttons so the user can retry.
- The optional watch alarm UI also keeps a pending confirmation alarm notification visible while waiting for the phone control ACK.
- Watch notification actions also switch to a pending confirmation notification instead of disappearing immediately; the notification is cleared when the phone control ACK arrives.
- While switching to pending confirmation, the watch updates the notification in place instead of starting or stopping a foreground ringing service.
- If the phone control ACK does not arrive, the watch restores only the retryable notification/UI controls without starting a new local vibration or foreground service, so a disconnected phone does not make the watch keep re-ringing.
- If the optional watch alarm button or notification action times out waiting for the phone control ACK, the watch keeps the alarm actionable and lets the user retry the control without starting another long-running signal.
- The optional watch alarm screen maps delivered Home/Assist/stem/back/volume-style key events to stop or snooze, while consuming those keys so the alarm screen is not accidentally dismissed. If Wear OS reserves the physical Home key and does not deliver it to the app, the alarm stays active through the notification controls instead of treating Home as a stop command.
- Phone ignores stale watch controls unless the requested alarm id and alarm occurrence timestamp both match the alarm currently ringing on the phone.
- Phone de-duplicates message/DataItem control events by alarm cycle, so only the first `끄기` or `스누즈` command wins for a single alarm occurrence.
- Phone records the latest watch send attempt, watch ACK, accepted watch control, and rejected watch control reason, then shows them in the test area as `최근 워치 전송 시도`, `최근 워치 수신 확인`, `최근 워치 제어 처리`, and `최근 워치 제어 거부`.
- If the phone rejects a watch control because that exact alarm occurrence is no longer ringing, it sends a timestamp-matched cancel signal back without clearing the current active watch DataItem, so an orphaned old watch alarm can close without disturbing a newer alarm.

This does not call the private Samsung Clock alarm UI. The watch alarm UI is app-owned, because third-party apps cannot reliably open Samsung's native alarm screen with stop/snooze controls.

## Build outputs

Use side-by-side APKs for testing without replacing the currently installed production app:

- Phone: `app/build/outputs/apk/sideBySide/app-sideBySide.apk`
- Watch: `wear/build/outputs/apk/sideBySide/wear-sideBySide.apk`

Both side-by-side APKs use the matching package id suffix:

- `com.example.shiftalarmmvp.next`

The matching package id and signing key are important because Wear Data Layer delivery is app-scoped.
The watch module also declares the `shift_alarm_watch_control` capability in `wear/src/main/res/values/wear.xml`; if the phone preview says the watch is connected but the watch app is not confirmed, reinstall both APKs from the same build.

## Source preflight

Before installing to devices, run the source/build-output preflight. It checks that the phone and watch modules still agree on Wear Data Layer paths, package suffixes, listener services, foreground-service requirements, and side-by-side APK outputs:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\validate-watch-integration-source.ps1
```

## Install test APKs

### Automated install

The fastest path is the install helper script:

```powershell
.\scripts\install-watch-side-by-side.ps1
```

If more than one phone or watch is connected, pass serials explicitly:

```powershell
.\scripts\install-watch-side-by-side.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL
```

If APKs are already built:

```powershell
.\scripts\install-watch-side-by-side.ps1 -SkipBuild
```

The script builds both side-by-side APKs, auto-selects one connected phone and one connected watch when possible, installs them to the selected devices, grants notification permission when possible, verifies both installs, and launches both apps. If auto-selection cannot safely decide, run `adb devices -l` and pass `-PhoneSerial` and `-WatchSerial`.

By default, the install helper also runs the source preflight before install and runs device verification after install. If you need to skip those steps during debugging:

```powershell
.\scripts\install-watch-side-by-side.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -SkipPreflight -SkipVerify
```

You can still run the post-install verification directly. It checks that both devices have the same side-by-side package and that the watch serial is really a watch:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-watch-side-by-side.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -LaunchApps
```

### Manual install

1. Connect the phone with USB debugging.
2. On the Galaxy Watch, enable both `ADB debugging` and `Wireless debugging`.
3. If either device does not appear in `adb devices -l`, run the ADB readiness helper:

```powershell
.\scripts\diagnose-watch-adb.ps1
```

It lists ready ADB devices, identifies which connected device is the watch, shows discovered `_adb-tls` wireless debugging endpoints, and prints the next pairing/connect/full-validation command.

4. Connect to the watch with the helper:

```powershell
.\scripts\connect-watch-wireless.ps1
```

If the watch has not been paired with this PC yet, open `Wireless debugging > Pair new device with pairing code` on the watch and run:

```powershell
.\scripts\connect-watch-wireless.ps1 -PairAddress WATCH_IP:PAIR_PORT -PairCode PAIR_CODE
```

You can still connect manually if you already know the connect port:

```powershell
adb connect WATCH_IP:WATCH_PORT
adb devices -l
```

5. Install the phone APK:

```powershell
adb -s PHONE_SERIAL install -r app\build\outputs\apk\sideBySide\app-sideBySide.apk
```

6. Install the watch APK:

```powershell
adb -s WATCH_SERIAL install -r wear\build\outputs\apk\sideBySide\wear-sideBySide.apk
```

7. Open `교대알람 워치` once on the watch and allow notification permission if prompted.

## Manual verification

### Log capture

For the first physical test, collect phone and watch logs in parallel:

```powershell
.\scripts\collect-watch-alarm-logcat.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Clear
```

The logs are written under `manual-validation/watch-alarm/` and include these tags:

- `ShiftWatchTest`: side-by-side ADB smoke test trigger and send result.
- `ShiftWatchBridge`: phone-side send, ACK, stop, and snooze routing.
- `ShiftWearAlarm`: watch-side receive, display, ACK, and button actions.

### ADB smoke path

The side-by-side phone APK includes a test-only exported receiver, so a connected phone/watch pair can be smoke-tested without navigating the phone UI.

Full validation path:

```powershell
.\scripts\run-watch-full-validation.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL
```

If exactly one phone and one watch are connected through ADB, the serials can be omitted:

```powershell
.\scripts\run-watch-full-validation.ps1
```

This first self-tests the smoke assertion parser with synthetic stop/snooze logs, runs the app/watch watch-integration unit tests, builds and installs the side-by-side phone/watch APKs, verifies both packages, then runs preview delivery, notification-blocked fallback, orphaned watch alarm stale-control recovery, automated watch stop/snooze, and hardware-key stop/snooze smoke assertions. Hardware-key smoke opens the optional watch alarm screen through the side-by-side test receiver before injecting the key, because the battery-saving default is notification-only. Use this as the main pass/fail gate before treating the watch integration as verified on a physical Galaxy Watch. If a watch model or emulator cannot inject key events through ADB, pass `-SkipHardwareKeySmoke` and validate the physical buttons manually.

Preview delivery test:

```powershell
.\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Mode preview -Clear -Assert
```

Notification-blocked fallback test:

```powershell
.\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Mode preview -BlockWatchNotifications -ExpectedDisplayMode fallback -Clear -Assert
```

Full control round trip:

```powershell
.\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Mode control -Clear -WaitSeconds 45 -Assert
```

Automated watch stop round trip:

```powershell
.\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Mode stop -Clear -Assert
```

Automated watch snooze round trip:

```powershell
.\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Mode snooze -Clear -Assert
```

Orphaned watch alarm stale-control recovery:

```powershell
.\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Mode orphan -AutoWatchAction stop -Clear -Assert
```

Automated hardware-key watch stop round trip:

```powershell
.\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Mode stop -AutoWatchActionSource hardwareKey -Clear -Assert
```

Automated hardware-key watch snooze round trip:

```powershell
.\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Mode snooze -AutoWatchActionSource hardwareKey -Clear -Assert
```

During manual `control` mode, tap `스누즈` or `끄기` on the watch before the wait window ends. `-Mode stop` and `-Mode snooze` are direct automated round trips that start the phone alarm, send the matching watch action, and assert the expected result. With `-AutoWatchAction`, the side-by-side watch APK uses its current active alarm payload and sends the same internal stop/snooze action that the watch notification buttons use. The automatic action is retried briefly because phone-to-watch delivery can lag on real devices. Filtered phone and watch logs are saved under `manual-validation/watch-alarm/`.
With `-AutoWatchActionSource hardwareKey`, the script opens the optional watch alarm screen, injects `adb shell input keyevent`, and asserts that the alarm screen received the key before sending stop/snooze.

The `-Assert` flag runs `scripts/assert-watch-smoke-result.ps1` after log capture and fails if the logs do not prove delivery, display, ACK, and the expected watch control round trip. Use `-ExpectedAction snooze` or `-ExpectedAction stop` when you want to require one specific watch button.

### Watch signal preview

Use this first because it does not require waiting for a real alarm:

1. Open the phone test app.
2. Go to the alarm settings/test area.
3. Tap `워치 알람 미리보기 보내기`.
4. Expected phone result: the message reports the number of connected watch nodes and message-send attempts.
5. If the phone reports `연결된 워치가 없습니다`, confirm the watch app is installed, paired, and connected before continuing.
6. Expected watch result: watch shows an actionable `교대알람` notification with a short vibration. Tapping the notification opens the optional alarm screen.
7. Expected phone result: the test area shows `최근 워치 수신 확인` with the alarm label and ACK time.
8. Tap `끄기` or `스누즈` on the watch.
9. Expected watch result: the watch notification or optional alarm screen dismisses.
10. Expected phone result: the test area shows `최근 워치 제어 처리` with the selected action and time.

The preview only proves phone-to-watch delivery and the watch notification/control path. It does not prove phone alarm control, because no phone alarm is actively ringing during the preview.

### Full alarm control

Use this after the preview succeeds because it proves watch-to-phone control:

1. On the phone, tap `워치 끄기/스누즈 테스트 울리기`.
2. Expected phone result: phone alarm screen opens and alarm sound/vibration starts.
3. Expected watch result: watch shows an actionable `교대알람` notification with a short vibration. Tapping the notification opens the optional alarm screen.
4. Tap `스누즈` on the watch.
5. Expected phone result: phone alarm stops and schedules snooze with the configured snooze minutes/count.
6. After the snooze alarm rings again, tap `끄기` on the watch.
7. Expected phone result: `최근 워치 제어 처리` updates first to `스누즈`, then to `끄기`.
8. Expected phone result: phone alarm stops and the phone alarm screen dismisses.

This test alarm is not saved to the user's alarm list. It exists only to verify the phone alarm service, watch alarm screen, and watch control round trip.

## Automated verification

Run these checks before installing test APKs:

```powershell
.\gradlew.bat :app:testDebugUnitTest :wear:testDebugUnitTest
.\gradlew.bat :app:assembleSideBySide :wear:assembleSideBySide
```

The unit tests cover the phone/watch alarm payload contract, unsafe number clamping, cancellation parsing, and snooze-limit behavior. They do not replace the required physical phone-watch test because Wear Data Layer delivery, watch notification behavior, and optional alarm-screen hardware-key handling depend on the paired devices.

## Troubleshooting

- If the phone rings but the watch does nothing, confirm both APKs use the same package id variant. For side-by-side testing, both must be `.next`.
- If only a watch notification appears but no alarm screen opens, that is now the expected battery-saving default. Tap the notification if you want the optional watch alarm screen.
- If watch buttons do not stop the phone, confirm the phone and watch are paired and connected, then reinstall both APKs from the same build.
- Galaxy Watch physical Home/Back buttons are model/OS dependent. The app maps delivered Home/Assist/stem/back/volume-style key events to stop/snooze and logs ignored hardware keys from the alarm screen, but Wear OS may reserve Home before third-party apps can handle it. If Home is reserved on a device, use the on-screen watch buttons, notification actions, Back/stem keys that are delivered to the app, or the Ultra quick button if it emits a delivered key code.

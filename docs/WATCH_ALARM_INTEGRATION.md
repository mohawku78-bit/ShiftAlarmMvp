# Galaxy Watch alarm integration

This project now has a phone app module and a Wear OS companion module for alarm control.

## What it does

- Phone alarm starts in `AlarmRingingService`.
- Phone sends `/shift_alarm/alarm/start` to connected Wear OS nodes through `MessageClient`.
- Phone also writes `/shift_alarm/alarm/active` as an urgent `DataItem`, so the watch has a backup sync path if the immediate message is missed.
- Watch receives the message in `WatchAlarmListenerService`.
- Watch starts `WatchAlarmRingingService` as a foreground service so the high-priority alarm notification and repeating vibration can continue even if Wear OS blocks an immediate background activity launch.
- Watch also tries to open a full-screen custom alarm activity. If that launch is restricted, the notification remains as the fallback entry point.
- If foreground startup fails, the service leaves the fallback alarm notification visible instead of clearing it during service teardown.
- Watch keeps separate duplicate gates for alarm start and cancel events, so a fast stop/cancel event is not rejected as a duplicate of the just-received start event.
- Phone cancellation payloads keep the original alarm occurrence timestamp, and the watch ignores cancellation events that do not match the accepted start occurrence. If cancel arrives before start for the same occurrence, the watch records it and ignores a late start for that occurrence.
- Watch advertises the `shift_alarm_watch_control` Wear capability. The phone preview test checks this capability, so it can distinguish "a watch is connected" from "the matching Shift Alarm watch app is installed and reachable".
- Watch sends `/shift_alarm/alarm/ack` back to the phone with the display path it used: foreground vibration service or notification fallback.
- Watch `끄기` sends `/shift_alarm/alarm/stop` back to the phone through both message and control `DataItem`.
- Watch `스누즈` sends `/shift_alarm/alarm/snooze` back to the phone with the original snooze payload through both message and control `DataItem`.
- Phone receives watch controls in `WearAlarmControlListenerService` and routes them to the existing phone alarm service.
- Phone ignores stale watch controls unless the requested alarm id and alarm occurrence timestamp both match the alarm currently ringing on the phone.
- Phone de-duplicates message/DataItem control events by alarm cycle, so only the first `끄기` or `스누즈` command wins for a single alarm occurrence.
- Phone records the latest watch ACK, its display mode, and accepted watch control, then shows them in the test area as `최근 워치 수신 확인` and `최근 워치 제어 처리`.

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
.\scripts\install-watch-side-by-side.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL
```

If APKs are already built:

```powershell
.\scripts\install-watch-side-by-side.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -SkipBuild
```

The script builds both side-by-side APKs, installs them to the selected phone and watch, grants notification permission when possible, and launches both apps. If you do not know the serials, run the script without serials or run `adb devices -l`.

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
2. Enable wireless debugging or ADB debugging on the Galaxy Watch.
3. Connect to the watch with ADB if needed:

```powershell
adb connect WATCH_IP:WATCH_PORT
adb devices -l
```

4. Install the phone APK:

```powershell
adb -s PHONE_SERIAL install -r app\build\outputs\apk\sideBySide\app-sideBySide.apk
```

5. Install the watch APK:

```powershell
adb -s WATCH_SERIAL install -r wear\build\outputs\apk\sideBySide\wear-sideBySide.apk
```

6. Open `교대알람 워치` once on the watch and allow notification permission if prompted.

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

Preview delivery test:

```powershell
.\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Mode preview -Clear -Assert
```

Full control round trip:

```powershell
.\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL -Mode control -Clear -WaitSeconds 45 -Assert
```

During `control` mode, tap `스누즈` or `끄기` on the watch before the wait window ends. Filtered phone and watch logs are saved under `manual-validation/watch-alarm/`.
The `-Assert` flag runs `scripts/assert-watch-smoke-result.ps1` after log capture and fails if the logs do not prove delivery, display, ACK, and the expected watch control round trip. Use `-ExpectedAction snooze` or `-ExpectedAction stop` when you want to require one specific watch button.

### Watch signal preview

Use this first because it does not require waiting for a real alarm:

1. Open the phone test app.
2. Go to the alarm settings/test area.
3. Tap `워치 알람 미리보기 보내기`.
4. Expected phone result: the message reports the number of connected watch nodes and message-send attempts.
5. If the phone reports `연결된 워치가 없습니다`, confirm the watch app is installed, paired, and connected before continuing.
6. Expected watch result: watch shows the `교대알람` alarm screen and vibrates repeatedly.
7. Expected phone result: the test area shows `최근 워치 수신 확인` with the alarm label and ACK time.
8. Tap `끄기` or `스누즈` on the watch.
9. Expected watch result: the watch alarm screen dismisses.
10. Expected phone result: the test area shows `최근 워치 제어 처리` with the selected action and time.

The preview only proves phone-to-watch delivery and the watch UI/vibration path. It does not prove phone alarm control, because no phone alarm is actively ringing during the preview.

### Full alarm control

Use this after the preview succeeds because it proves watch-to-phone control:

1. On the phone, tap `워치 끄기/스누즈 테스트 울리기`.
2. Expected phone result: phone alarm screen opens and alarm sound/vibration starts.
3. Expected watch result: watch shows the `교대알람` alarm screen and vibrates repeatedly.
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

The unit tests cover the phone/watch alarm payload contract, unsafe number clamping, cancellation parsing, and snooze-limit behavior. They do not replace the required physical phone-watch test because Wear Data Layer delivery and full-screen watch UI behavior depend on the paired devices.

## Troubleshooting

- If the phone rings but the watch does nothing, confirm both APKs use the same package id variant. For side-by-side testing, both must be `.next`.
- If only a watch notification appears but no alarm screen opens, open the watch app once and grant notification permission. Some Wear OS builds restrict background activity starts, so the notification is also used as a fallback entry point.
- If watch buttons do not stop the phone, confirm the phone and watch are paired and connected, then reinstall both APKs from the same build.
- Galaxy Watch physical Home/Back buttons are system-controlled and should not be treated as reliable third-party alarm stop/snooze inputs. The implemented reliable controls are on-screen watch buttons and notification actions.

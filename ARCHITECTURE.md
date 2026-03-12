# ARCHITECTURE

## 목적
`ShiftAlarmMvp`의 아키텍처는 “교대 패턴을 왜곡 없이 다룬다”와 “다음 기상 알람이 실제로 안전한지 검증한다”를 동시에 만족해야 합니다.

앱은 크게 네 계층으로 나뉩니다.
- Pattern Layer: 기준일, intervalWeeks, 반복 시퀀스, 단일 날짜 override, 기간 override로 특정 날짜의 실제 근무 타입을 계산
- Planning Layer: 계산된 근무 결과를 바탕으로 다음 기상 알람 시점을 도출
- Schedule Layer: AlarmManager 및 플랫폼 이벤트를 사용해 실제 알람을 등록/검증/복구
- Reliability Layer: 권한, 배터리, 등록 상태, 점검 이력을 수집해 단일 진실 공급원(SSOT)으로 UI에 제공

## 제품 계약
이 앱의 핵심 질문은 다음 하나입니다.
- **내 다음 기상 알람이 지금 실제로 안전하게 등록되어 있는가?**

따라서 구조상 다음 원칙을 우선합니다.
- 기능 추가보다 알람 신뢰성 보장
- 설정 화면보다 감지 -> 검증 -> 즉시 복구
- 패턴 자유도와 신뢰도 검증을 동시에 유지

## 비가역 불변식
- Pattern/Planning 계산 로직은 Android 프레임워크에 의존하지 않습니다.
- 편집기, 프리셋, 복제, 백업, 미리보기, 스케줄 계산은 같은 normalize 규칙을 사용합니다.
- Reliability 상태는 중복 계산하지 않고 `ReliabilityCenterPolicy` / `ReliabilityCenterUiModel`만 참조합니다.
- 정보가 불충분하면 추측하지 않고 `미지정`으로 표기합니다.
- 모든 변경은 실패 상태, 방어 로직, 테스트를 포함한 production-ready 상태를 목표로 합니다.

## Layer Contract

### 1) Pattern Layer
책임
- anchor date, intervalWeeks, weekPatterns, override를 입력받아 특정 날짜의 근무 타입을 계산합니다.
- long rotation, single-date override, range override를 동일한 규칙으로 다룹니다.

제약
- `Context`, `AlarmManager`, `PendingIntent` 등 Android API 금지
- `java.time` 중심의 pure Kotlin 구현 유지
- JVM 단위 테스트만으로 검증 가능해야 함

주요 방향
- `normalizeIntervalWeeks`, `normalizeWeekPatterns`, `normalizeEditorPatternState`를 공통 진입점으로 유지
- override 충돌 시 merge/split/replace 정책을 명시적으로 관리

### 2) Planning Layer
책임
- Pattern Layer의 결과를 바탕으로 다음 기상 알람 시점을 계산합니다.
- “현재 + 24시간 millis” 같은 단순 덧셈이 아니라 wall-clock 규칙 기반으로 계산합니다.

제약
- DST 시작/종료, timezone 변경, 수동 시간 변경, long rotation, override를 모두 고려
- `Instant`, `ZonedDateTime`, `OffsetDateTime` 중심으로 계산

### 3) Schedule Layer
책임
- 실제 Android 알람 등록, 재등록, 재검증, self-healing을 담당합니다.
- boot, permission, time/timezone 변경 이후 재계산/재등록을 수행합니다.

제약
- exact alarm 예약 전 `canScheduleExactAlarms()` 확인
- `SecurityException`을 삼키지 않고 Reliability Layer에 실패 상태 보고
- one-shot exact alarm 전제를 두고 트리거 직후 다음 알람을 즉시 재예약
- 백그라운드 직접 `startActivity()` 금지, 필요 시 high-priority notification + full-screen intent 전략 검토

### 4) Reliability Layer
책임
- exact alarm permission, notification permission, battery risk, registered-next-alarm, recovery/self-test/nightly-check 상태를 한 모델로 합칩니다.
- 홈, 에디터, 위저드, 향후 위젯까지 같은 SSOT를 참조하게 합니다.

제약
- UI별 중복 판정 금지
- raw signal과 user-facing copy가 서로 모순되면 안 됨
- 실패 원인, 검증 시각, 즉시 복구 액션을 함께 설명

## 현재 저장소 기준 상태
- `v082`: editor/preset/backup/duplicate 흐름의 intervalWeeks 및 pattern normalization을 공통 helper로 정리
- `v083`: `ReliabilityCenterPolicy` / `ReliabilityCenterUiModel` 도입으로 홈 배너와 Editor step 3가 동일한 신뢰도 요약을 렌더링
- `v084`: app backup restore를 `preview -> confirm -> apply` 흐름으로 바꾸고 restore 후 preset/log/reliability refresh 및 stale UI state 정리를 추가

## 현재 리스크와 다음 P0
- exact alarm permission lifecycle self-healing
- timezone/DST/time changed 이후 전체 재계산 및 재등록
- watchdog + trigger history + missed-alarm self-healing
- restore 직후 권한/등록 상태를 새 기기 기준으로 다시 검증하는 강제 흐름

## 현재 코드와 목표 구조의 차이
- 현재 UI 편집 상태 일부는 `MainActivity`의 Compose local state에 남아 있습니다. 장기적으로는 ViewModel의 immutable `StateFlow` 중심 구조로 이동해야 합니다.
- restore 후 reliability refresh는 수행하지만, product contract 차원에서는 “새 기기 권한은 신뢰하지 않는다”는 더 강한 post-restore check contract가 아직 남아 있습니다.
- watchdog / trigger history / missed alarm self-healing은 P0 설계 대상으로 남아 있습니다.

## 참고 문서
- [README.md](README.md)
- [docs/ENGINEERING_CHARTER.md](docs/ENGINEERING_CHARTER.md)
- [TODO.md](TODO.md)

## 주요 파일
- `app/src/main/java/com/example/shiftalarmmvp/ui/MainActivity.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/EditorPage.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/AlarmViewModel.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/AppBackupCodec.kt`
- `app/src/main/java/com/example/shiftalarmmvp/recovery/ReliabilityCenterPolicy.kt`
- `app/src/main/java/com/example/shiftalarmmvp/scheduler/AlarmScheduler.kt`
- `app/src/main/java/com/example/shiftalarmmvp/receiver/NightlyReliabilityCheckReceiver.kt`

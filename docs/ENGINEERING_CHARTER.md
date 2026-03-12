# ENGINEERING CHARTER

## 1. Mission
`ShiftAlarmMvp`는 교대 근무자를 위한 초고신뢰성(Ultra-Reliability) 기상 알람 앱이다.
알람 누락은 단순 버그가 아니라 사용자의 금전, 고용, 사회적 손실로 이어질 수 있는 high-risk incident다.
모든 설계와 구현은 “기능 추가”보다 “알람 신뢰성 보장”을 우선한다.

## 2. Product Philosophy
- 핵심 철학은 `Reliability + Pattern Freedom`이다.
- 사용자의 가장 중요한 질문은 **내 다음 기상 알람이 지금 실제로 안전하게 등록되어 있는가?** 이다.
- UX 핵심은 단순 설정 화면이 아니라 `감지 -> 검증 -> 즉시 복구`다.

## 3. Layer Model

### Pattern Layer
입력
- 기준일
- 반복 시퀀스
- `intervalWeeks`
- 단일 날짜 override
- 기간 범위 override

출력
- 특정 날짜의 실제 근무 형태

계약
- pure Kotlin + `java.time`
- Android 프레임워크 의존 금지
- JVM 단위 테스트 가능해야 함

### Planning Layer
입력
- Pattern Layer의 근무 결과
- 알람 규칙

출력
- 다음 기상 알람 시점

계약
- `Instant`, `OffsetDateTime`, `ZonedDateTime` 중심 계산
- “현재 timestamp + 24시간 millis” 방식 금지
- DST / timezone / 수동 시간 변경 / long rotation / override 모두 고려

### Schedule Layer
책임
- AlarmManager를 이용한 실제 등록, 재등록, 검증, self-healing

계약
- 예약 전 `canScheduleExactAlarms()` 검사
- `SecurityException` 명시적 catch 후 Reliability Layer에 실패 보고
- one-shot exact alarm 전제를 두고 트리거 직후 다음 알람 즉시 재예약
- `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` 이후 재계산/재등록 수행
- 백그라운드 직접 `startActivity()` 금지

### Reliability Layer
책임
- exact alarm permission
- notification permission
- battery optimization risk
- registered-next-alarm 상태
- recovery / self-test / nightly-check 상태

계약
- `ReliabilityCenterPolicy` / `ReliabilityCenterUiModel`이 UI의 SSOT
- 홈, 에디터, 위저드, 향후 위젯이 서로 다른 판정 규칙을 가지면 안 됨
- 실패 원인, 마지막 검증 시각, 즉시 복구 액션을 함께 설명

## 4. Absolute Rules
1. PatternEngine과 근무 계산 로직은 Android 프레임워크에 의존하지 않는다.
2. UI 동적 상태는 장기적으로 ViewModel 내부 immutable `StateFlow` 중심으로 수렴시킨다.
3. Reliability 상태는 중복 계산하지 않는다.
4. editor / preset / duplicate / backup / preview / schedule 계산은 같은 normalize helper를 사용한다.
5. 정보가 불충분하면 추측하지 않고 `미지정`으로 표기한다.
6. 모든 변경은 임시방편이 아니라 production-ready 구현을 목표로 한다.

## 5. Platform Guardrails
- `targetSdk 35` 기준으로 방어적으로 구현한다.
- exact alarm은 `USE_EXACT_ALARM`을 기본 가정하지 않는다.
- `setExactAndAllowWhileIdle()` / `setAlarmClock()` 전략은 요구사항 기준으로 명시한다.
- Android 14+에서 exact alarm 권한은 신규 설치/복원 시 기본 거부 가능성을 고려한다.
- 복원 직후 새 기기 권한은 신뢰하지 않는다.
- time / timezone 변경 시 전체 재계산 및 재등록을 수행한다.

## 6. Time / Algorithm Guardrails
- wall-clock 의미를 잃는 단순 millis 덧셈 금지
- override 삽입 시 겹침 검사, merge / split / replace 정책 명시
- override, timezone 변경 시 관련 cache invalidate 보장
- long rotation과 single-date / range override가 섞여도 동일한 계산 결과 유지

## 7. Watchdog / Telemetry Guardrails
- `AlarmWatchdogWorker` 또는 동등한 주기적 self-check 메커니즘을 둔다.
- 예약 이력과 실제 트리거 이력을 비교할 수 있는 `TriggerHistory` 저장소를 설계한다.
- missed alarm 감지 시 지연 알람 또는 high-priority missed notification과 함께 원인 후보를 설명한다.
- `last_verified_at` 등 audit trail을 Reliability UI에 노출한다.
- Direct Boot 저장소가 필요하면 최소한의 데이터만 보관한다.

## 8. UX / UI Guardrails
- 홈 최상단 우선순위는 `Today's Shift -> Next Alarm -> Reliability Summary`
- 단순 “설정됨”으로 끝내지 않고 실제 검증 시각, 실패 원인, 복구 액션을 함께 표시
- 위험한 변경에는 confirmation gate / safety interrupt 적용
- 위험 상태는 색상만이 아니라 아이콘, 텍스트 라벨, 우선순위로 함께 표현
- tone은 smart, serious, sophisticated를 유지

## 9. Compose / Accessibility Guardrails
- 고비용 계산은 `remember`로 메모이즈한다.
- 고빈도 상태 기반 UI는 필요한 곳에 `derivedStateOf`를 사용한다.
- Lazy list / calendar / pattern list에는 안정적인 key를 제공한다.
- icon-only 또는 비가시 의미 요소에는 적절한 `contentDescription`을 제공한다.
- custom interactive component에는 semantics role을 명시한다.
- 신뢰성 경고/오류/상태 변경 영역에는 적절한 `liveRegion`을 적용한다.
- 문자열은 `strings.xml`을 기준으로 관리한다.

## 10. OEM Battery Guardrails
- `Build.MANUFACTURER`와 `Build.BRAND`를 함께 사용한다.
- 제조사별 배터리 가이드는 asset catalog 기반으로 파싱한다.
- 제조사 제한으로 deep link가 불안정하면 단계별 수동 가이드를 렌더링한다.
- 알 수 없는 제조사는 AOSP 기본 가이드로 fallback한다.

## 11. Backup / Recovery Guardrails
- backup/export/import는 pattern, override, alarm settings, 최소 audit data를 구조화된 포맷으로 다룬다.
- schema version을 반드시 포함한다.
- import 시 schema validation, migration, version mismatch 처리를 구현한다.
- restore 직후에는 권한 상태가 새 기기에서 보장되지 않는다고 가정한다.
- restore 완료 후 Reliability Check 단계로 강제 라우팅하거나, 동등한 수준의 blocking re-verification contract를 둔다.
- 성공, 부분 성공, 실패, 버전 불일치를 명확히 설명한다.

## 12. Documentation / Release Guardrails
출시 전 P0
- exact alarm lifecycle
- notification permission
- battery risk
- registered-next-alarm verification
- backup/recovery UX
- accessibility minimum
- privacy / data safety 준비

미지정으로 두는 범위
- 서버
- 웹
- iOS
- 로그인
- 클라우드 sync
- 구독
- 관리자 기능

## 13. Priority
- P0: exact alarm lifecycle, timezone/DST reschedule, watchdog/missed alarm self-healing, backup/recovery reliability gap
- P1: long rotation + override integrity, backup schema/migration, pure Kotlin test coverage
- P2: trust UX, audit trail, Compose accessibility/performance
- P3: widget/glance, OEM battery guide repository integration

## 14. Current Repo Truth
- `v082`: `normalizeEditorPatternState`를 도입해 draft/preset/duplicate/backup normalization을 중앙화했다.
- `v083`: `ReliabilityCenterPolicy` / `ReliabilityCenterUiModel`을 도입해 홈과 Editor step 3의 reliability summary를 공유했다.
- `v084`: app backup restore를 `preview -> confirm -> apply` 흐름으로 바꿨고 restore 후 preset/log/reliability refresh와 stale UI state 정리를 추가했다.

아직 남아 있는 핵심 갭
- restore 후 강제 Reliability Check contract
- exact alarm permission lifecycle self-healing
- watchdog / trigger history / missed alarm 복구
- timezone/DST/time changed full reschedule
- UI state의 ViewModel `StateFlow` 수렴

## 15. Working Response Contract
작업/응답 시에는 다음 순서를 지킨다.
1. 영향받는 계층, 파일, 리스크 요약
2. 설계 결정과 근거 설명
3. production-ready 코드 또는 패치 제안
4. 테스트 케이스, 회귀 포인트, 남은 `미지정` 항목 정리

코드 생성 시 항상 실패 시나리오와 방어 로직을 포함한다.

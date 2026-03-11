# ARCHITECTURE

## 앱 구조 개요
`ShiftAlarmMvp`는 크게 네 축으로 구성됩니다.
- 패턴을 저장하고 특정 날짜의 근무를 계산하는 패턴 계층
- 계산된 근무 결과로 다음 기상 알람 시점을 만드는 계획 계층
- Android 플랫폼에 실제 알람을 등록하고 점검하는 스케줄 계층
- 권한, 배터리, 등록 상태를 사용자에게 설명하는 신뢰도 계층

현재 제품 방향은 `신뢰도 + 패턴 자유도`를 중심에 두고 있습니다. 즉, 단순히 근무표를 보여주는 것이 아니라, 실제 근무 패턴을 왜곡 없이 표현하고 다음 알람이 안전하게 남아 있는지 끝까지 확인하는 구조를 우선합니다.

## 패턴 엔진 개념
예정 아키텍처 기준 핵심 개념은 `PatternEngine`입니다.
- 입력: 기준일, 반복 시퀀스, 예외 날짜 override
- 출력: 특정 날짜의 실제 근무 타입
- 역할: 저장 형식과 UI 입력 방식이 달라도 계산 규칙은 한 곳에서 일관되게 유지

이 계층이 안정적이어야 장기 로테이션, 프리셋, 편집기 미리보기, 다음 알람 계산이 모두 같은 결과를 공유할 수 있습니다. 최근에는 예외 날짜 override를 충돌 없는 모델로 정규화해 skip/add가 겹치지 않도록 정리했고, 저장/로드/미리보기/스케줄 계산이 같은 규칙을 쓰도록 맞췄습니다.

## 반복 시퀀스 모델
반복 시퀀스 모델은 “몇 주짜리 로테이션인가”보다 “실제 사용자의 근무 흐름을 얼마나 그대로 담을 수 있는가”를 우선합니다.
- 2~6 슬롯 같은 하드코딩 제한을 줄이고 가변 길이 패턴을 지원
- 6주를 넘는 장기 로테이션도 저장/로드/계산에서 유지
- 편집 화면에서는 빠른 선택과 직접 입력을 함께 제공
- 특정 날짜 1회성 수정은 override 모델로 별도 관리

이 모델의 목적은 `주/당/비/휴/당/비` 같은 복합 패턴이나 더 긴 주기를 왜곡 없이 다루는 것입니다. 반복 시퀀스는 앱 전반의 기준 데이터이므로, 에디터와 스케줄러가 각자 해석하지 않고 같은 계산 로직을 바라보는 구조가 중요합니다.

## 알람 신뢰도 시스템
알람 신뢰도 시스템은 “알람 설정 화면을 한 번 열어봤다”가 아니라 “다음 기상 알람이 지금 실제로 안전한가”를 판단하는 흐름입니다.
- exact alarm 권한 상태 확인
- `POST_NOTIFICATIONS` 권한 상태 확인
- 시스템에 다음 알람이 실제 등록돼 있는지 확인
- 마지막 점검/복구 시각 기록
- 배터리 제한 위험 여부 및 제조사별 안내 제공
- 필요한 경우 재설정/재예약 액션으로 즉시 복구 유도

예정 아키텍처 기준으로는 `ReliabilityInspector`가 권한, 배터리 상태, 마지막 점검, 복구 여부, 테스트 결과를 모아 UI 상태로 변환합니다. `ReliabilitySetupPolicy`와 홈/편집 신뢰도 패널은 이 상태 모델을 공유해 같은 우선순위와 같은 문구로 다음 조치를 설명합니다.

## 주요 컴포넌트 역할
- `PatternEngine`: 패턴, 기준일, override를 받아 특정 날짜 근무를 계산
- `AlarmPlanner`: 근무 결과와 알람 규칙을 조합해 필요한 알람 시점을 계산
- `AlarmScheduler`: Android 플랫폼 API 호출 담당, 실제 기상 알람은 exact alarm 중심으로 관리
- `ReliabilityInspector`: 권한, 배터리 상태, 마지막 점검, 복구 여부, 테스트 결과를 모아 UI 상태로 변환
- `MainActivity` / 홈 UI: 오늘 근무, 다음 알람, 신뢰도 상태를 가장 먼저 보여주는 진입점
- `EditorPage`: 패턴/알람 규칙 편집과 신뢰도 상세 안내를 함께 제공
- `ShiftPage`: 첫 설정 위저드와 패턴 입력 흐름을 담당
- `NightlyReliabilityCheckReceiver`: 취침 전 자동 점검과 후속 알림 트리거 담당

## 기술 원칙
- Exact alarm 경로는 한 가지로 고정하지 않고 `USE_EXACT_ALARM` 배포 전략과 `SCHEDULE_EXACT_ALARM` 사용자 부여 경로를 함께 고려합니다.
- 배터리 최적화 예외는 기본 온보딩 필수 단계가 아니라, 신뢰도 패널에서 위험 징후가 보일 때 제안하는 복구 수단으로 다룹니다.
- 신뢰도는 포그라운드 서비스 유지보다 exact alarm 중심으로 설계하고, 점검/복구/로그 갱신은 보조 경로로 분리합니다.
- predictive back/애니메이션보다 먼저 단계 복귀 정확성과 입력값 보존을 우선합니다.
- 현행 기준은 `targetSdk 35`이며, exact alarm, 알림 권한, FGS, fullscreen intent, back 동작을 모두 최신 타깃 SDK 기준으로 판단합니다.

## 참고 파일
- `app/src/main/java/com/example/shiftalarmmvp/ui/MainActivity.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/EditorPage.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/ShiftPage.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/SelfTestActionHandler.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/HomeReliabilityUiState.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/HomeReliabilityChip.kt`
- `app/src/main/java/com/example/shiftalarmmvp/recovery/ReliabilitySetupPolicy.kt`
- `app/src/main/java/com/example/shiftalarmmvp/scheduler/AlarmScheduler.kt`
- `app/src/main/java/com/example/shiftalarmmvp/receiver/NightlyReliabilityCheckReceiver.kt`
- `app/src/main/java/com/example/shiftalarmmvp/recovery/ReliabilityInspector.kt`
- `app/src/main/java/com/example/shiftalarmmvp/recovery/NightlyReliabilityCheckStore.kt`

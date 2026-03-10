# ShiftAlarmMvp

## 프로젝트 한눈에
`ShiftAlarmMvp`는 Android Kotlin + Compose 기반 교대근무 알람 앱입니다.
- 핵심 가치는 **실사용자가 신뢰할 수 있는 알람 동작**입니다.
- 기준 날짜: 2026-03-10
- 현재 단계: MVP 이후, 신뢰도 루프/패턴 확장 단계

## 현재 상태 (2026-03-10)
- 홈에서 오늘 근무, 다음 알람, 신뢰도 상태를 확인할 수 있습니다.
- 패턴/알람/복구/2분 테스트 상태가 홈 신뢰도 패널로 연결되어 있습니다.
- 취침 전 자동 점검(스케줄러 + 리시버 + 알림)이 동작합니다.
- 제조사별 배터리 진입 가이드 및 폴백 경로가 연결되어 있습니다.
- 홈 배너 버전: `v060`
- 변경 내용은 아래 작업 기록 규칙 형식으로 누적 기록합니다.

## 내일(다음 실행) 우선순위

### 1) 뒤로가기 동작 우선 처리 (가장 중요)
- 목표: 위저드 3단계에서 뒤로가기 시 앱 종료가 아니라 이전 단계로 이동
- 변경 범위(우선 적용 대상)
  - `EditorPage`: `step` 상태를 이용한 `BackHandler` 적용
  - `ShiftPage`: `wizardStep` 상태를 이용한 `BackHandler` 적용
- 완료 기준
  - 3단계에서 뒤로가기 누르면 2단계로 복귀
  - 2단계에서 뒤로가기 누르면 1단계로 복귀
  - 1단계에서 뒤로가기 동작은 기존 동작(종료/상위화면 이동) 유지

### 2) 근무 형태 확장성 복구 (제한 해제)
- 현재 문제 인식
  - 기본 슬롯 또는 2~6주 제한으로 복잡한 패턴이 느슨하게 강제됨
  - 단순 패턴(2~3주) 중심 UI가 있고, 장기 주기 패턴에서 제약이 생김
- 목표
  - 주/복잡 근무 형태 제한을 단계적으로 해제
  - 회전 슬롯 수를 고정 2~6으로 두지 말고, 실제 사용자 패턴 길이에 맞춰 확장
  - 기존 v060의 6주 슬롯 확장 방향을 "기본 동작 유지 + 확장형 입력"으로 이행
- 적용 순서
  1. 현재 주기/슬롯 상한값이 하드코딩되어 있는 위치를 식별
  2. UI(드롭다운/선택칩)에서 최대/최소 제한을 완화
  3. 계산기 쪽에서 패턴 길이 기반 슬롯 검증 로직 보강
  4. 장기 패턴 저장/로드/미리보기 테스트 케이스 보강

### 3) 패턴 엔진 회귀 점검
- 목표: 확장 패턴이 다음 항목에서 깨지지 않음
  - 저장/로드
  - 스케줄 계산
  - 다음 알람 미리보기(2/3/10 회차)
  - 2분 테스트/재점검 로그
- 완료 기준
  - 복잡 패턴 시나리오 3개 이상으로 수동 확인
    - 예: `주/당/비/휴/당/비`, 장기 5개+ 패턴, 사용자 직접 입력 패턴

## 다음 스프린트 실행 순서
1. `뒤로가기` 동작 수정 코드 적용
2. `근무 패턴 제한 해제` 설계 확정(회전 슬롯 규칙 문서화)
3. 코드 수정 + 테스트 데이터/테스트 코드 보강
4. UI/기능 회귀 확인 체크리스트 수행
5. README 업데이트(이번 항목에 완료일/담당/검증 결과 반영)

## 실행 체크리스트(다른 PC에서 바로 착수)
- [ ] `main` 기준으로 브랜치/헤드 확인
- [ ] 위저드 단계 상태(`step`, `wizardStep`)에 대한 `BackHandler` 규칙 정의
- [ ] 근무 슬롯 제한 하드코딩 지점 정리
- [ ] 제한 해제 후 동작 테스트 항목 3개 이상 통과
- [ ] 변경 로그에 `작업일/작업 내용/검증 결과` 기록

## 지금까지 완료된 핵심 개선 요약
- `v060` 복잡 패턴(예: `주/당/비/휴/당/비`) 정확도 개선
  - 슬롯 상한을 6주까지 확장하고 저장/로드/계산/프리셋/에디터 간 값 정합성 맞춤
  - 84일 위상 테스트 케이스 추가
- `v059`~`v055`까지 화면 가독성, 배터리 가이드, 신뢰도 액션 개선
- `v054`~`v052` 복구 로그/조치 가시성 개선
- `v049`~`v047` 컴파일 안정성 관련 `dp` 참조 정리
- `v046`~`v041` 문구·로직 분리 및 신뢰도 UX 정리
- `v040`~`v026` 재점검/테스트/취침전 점검 루프 기반의 기반 기능 추가

## 참고 파일
- `app/src/main/java/com/example/shiftalarmmvp/ui/MainActivity.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/EditorPage.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/ShiftPage.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/SelfTestActionHandler.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/HomeReliabilityUiState.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/HomeReliabilityChip.kt`
- `app/src/main/java/com/example/shiftalarmmvp/receiver/NightlyReliabilityCheckReceiver.kt`
- `app/src/main/java/com/example/shiftalarmmvp/recovery/ReliabilityInspector.kt`
- `app/src/main/java/com/example/shiftalarmmvp/recovery/NightlyReliabilityCheckStore.kt`

## 검증 메모(최소 기준)
- 빌드 체크: `:app:compileDebugKotlin`, `:app:assembleDebug`
- 동작 체크: 뒤로가기, 위저드 패턴 입력, 배터리 가이드 진입, 신뢰도 패널

## 작업 기록 규칙 (계속 유지)
- 코드 변경 시 작업 기록 규칙 항목에 같은 날짜로 한 줄 이상 누적
- 각 항목은 반드시 포함
  - 식별자(버전/커밋)
  - 사용자 관점 동작 변화
  - 확인 항목(빌드/실기기/UI)
  - 남은 리스크


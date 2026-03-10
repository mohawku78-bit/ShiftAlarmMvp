# ShiftAlarmMvp

## 프로젝트 한눈에
`ShiftAlarmMvp`는 Android Kotlin + Compose 기반 교대근무 알람 앱입니다.
- 핵심 가치는 **실사용자가 신뢰할 수 있는 알람 동작**입니다.
- 기준 날짜: 2026-03-11
- 현재 단계: MVP 이후, 신뢰도 루프/패턴 확장 및 실기기 회귀 점검 단계

## 현재 상태 (2026-03-11)
- 홈에서 오늘 근무, 다음 알람, 신뢰도 상태를 확인할 수 있습니다.
- 패턴/알람/복구/2분 테스트 상태가 홈 신뢰도 패널로 연결되어 있습니다.
- 취침 전 자동 점검(스케줄러 + 리시버 + 알림)이 동작합니다.
- 제조사별 배터리 진입 가이드 및 폴백 경로가 연결되어 있습니다.
- 위저드 단계 뒤로가기가 이전 단계로 안전하게 복귀합니다.
- 6주를 넘는 장기 로테이션도 저장/로드/프리셋/스케줄 계산에서 유지됩니다.
- 편집 화면에서 1/2/3/4/6/8/12주 빠른 선택과 직접 입력으로 장기 주기를 설정할 수 있습니다.
- 홈 배너 버전: `v062`
- 변경 내용은 아래 작업 기록 규칙 형식으로 누적 기록합니다.

## 내일(다음 실행) 우선순위

### 1) 실기기 장기 패턴 편집 회귀 확인
- 목표: 8주/13주 이상 패턴에서도 편집, 저장, 뒤로가기 흐름이 끊기지 않음
- 확인 범위
  - `EditorPage`에서 빠른 선택 버튼과 직접 입력이 모두 정상 반영되는지
  - `ShiftPage` 첫 설정 위저드에서 뒤로가기와 단계 이동이 자연스러운지
  - 편집 후 홈의 다음 알람/오늘 근무 카드가 기대대로 갱신되는지
- 완료 기준
  - 장기 패턴 3개 이상을 실기기에서 저장/재진입/수정까지 확인

### 2) Exact Alarm 권한 온보딩 상태기계 정리
- 목표: Android 14+에서도 사용자에게 필요한 권한 흐름을 한 번에 안내
- 변경 범위
  - `USE_EXACT_ALARM` / 설정 진입 / 실패 시 대체 안내를 단계별로 노출
  - 홈 신뢰도 패널과 편집 화면의 권한 메시지 중복을 정리
- 완료 기준
  - 권한 거부/허용/재진입 시나리오별 문구와 액션이 명확함

### 3) 제조사 배터리 예외 가이드 고도화
- 목표: 삼성/샤오미/기타 OEM에서 배터리 최적화 해제 경로를 더 직접적으로 안내
- 변경 범위
  - 제조사별 진입 문구/폴백 경로 점검
  - 실패 시 앱 상세 설정으로 안전하게 복귀하는 흐름 보강
- 완료 기준
  - 주요 제조사 2종 이상에서 경로 문구와 버튼 동작을 수동 검증

## 다음 스프린트 실행 순서
1. 실기기에서 장기 패턴 편집/저장/뒤로가기 시나리오 확인
2. Exact Alarm 권한 온보딩 상태기계 정리
3. 제조사 배터리 예외 가이드 문구 및 진입 경로 점검
4. 홈 신뢰도 패널 문구와 편집 화면 액션 정합성 확인
5. README 업데이트(이번 항목에 완료일/검증 결과 반영)

## 실행 체크리스트(다른 PC에서 바로 착수)
- [ ] `main` 기준으로 브랜치/헤드 확인
- [ ] 장기 패턴(8주/13주 이상) 저장-재진입-수정 흐름 확인
- [ ] Exact Alarm 권한 허용/거부/재진입 시나리오 점검
- [ ] 제조사 배터리 예외 가이드 버튼 동작 점검
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

## 작업 기록
- 2026-03-11 `v062`: 편집 화면의 로테이션 주기 선택을 1/2/3/4/6/8/12주 빠른 버튼과 직접 숫자 입력으로 확장해 장기 패턴을 UI에서도 바로 설정할 수 있게 수정. 확인 항목: `:app:compileDebugKotlin -x kspDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug`. 남은 리스크: 실기기에서 긴 숫자 입력과 스크롤 동작의 체감 확인 필요.
- 2026-03-11 `v061`: 위저드 뒤로가기와 장기 로테이션 상한 제거를 적용해 13주 이상 패턴도 저장/로드/프리셋/스케줄 계산에서 유지되도록 수정. 확인 항목: `:app:testDebugUnitTest`, `:app:compileDebugKotlin -x kspDebugKotlin`. 남은 리스크: 실기기에서 장기 패턴 편집 UI와 예측형 뒤로가기 체감 확인 필요.

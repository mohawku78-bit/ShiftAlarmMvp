## 내일 이어하기 체크리스트 (복붙용)

### 1) 환경 동기화
- `git pull`
- `git log --oneline -n 5`에서 최근 커밋 확인
  - `2df8b38 chore: save today's work (non-readme) for handoff`
  - `36d02c8 docs: update tomorrow handover plan for back navigation and rotation expansion`

### 2) 오늘 작업 동기 요약 파악
- `README.md`에서 "내일(다음 실행) 우선순위" 확인
- 핵심 작업 3개
  - 뒤로가기 동작을 이전 단계로 복귀
  - 근무 슬롯 제한 해제 설계
  - 패턴 엔진 회귀 점검

### 3) 먼저 확인할 실행 흐름
- 홈: 신뢰도 패널 정상 동작
- 패턴 탭(기초): 설정/미리보기
- Editor: 단계형 편집 흐름
- 2분 테스트/복구 연동

### 4) 수정할 대상 파일 맵
- `app/src/main/java/com/example/shiftalarmmvp/ui/EditorPage.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/ShiftPage.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/MainActivity.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/PageModels.kt`
- `app/src/main/java/com/example/shiftalarmmvp/scheduler/AlarmTimeCalculator.kt`
- `app/src/main/java/com/example/shiftalarmmvp/recovery/ReliabilityInspector.kt`
- `app/src/main/java/com/example/shiftalarmmvp/recovery/ReliabilityStateCoordinator.kt`
- `app/src/main/java/com/example/shiftalarmmvp/recovery/NightlyReliabilityCheckStore.kt`
- `app/src/main/java/com/example/shiftalarmmvp/scheduler/NightlyReliabilityCheckScheduler.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/SelfTestActionHandler.kt`

### 5) 오늘 코드 변경 기준으로 꼭 살펴볼 포인트
- 위저드 단계 네비: `step`, `wizardStep` 상태 반응성
- 뒤로가기 동작 정책(우선순위/예외 포함)
- 근무 슬롯(주/반복 길이) 상한 처리 위치
- 복잡 패턴 저장/로드/미리보기가 일관된지

### 6) 내일 작업 완료 조건
- 3단계 상태에서 뒤로가기로 이전 단계 전환 확인
- 제한 해제 설계안 정리 후 최소 1개 패턴 입력 폼 반영
- 패턴 복잡도 테스트 3개 케이스 수동/단위 테스트 적용
- 변경 사항을 README에 작업 내역으로 1회 기록
- `:app:compileDebugKotlin`, `:app:assembleDebug` 실행

### 7) 다음 브리지 액션
- 필요 시 이번 커밋 기준으로 작은 단위 브랜치 분기
- 작업 후에는 `git commit -am "feat: ..."` 형태로 이어서 push

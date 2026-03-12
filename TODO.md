# TODO

## 현재 상태
- 핵심 알람 신뢰성 기능은 대부분 완료 단계입니다.
- 지금 남은 일은 새 엔진 기능 추가보다 최종 제품 QA와 사용자용 정리입니다.
- 최근 마일스톤
  - exact alarm lifecycle self-healing
  - timezone / DST / wall-clock invalidation
  - watchdog + trigger history + missed alarm recovery
  - backup restore preview / confirm / apply
  - restore post-check reliability contract
  - shared reliability center summary
  - first-setup wizard regression recovery

## 다음 순서

### 1. 최종 실기기 QA
- [ ] 삼성에서 수동 `오늘 알람` 테스트 후 원래 근무표로 돌아왔을 때도 `next alarm registration`이 안정적으로 맞는지 다시 확인
- [ ] seeded QA 상태가 아니라 실제 일상 흐름으로 앱 백업 복구를 한 번 더 검증
- [ ] 실기기 상태 변화 뒤에도 Home `Alarm Check`와 Editor step 3가 같은 이유와 액션을 보여주는지 확인

### 2. 삼성 안내 문구 보강
- [ ] `앱 정보 > 배터리 > 제한 없음`을 사용자 문구에 명시
- [ ] 삼성 `백그라운드 앱 사용 제한` 화면에서 무엇을 해야 하는지 설명 추가
- [ ] 배터리 / 특수 접근 설정에서 앱으로 돌아온 뒤 무엇을 다시 확인해야 하는지 안내 보강

### 3. 사용자용 용어 정리
- [ ] `strings.xml`에서 기술적인 표현을 쉬운 한국어 UI 문구로 정리
- [ ] 내부 코드명과 저장 스키마 이름은 유지
- [ ] 문구가 쉬워져도 신뢰성 경고 톤은 유지

### 4. 디자인 / 레이아웃 정리
- [ ] Home 정보 우선순위 `Today's Shift -> Next Alarm -> Alarm Check` 다시 확인
- [ ] 카드 간격, 버튼 강조, 시각적 계층 정리
- [ ] reliability center 가독성 개선

## 중요하지만 급하지 않은 항목

### Reliability / Platform
- [ ] 삼성 외 OEM 배터리 QA
- [ ] release checklist: privacy / data safety / QA matrix 정리

### Pattern / Data
- [ ] legacy preset import fallback 동작 재검토
- [ ] backup schema versioning / migration 규칙 보강 여부 결정

### Quality
- [ ] manual QA 의존 구간의 pure Kotlin coverage 확장
- [ ] release candidate용 최종 회귀 체크리스트 추가

## 최근 완료
- [x] shared enabled-alarm rescheduler로 exact permission, boot, time, timezone, restore, manual reschedule 통합
- [x] exact alarm 불가 시 degraded fallback 경로 정리
- [x] 자연 DST 경계 검증 완료
- [x] watchdog + trigger history + missed-alarm self-healing 추가
- [x] restore post-check를 shared reliability center로 연결
- [x] 수동 재예약 피드백과 완료 처리 보강
- [x] first-setup wizard back / close / select-then-next 회귀 복구
- [x] 2분 테스트 예약 시각을 초 단위로 표시

# ShiftAlarmMvp

`ShiftAlarmMvp`는 교대근무자를 위한 Android Kotlin + Compose 알람 앱입니다. 이 저장소의 우선순위는 기능 개수보다 신뢰성입니다. 다음 기상 알람이 실제로 등록돼 있는지, 시간이나 권한이 바뀐 뒤에도 계속 믿을 수 있는지, 문제가 생기면 앱이 스스로 복구할 수 있는지가 핵심입니다.

## 제품 초점
- `Reliability + Pattern Freedom`
- 핵심 UX: `감지 -> 검증 -> 즉시 복구`
- 핵심 질문: `내 다음 기상 알람이 지금 안전하게 등록돼 있나?`

## 현재 로컬 상태
- 기준 날짜: 2026-03-12
- 현재 로컬 마일스톤: `v089`
- 핵심 신뢰성 작업은 대부분 닫힌 상태입니다.
  - exact alarm lifecycle self-healing
  - wall-clock / timezone / DST invalidation
  - watchdog + trigger history + missed alarm self-healing
  - backup restore preview / confirm / apply
  - restore post-check reliability contract
  - Home / Editor step 3 shared reliability summary
  - first-setup wizard regression recovery
- 최근 검증
  - 삼성 실기기에서 백업 복구, 수동 재예약, 위저드 회귀, 2분 테스트 피드백 확인 완료
  - 자연 DST 경계 에뮬레이터 검증 완료: spring-forward, fall-back 모두 통과

## 앱이 하는 일
- 홈 상단에서 `Today's Shift`, `Next Alarm`, `Alarm Check`를 우선 확인할 수 있습니다.
- 반복 패턴, 장기 로테이션, 특정 날짜 변경, 기간 범위 변경을 함께 다룹니다.
- 고정된 일일 알람이 아니라 근무 규칙에서 다음 기상 알람을 계산합니다.
- exact alarm, notification permission, battery risk, registered-next-alarm 상태를 하나의 shared reliability model로 설명합니다.
- 부팅, 시간 변경, 시간대 변경, 복원, exact permission 변경 뒤 self-healing 재예약을 수행합니다.
- recovery와 watchdog 이력을 남겨서 알람 누락 가능성을 설명할 수 있게 합니다.
- 알람, 저장한 근무표, 최근 로그를 로컬 JSON으로 백업하고 복원할 수 있습니다.

## 현재 우선순위
1. 삼성 실기기에서 정상 근무표 기준 `next alarm registration` 최종 QA
2. 삼성 배터리 / exact alarm 안내 문구 보강
3. 사용자용 용어 정리
4. 디자인과 레이아웃 polish

## 문서 읽기 순서
1. [README.md](README.md): 제품 개요와 현재 상태
2. [ARCHITECTURE.md](ARCHITECTURE.md): 계층 경계와 불변식
3. [docs/ENGINEERING_CHARTER.md](docs/ENGINEERING_CHARTER.md): 신뢰성 규칙, 플랫폼 제약, UX 가드레일
4. [TODO.md](TODO.md): 남은 작업 우선순위
5. [CHANGELOG.md](CHANGELOG.md): 최근 마일스톤과 검증 기록

## 빌드와 검증
Android Studio JBR 또는 주입된 `JAVA_HOME`을 사용합니다. 머신별 `org.gradle.java.home`는 다시 커밋하지 않습니다.

```powershell
./gradlew.bat :app:compileDebugKotlin -x kspDebugKotlin
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :app:assembleDebug
```

## 저장소 메모
- Reliability 상태는 화면마다 다시 계산하지 않고 shared policy / model 경로에서 읽습니다.
- Pattern 계산 로직은 pure Kotlin + `java.time` 경계를 유지합니다.
- 앱 백업 복원은 현재 `replace-all` 계약을 유지합니다.
- 사용자용 문구는 더 쉽게 바꿀 수 있지만, 신뢰성 경고는 분명하게 남겨야 합니다.

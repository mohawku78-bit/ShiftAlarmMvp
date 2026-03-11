# ShiftAlarmMvp

## 프로젝트 소개
`ShiftAlarmMvp`는 Android Kotlin + Compose 기반 교대근무 알람 앱입니다.
- 핵심 가치는 **실사용자가 신뢰할 수 있는 알람 동작**입니다.
- 제품 정의: **교대근무자의 실제 패턴을 표현하고, 다음 기상 알람이 안전하게 등록돼 있는지 끝까지 확인해주는 앱**
- 기준 날짜: 2026-03-11
- 현재 단계: MVP 이후, **신뢰도 + 패턴 자유도** 축으로 제품 방향을 고정하고 실기기 회귀를 정리하는 단계

## 핵심 기능
- 홈에서 오늘 근무, 다음 알람, 신뢰도 상태를 한 번에 확인
- 반복 패턴, 장기 로테이션, 예외 날짜 override를 함께 관리
- 취침 전 자동 점검과 복구 흐름으로 알람 누락 위험 감지
- 정확 알람, 알림 권한, 배터리 예외 상태를 신뢰도 패널로 안내
- 제조사별 배터리 가이드 및 알람 재예약 액션 제공
- 앱 백업 JSON으로 현재 알람 규칙, 프리셋, 최근 로그를 저장하고 복구

## 기술 스택
- Kotlin
- Jetpack Compose
- Android SDK (`targetSdk 35` 기준)
- Android AlarmManager / BroadcastReceiver 기반 알람 스케줄링
- JUnit 기반 단위 테스트

## 프로젝트 구조
- `app/src/main/java/com/example/shiftalarmmvp/ui`: 홈, 편집, 위저드, 신뢰도 UI
- `app/src/main/java/com/example/shiftalarmmvp/data`: 패턴/알람 규칙 저장 모델
- `app/src/main/java/com/example/shiftalarmmvp/scheduler`: 다음 알람 계산 및 플랫폼 스케줄 등록
- `app/src/main/java/com/example/shiftalarmmvp/recovery`: 권한/배터리/점검 상태 기반 신뢰도 정책
- `app/src/main/java/com/example/shiftalarmmvp/receiver`: 야간 점검 및 알람 관련 리시버
- `app/src/main/assets`: 제조사별 배터리 최적화 가이드 JSON
- `TODO.md`: 현재/다음 스프린트와 아이디어 관리
- `CHANGELOG.md`: 버전별 변경 기록
- `ARCHITECTURE.md`: 패턴 엔진, 반복 시퀀스 모델, 신뢰도 시스템 설명

## 빌드 방법
1. Android Studio에서 프로젝트를 엽니다.
2. JDK는 Android Studio 기본 JBR을 사용합니다.
3. 아래 Gradle 작업으로 기본 검증을 실행합니다.

```powershell
./gradlew.bat :app:compileDebugKotlin -x kspDebugKotlin
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :app:assembleDebug
```

## 현재 버전
- 현재 버전: `v080`
- 최신 변경 기록: [CHANGELOG.md](CHANGELOG.md)
- 현재 작업 계획: [TODO.md](TODO.md)
- 구조 설명: [ARCHITECTURE.md](ARCHITECTURE.md)

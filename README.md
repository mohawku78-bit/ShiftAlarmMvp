# ShiftAlarmMvp

교대근무자를 위한 2~3주 주차별 요일 로테이션 알람 MVP입니다.

예시:
- 1주차: 월/수/금/일
- 2주차: 화/목/토
- (3주 로테이션이면 3주차도 별도 요일 선택)

## 핵심 기능
- 로테이션 길이 선택(2주/3주)
- 주차별 독립 요일 선택
- 기준 시작일(anchor date) 지정
- AlarmManager 정확 알람 예약
- 알람 발생 시 다음 회차 자동 재예약
- 재부팅 후 알람 복구

## 반복 계산 규칙
- 기준일(anchor) 주를 1주차로 간주
- 현재 날짜가 로테이션의 몇 주차 슬롯인지 계산
- 해당 슬롯에 선택된 요일이면 알람 후보
- 가장 가까운 미래 시각을 예약

## 주요 파일
- `app/src/main/java/com/example/shiftalarmmvp/data/AlarmRuleEntity.kt`
- `app/src/main/java/com/example/shiftalarmmvp/scheduler/AlarmTimeCalculator.kt`
- `app/src/main/java/com/example/shiftalarmmvp/ui/MainActivity.kt`

## 다른 장소에서 이어 개발하기

### 1) Git 사용 (권장)
현재 PC:
1. 변경 저장
2. 커밋
3. 원격 저장소로 push

새 PC:
1. 저장소 clone
2. Android Studio에서 프로젝트 열기
3. Gradle Sync
4. `./gradlew.bat :app:assembleDebug`로 빌드 확인

### 2) ZIP/외장디스크 복사 (간단)
1. `ShiftAlarmMvp` 폴더 전체 복사
2. 새 PC에서 Android Studio로 열기
3. 첫 실행 시 Gradle/SDK 재다운로드 후 Sync

권장 제외 항목(복사 용량 절약):
- `.gradle/`
- `build/`
- `app/build/`

### 3) 장비별로 다시 설정되는 항목
- `local.properties` (SDK 경로): PC마다 자동/수동 재생성
- 에뮬레이터(AVD): PC마다 별도 생성 필요
- 앱 내부 데이터(알람 DB): 에뮬레이터를 바꾸면 기본적으로 같이 이동되지 않음

### 4) 새 환경에서 바로 체크할 것
1. 앱 설치/실행
2. 알람 1개를 1분 뒤로 등록
3. 홈으로 나가 백그라운드 상태에서 알람 수신 확인
4. 스누즈/해제 동작 확인

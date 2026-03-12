# ShiftAlarmMvp AGENTS Guide

## 읽기 순서
1. [README.md](README.md)
2. [ARCHITECTURE.md](ARCHITECTURE.md)
3. [docs/ENGINEERING_CHARTER.md](docs/ENGINEERING_CHARTER.md)
4. [TODO.md](TODO.md)

## 이 저장소의 핵심 목표
이 앱은 교대 근무자를 위한 초고신뢰성(Ultra-Reliability) 기상 알람 앱이다.
알람 누락은 단순 버그가 아니라 금전, 고용, 사회적 손실로 이어질 수 있는 high-risk incident다.

항상 아래 질문을 기준으로 결정한다.
- **내 다음 기상 알람이 지금 실제로 안전하게 등록되어 있는가?**

## 비가역 규칙
- 기능 추가보다 알람 신뢰성 보장을 우선한다.
- Pattern / Planning 계산 로직은 Android 프레임워크에 의존하지 않는다.
- Reliability 상태는 `ReliabilityCenterPolicy` / `ReliabilityCenterUiModel`만 참조한다.
- editor, preset, duplicate, backup, preview, schedule 계산은 같은 normalize helper를 사용한다.
- 정보가 불충분하면 추측하지 말고 `미지정`으로 표기한다.
- 모든 변경은 production-ready 수준의 실패 처리, 테스트, 회귀 포인트를 포함한다.

## 구현 시 우선 확인
- 어떤 계층이 영향받는가: Pattern / Planning / Schedule / Reliability
- exact alarm permission lifecycle에 영향이 있는가
- timezone / DST / time changed에 영향이 있는가
- restore / migration / version mismatch에 영향이 있는가
- Reliability UI SSOT를 우회하는가

## 현재 우선순위
- P0: exact alarm lifecycle, timezone/DST reschedule, watchdog/self-healing, restore post-check contract
- P1: long rotation + override integrity, backup schema/migration, pure Kotlin test coverage
- P2: trust UX, audit trail, accessibility/performance
- P3: widget/glance, OEM battery guide integration polish

## 응답/작업 방식
- 먼저 영향 계층, 핵심 파일, 리스크를 요약한다.
- 설계 결정은 제품 철학과 현재 코드 상태를 함께 근거로 설명한다.
- 구현 시 실패 시나리오와 방어 로직을 포함한다.
- 마지막에 테스트, 회귀 포인트, 남은 미지정 항목을 명확히 적는다.

## 참고
상세 플랫폼/알고리즘/백업/UX 가드레일은 [docs/ENGINEERING_CHARTER.md](docs/ENGINEERING_CHARTER.md)에 둔다.

# Payday Design System

> 정확한 컬러·타이포·컴포넌트 값은 디자인 확정 후 갱신합니다. 현재 토큰은 초기 개발을 위한 임시 기준입니다.

## Design Principles

1. **한눈에 이해하기**: 금액, 결제일, 내 실부담금을 가장 먼저 읽을 수 있게 배치합니다.
2. **가볍고 다정하게**: 무거운 금융 용어보다 친구가 알려주는 듯한 문장을 사용합니다.
3. **절약을 강요하지 않기**: 경고보다 현황과 선택지를 제공하는 넛지형 UX를 지향합니다.
4. **일관된 상태 표현**: 수입, 고정 지출, 변동 지출, 저축·투자는 색상과 부호를 일관되게 사용합니다.

## Color Tokens

| 토큰 | 값 | 용도 |
| :--- | :--- | :--- |
| `payday_primary` | `#6750A4` | 주요 버튼, 강조 |
| `payday_primary_dark` | `#4F378B` | 상태바, 진한 강조 |
| `payday_background` | `#FFFBFE` | 기본 배경 |
| `payday_text_primary` | `#1D1B20` | 본문 및 핵심 정보 |

## Typography

- 화면 제목: 24sp / Bold
- 섹션 제목: 18sp / SemiBold
- 본문: 16sp / Regular
- 보조 문구: 14sp / Regular
- 금액 강조: 20~28sp / Bold, 천 단위 구분

## Spacing & Shape

- 기본 간격 단위: 4dp
- 화면 좌우 여백: 20~24dp
- 카드 내부 여백: 16dp
- 카드/버튼 모서리: 12~16dp
- 터치 영역: 최소 48dp

## Core Components

- Primary/Secondary Button
- 입력 필드와 검증 메시지
- 지출 유형 세그먼트: 고정 지출 / 변동 지출 / 저축·투자
- 카테고리 Chip
- 금액 요약 Card
- 거래 내역 Row
- 공유 인원 Stepper
- D-DAY Badge
- Empty State / Snackbar / Confirm Dialog

## Accessibility

- 색상만으로 수입·지출을 구분하지 않고 `+`, `-`, 텍스트 라벨을 함께 제공합니다.
- 본문과 배경은 WCAG AA 수준의 대비를 목표로 합니다.
- TalkBack을 고려해 아이콘에는 content description을 제공합니다.


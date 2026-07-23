# Payday Android
> **수입은 한눈에, 정기지출은 미리. 대학생을 위한 가벼운 지출·구독 관리 앱**

## 📢 프로젝트 소개 (Project Overview)

**Payday**는 용돈과 아르바이트비처럼 변동적인 수입을 가진 대학생이 고정 지출, 변동 지출, 저축·투자를 구조적으로 기록하고 다음 결제일까지의 지출을 예측하도록 돕는 Android 앱입니다.

- **기획 배경:** 여러 서비스에 흩어진 정기결제는 월 고정비를 파악하기 어렵게 하고, 구독 공유 비용은 매번 수동 계산을 요구합니다.
- **타겟 유저:** OTT, 음악, AI, 통신비 등 여러 정기결제를 사용하며 수입 대비 지출 균형을 확인하고 싶은 대학생
- **핵심 가치:**
  - 수입 대비 고정 지출·변동 지출·저축 비율을 한눈에 확인
  - 연간 결제의 월 환산액과 공유 인원별 내 실부담금 자동 계산
  - 결제 하루 전 D-DAY 알림으로 예상치 못한 인출 예방

<br/>

## 👥 팀원 및 역할 분담 (Team & Roles)

| 이름 | 역할 및 담당 도메인 | GitHub |
| :---: | :--- | :--- |
| **오윤아** | • 팀장<br>• GitHub 저장소 초기 세팅 및 구조 설계<br>• 지출 관리<br>• 알림 기능 | [@yoona24](https://github.com/yoona24) |
| **전해린** | • 회원 관리<br>• 수입 관리<br>• 대시보드<br>• 거래 내역 리스트 | [@lynni0925](https://github.com/lynni0925) |
| **송은선** | • 디자이너<br>• 화면 디자인<br>• 기획서·기능명세서 등 문서 관리<br>• 발표자료 및 구현 영상 제작 | [@eunda0-0](https://github.com/eunda0-0) |

<br/>

## 📱 화면 목록 및 주요 기능 (Screen List & Features)

| 화면 | 주요 기능 |
| :--- | :--- |
| **회원가입 / 로그인** | 이메일 형식 및 비밀번호 검증, 닉네임 등록, 공통 로그인 오류 처리 |
| **홈 / 대시보드** | 지출 합계·정기 결제 금액 요약, 수입 대비 지출 도넛 차트, 카테고리별 거래 필터 |
| **수입 등록·수정** | 월급·용돈·환급·기타 카테고리, 금액 검증, 수정·삭제 |
| **지출 등록·수정** | 고정 지출·변동 지출·저축·투자 분류, 카테고리·결제수단·결제일 입력 |
| **정기 결제 설정** | 매월 결제일, 월간·연간 반복, 월 환산 금액, 공유 인원별 내 부담금 계산 |
| **거래 내역** | 이번 달 수입·지출 최신순 조회, 지출 유형 및 수입 카테고리 필터 |
| **알림** | 정기결제 D-1 오전 9시 로컬 알림, 알림 클릭 시 관련 목록 이동 |

> 과다 지출 경고의 판단 기준과 별도 구독 전용 목록 제공 여부는 기획 확인이 필요한 항목입니다.

<br/>

## 🎨 Design System

→ **[프로젝트 디자인 시스템 명세서](./design.md)**

<br/>

## Implementation Guidelines

- **Single Activity + Feature 중심 패키지:** 화면 진입점과 도메인 로직을 분리합니다.
- **MVVM:** UI는 상태를 표시하고, ViewModel은 화면 상태와 사용자 액션을 관리합니다.
- **Repository Pattern:** 로컬/원격 데이터 소스를 Repository 뒤에 숨겨 교체와 테스트를 쉽게 합니다.
- **금액 계산의 단일 책임:** 월 환산액과 내 실부담금 계산은 UI가 아닌 domain 계층에서 처리합니다.
- **알림 일관성:** 정기지출 수정·삭제 시 WorkManager 예약도 함께 갱신·취소합니다.

<br/>

## 📔 Tech Stack

| 분류 | 기술 | 용도 |
| :--- | :--- | :--- |
| **Language** | Kotlin | Android 앱 개발 |
| **UI** | XML, ViewBinding, Material Components | 화면 및 디자인 시스템 |
| **Architecture** | MVVM, Repository Pattern | 관심사 분리와 테스트 용이성 |
| **Async** | Coroutines, Flow | 비동기 작업 및 상태 스트림(도입 예정) |
| **Local Data** | Room, DataStore | 거래·설정 저장(도입 예정) |
| **Background** | WorkManager | 결제일 D-1 알림(도입 예정) |
| **Chart** | MPAndroidChart 또는 대안 검토 | 월간 지출 비율 시각화 |
| **Quality** | JUnit, Android Lint, GitHub Actions | 테스트 및 정적 검사 |

<br/>

## ⚙️ Prerequisites

- Android Studio (JDK 17 포함 버전 권장)
- Android SDK 34
- 최소 지원 버전: Android 7.0 (API 24)
- Git

<br/>

## Getting Started

### 1. 프로젝트 클론

```bash
git clone https://github.com/lynni0925/GURU2-android.git
cd GURU2-android
git switch develop
```

### 2. Android Studio 실행

저장소 루트를 Android Studio에서 열고 Gradle Sync를 완료합니다. 로컬 SDK 경로는 자동 생성되는 `local.properties`에만 저장하며 commit하지 않습니다.

### 3. 빌드 및 검사

```bash
./gradlew assembleDebug
./gradlew test lint
```

### 4. Codespaces

저장소의 **Code → Codespaces → Create codespace on develop**에서 생성합니다. Codespaces는 코드 리뷰·문서·일반 Kotlin/Gradle 작업에 활용하고, Android 에뮬레이터 및 실제 기기 테스트는 Android Studio에서 진행하는 것을 권장합니다.

<br/>

## 📂 Project Structure

기능 구현이 시작되면 아래 구조를 기준으로 확장합니다.

```text
app/src/main/java/com/guru2/payday/
├── core/                # 공용 UI, 확장 함수, 디자인 토큰
├── data/                # Room/네트워크 데이터 소스와 Repository 구현
├── domain/              # 모델, Repository 인터페이스, 계산 UseCase
└── feature/
    ├── auth/            # 회원가입, 로그인, 로그아웃
    ├── dashboard/       # 월간 요약 및 차트
    ├── income/          # 수입 CRUD
    ├── expense/         # 지출 CRUD와 정기결제 설정
    ├── transaction/     # 전체 거래 내역 및 필터
    └── notification/    # WorkManager 예약과 알림 이동
```

### 개발 원칙

1. 특정 화면에서만 쓰는 코드는 해당 `feature/` 내부에 둡니다.
2. 금액, 날짜, 공유 정산 로직은 View/Activity에 작성하지 않습니다.
3. 문자열·색상·크기는 resource와 `design.md`를 기준으로 관리합니다.
4. 새 기능은 테스트 가능한 작은 단위로 나누고 PR은 하나의 목적만 갖도록 합니다.

<br/>

## Contribution Guide

### Git Flow

- `main`: 제출·배포 가능한 안정 버전
- `develop`: 개발 통합 브랜치이자 일반 PR 대상
- 개인 작업 브랜치: 최신 `develop`에서 생성

**브랜치 명명 규칙: `타입/기능명_닉네임`**

```bash
git switch develop
git pull origin develop
git switch -c feat/income_yoona
```

| 타입 | 설명 | 예시 |
| :--- | :--- | :--- |
| `feat` | 새 기능 | `feat/dashboard_yoona` |
| `fix` | 버그 수정 | `fix/payment-date_yoona` |
| `design` | UI 변경 | `design/transaction-card_yoona` |
| `refactor` | 리팩터링 | `refactor/expense-domain_yoona` |
| `docs` | 문서 | `docs/readme_yoona` |
| `chore` | 환경 설정 | `chore/ci_yoona` |

자세한 규칙은 **[CONTRIBUTING.md](./CONTRIBUTING.md)**를 확인해주세요.

<br/>

## PR Convention

- PR 방향은 `개인 브랜치 → develop`입니다.
- 제목 형식: `태그: 작업 요약 (#이슈번호)`
- 최소 1명 승인 및 `./gradlew test lint` 통과 후 merge합니다.
- UI 변경은 스크린샷 또는 GIF를 첨부합니다.
- 리뷰 우선순위는 `[P1] 필수`, `[P2] 권장`, `[P3] 의견`을 사용합니다.

<br/>

## References

- [2026 GURU2 Notion](https://app.notion.com/p/2026-GURU2-39f6fce1763180b29466d664c60430b7)
- 기능명세서: `기능명세서_이거사조.xlsx`
- README 형식 참고: [Pebble Frontend](https://github.com/umc-pebble/Pebble-Frontend)

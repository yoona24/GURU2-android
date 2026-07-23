# Contribution Guide

## Branch Strategy

- `main`: 제출·배포 가능한 안정 버전
- `develop`: 기능 통합 및 QA 기준 브랜치
- 작업 브랜치: 반드시 최신 `develop`에서 생성

```bash
git switch develop
git pull origin develop
git switch -c feat/income_yuna
```

브랜치 명명 규칙은 `타입/기능명_이름`을 사용합니다.

| 타입 | 설명 | 예시 |
| :--- | :--- | :--- |
| `feat` | 새 기능 | `feat/dashboard_yuna` |
| `fix` | 버그 수정 | `fix/payment-date_yuna` |
| `design` | UI 변경 | `design/transaction-card_yuna` |
| `refactor` | 리팩터링 | `refactor/expense-domain_yuna` |
| `docs` | 문서 | `docs/readme_yuna` |
| `chore` | 환경 설정 | `chore/ci_yuna` |

## Commit Convention

Conventional Commits 형식을 사용합니다.

```text
feat: 수입 등록 폼 구현
fix: 연간 결제 월 환산 오차 수정
docs: 브랜치 전략 보완
```

변경사항을 커밋한 뒤 현재 작업 브랜치명으로 push합니다.

```bash
git add .
git commit -m "feat: 지출 등록 화면 구현"
git push -u origin feat/expense_yuna
```

위 예시의 `feat/expense_yuna`는 개인 작업 브랜치명입니다. 브랜치를 만들 때 사용한 이름과 push할 때 사용하는 이름이 같아야 합니다.

## Pull Request

1. 작업 브랜치를 원격에 push합니다.
2. `작업 브랜치 → develop` 방향으로 PR을 생성합니다.
3. 템플릿을 작성하고 최소 1명의 승인을 받습니다.
4. `./gradlew test lint` 통과 후 작성자가 merge합니다.
5. `develop → main`은 마일스톤 단위의 통합 PR로만 진행합니다.

리뷰 코멘트는 `[P1] 필수`, `[P2] 권장`, `[P3] 의견` 태그를 권장합니다.

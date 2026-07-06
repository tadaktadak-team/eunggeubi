# 응급이 (eunggeubi)

동아리 타닥타닥 프로젝트

## 프로젝트 구조

| 폴더 | 설명 | 기술스택 |
|------|------|------|
| `backend/` | REST API 서버 | Spring Boot 3.5.0, Java 17 |
| `frontend/` | 모바일 앱 | React Native (Expo 57), TypeScript |

### backend 패키지 구조

```
com.tadaktadak.eunggeubi
├── domain/            # 기능(도메인)별 패키지 (예: domain/login, domain/matching)
└── global/            # 여러 도메인이 같이 쓰는 것 (설정, 예외처리 등)
    ├── config/
    ├── exception/
    └── health/
```

### frontend 폴더 구조

```
frontend/src/
├── domain/            # 기능(도메인)별 폴더 (예: domain/home, domain/login)
│   └── {도메인명}/
│       ├── screens/
│       ├── components/
│       ├── hooks/
│       └── api/
├── navigation/         # 화면 연결 (여러 도메인을 스택으로 묶음)
└── shared/             # 여러 도메인이 같이 쓰는 것 (공용 컴포넌트/훅/api client)
```

새 기능을 만들 때는 `domain/{도메인명}/` 폴더를 새로 추가하고, 2개 이상 도메인이 같이 쓰는 코드만 `shared/`(프론트)나 `global/`(백엔드)로 옮깁니다.

## 브랜치 전략

- `main` : 배포 브랜치 (직접 push 금지)
- `develop` : 통합 개발 브랜치
- `feature/도메인명` : 도메인 단위 기능 개발, 프론트/백엔드 변경 함께 포함 (예: `feature/login`)

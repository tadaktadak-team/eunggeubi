# 응급이 (eunggeubi)

동아리 타닥타닥 프로젝트

## 프로젝트 구조

| 폴더 | 설명 | 기술스택 |
|------|------|------|
| `backend/` | REST API 서버 | Spring Boot 3.5.0, Java 17 |
| `frontend/` | 모바일 앱 | React Native (Expo 57), TypeScript |

## 브랜치 전략

- `main` : 배포 브랜치 (직접 push 금지)
- `develop` : 통합 개발 브랜치
- `feature/도메인명` : 도메인 단위 기능 개발, 프론트/백엔드 변경 함께 포함 (예: `feature/login`)

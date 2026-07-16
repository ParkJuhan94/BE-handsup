# HandsUp - 중고 경매 거래 플랫폼

> 물품을 등록하고 실시간으로 입찰하며, 채팅으로 거래하는 경매 웹 서비스

[![CI Backend](https://github.com/ParkJuhan94/BE-handsup/actions/workflows/ci.yml/badge.svg)](https://github.com/ParkJuhan94/BE-handsup/actions/workflows/ci.yml)

- 백엔드 협업 Repository 👉🏻 [Programmers-HandsUp/BE-handsup](https://github.com/Programmers-HandsUp/BE-handsup)
- 프론트엔드 Repository 👉🏻 [Programmers-HandsUp/FE-HandsUp](https://github.com/Programmers-HandsUp/FE-HandsUp)

<div align="left">
  <img width="600" alt="서비스 소개" src="https://github.com/Programmers-HandsUp/BE-handsup/assets/77109954/8e3b8727-a732-4b9c-a575-c2bdca9f576b">
</div>

<br>

## ✨ 핵심 기능

- **경매** : 물품 등록 · 검색 · 북마크 · 댓글, 스케줄러 기반 상태 자동 전이 (입찰 중 → 거래 중 → 거래 완료)
- **입찰** : 실시간 입찰, Redisson 분산 락으로 동시 입찰 정합성 제어
- **채팅** : WebSocket 기반 판매자 ↔ 입찰자 실시간 채팅
- **알림** : FCM 푸시 알림 (입찰 이벤트를 Kafka로 비동기 처리)
- **리뷰 / 신뢰도** : 거래 후 리뷰 작성, 사용자 신뢰도 점수(0~200) 반영
- **인증** : JWT 액세스/리프레시 토큰 인증

<br>

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.2, JPA, QueryDSL, Spring Security |
| Messaging | Kafka |
| DB / Cache | MySQL, Redis (Redisson), Caffeine |
| Infra / CI·CD | Docker, AWS EC2 · S3, GitHub Actions |
| Test | JUnit 5, Mockito, Testcontainers, JaCoCo, k6 |
| 기타 | JWT, Swagger, Firebase FCM, OpenAI API |

<br>

## 📁 멀티 모듈 구조

```
api     → REST API, WebSocket 엔드포인트 (Controller, DTO)
core    → 핵심 비즈니스 로직 (Service, Domain, Repository)
event   → Kafka 이벤트 처리 (Producer / Consumer)
common  → 공통 유틸리티
```

> 의존 방향: `api → core, event` / `event → core`

<br>

## 🔍 기술적 특징

- **이벤트 기반 알림** : 입찰 이벤트를 트랜잭션 커밋 후(`AFTER_COMMIT`) Kafka로 발행, `@RetryableTopic` 재시도 + DLT로 메시지 유실 방지
- **동시성 제어** : Redisson 분산 락을 AOP(`@DistributeLock`)로 적용해 동시 입찰 시 데이터 정합성 보장
- **2단 캐시** : 추천 경매 조회에 Caffeine(로컬) → Redis(분산) 계층 캐시 적용
- **테스트 격리** : Testcontainers(MySQL · Redis) 기반 통합 테스트, JaCoCo + Codecov 커버리지 관리
- **운영 환경 분리** : 앱 서버와 Kafka 서버를 별도 EC2로 분리, GitHub Actions로 Docker 자동 배포

<br>

## 📚 상세 자료

<details>
<summary><b>DB ERD</b></summary>

<img width="1600" alt="ERD" src="https://github.com/Programmers-HandsUp/BE-handsup/assets/77109954/a6253b8b-497a-4cfe-839a-c10d25d349a9">

</details>

<details>
<summary><b>CI/CD Pipeline</b></summary>

<img width="600" alt="CI/CD 파이프라인" src="https://github.com/Programmers-HandsUp/BE-handsup/assets/77109954/79ef0f21-d86b-45ae-a955-03d60b5ccfcb">

</details>

<details>
<summary><b>API 리스트</b></summary>

<img width="361" alt="API 리스트 1" src="https://github.com/user-attachments/assets/2ab88cb2-18dc-4b98-a0d3-baa85971ea19" />
<img width="403" alt="API 리스트 2" src="https://github.com/user-attachments/assets/aa7de8f1-1485-4780-ab3b-5eeac36d1105" />

</details>

<details>
<summary><b>테스트 결과</b></summary>

<img width="347" alt="테스트 결과" src="https://github.com/user-attachments/assets/e452b6aa-0f00-42b4-aaa6-a6ad1618a599" />
<img width="954" alt="테스트 상세 1" src="https://github.com/user-attachments/assets/ae7841f8-f528-4cf2-a12d-97e53c64f712" />
<img width="1039" alt="테스트 상세 2" src="https://github.com/user-attachments/assets/7a0b8565-fcee-47c6-be3b-88be080945ad" />

</details>

<br>

## 👥 팀원 소개

<table>
  <tr>
    <th>손가현(팀장)</th>
    <th>박주한</th>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/hyun2371">
        <img width="100px" src="https://github.com/hyun2371.png" />
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/ParkJuhan94">
        <img width="100px" src="https://github.com/ParkJuhan94.png" />
      </a>
    </td>
  </tr>
</table>

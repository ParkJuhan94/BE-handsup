# Plan: 입찰 동시성 제어 버그 수정

## 문제 분석

`BiddingConcurrencyTest.concurrency_test()` 테스트가 실패하여 `@Disabled` 처리됨.
- 100개 스레드가 동일 가격으로 동시 입찰 시, 1개만 저장되어야 하지만 여러 개가 저장됨

### 근본 원인: AOP Aspect 순서 문제

`@Transactional`과 `@DistributeLock` 간의 실행 순서가 정의되지 않아:
1. 여러 스레드가 트랜잭션을 먼저 시작 (DB 스냅샷 읽음)
2. 모든 스레드가 `maxBiddingPrice = NULL` 읽음 (stale data)
3. 락 획득 후에도 이미 읽은 데이터 사용 → 여러 입찰 저장됨

### 추가 문제: leaseTime < waitTime
- `waitTime=5s`, `leaseTime=3s` → 트랜잭션이 3초 넘으면 락 자동 해제

## 수정 계획

### 1. DistributeLockAop에 @Order 추가
**파일**: `core/src/main/java/dev/handsup/common/redisson/DistributeLockAop.java`

```java
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)  // 추가
public class DistributeLockAop {
```

→ 락이 트랜잭션보다 먼저 실행되도록 보장

### 2. BiddingService.registerBidding()에서 @Transactional 제거
**파일**: `core/src/main/java/dev/handsup/bidding/service/BiddingService.java`

```java
// 변경 전
@Transactional
@DistributeLock(key = "'auction_' + #auctionId")
public BiddingResponse registerBidding(...) {

// 변경 후
@DistributeLock(key = "'auction_' + #auctionId")
public BiddingResponse registerBidding(...) {
```

→ `AopForTransaction.proceed()`의 `REQUIRES_NEW`가 트랜잭션 관리

### 3. DistributeLock 기본 leaseTime 증가
**파일**: `core/src/main/java/dev/handsup/common/redisson/DistributeLock.java`

```java
// 변경 전
long leaseTime() default 3L;

// 변경 후
long leaseTime() default 10L;
```

### 4. 테스트 활성화
**파일**: `core/src/test/java/dev/handsup/bidding/repository/BiddingConcurrencyTest.java`

`@Disabled` 어노테이션 제거

## 수정 후 예상 동작

```
Thread A: [락 획득] → [TX 시작] → [maxPrice=NULL 읽음] → [저장] → [커밋] → [락 해제]
Thread B: [락 대기...] → [락 획득] → [TX 시작] → [maxPrice=10000 읽음] → [검증 실패!] → [락 해제]
```

→ 1개의 입찰만 저장됨

## 검증 방법

```bash
./gradlew :core:test --tests '*BiddingConcurrencyTest*'
```

- `biddingRepository.findAll().hasSize(1)` 통과 확인
- 로그에서 `unlock skipped: lock not held by current thread` 경고 없음 확인

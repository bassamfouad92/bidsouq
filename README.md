# BidSouq
 
A live car auction platform built to explore concurrency, caching, and real-time delivery under contention.
 
The interesting problem here isn't CRUD — it's what happens in the last sixty seconds of an auction, when thousands of bidders hit the same auction at once and every bid has to be validated against a value that changes several times per second.
 
---
 
## Why this project
 
Most portfolio projects demonstrate that you can wire up a framework. This one is an excuse to work through problems that only show up under load:
 
- **Row-level lock contention** — what happens when every write targets the same row, and why adding threads doesn't help
- **Read/write split** — validating against Redis while keeping Postgres as the source of truth
- **Fanout** — pushing updates to ~10,000 concurrent viewers without generating millions of messages per second
- **Delivery guarantees** — knowing which events can be lost (live bid updates) and which cannot (winner announcement, payment)
---
 
## Stack
 
| Layer | Choice |
|---|---|
| Runtime | Java 21, Spring Boot 4.1 |
| Persistence | Spring Data JPA, Hibernate 7, PostgreSQL 16 |
| Cache / messaging | Redis 7 (sorted sets + pub/sub) |
| Migrations | Flyway |
| Security | Spring Security, JWT |
| Concurrency | Virtual threads (`spring.threads.virtual.enabled`) |
| Build | Maven |
 
---
 
## Architecture
 
Two deployable services sharing one database, with strict table ownership — no cross-service joins.
 
```
                        ┌──────────────┐
  Bidders ──POST──────► │ API Gateway  │  rate limit · idempotency · JWT
                        └──────┬───────┘
                               │
              ┌────────────────┴────────────────┐
              ▼                                 ▼
      ┌───────────────┐                 ┌───────────────┐
      │ core-service  │                 │  bid-service  │
      │ :8080         │                 │  :8081        │
      │ auth · users  │                 │  bids only    │
      │ cars · auctions│                │               │
      └───────┬───────┘                 └───────┬───────┘
              │                                 │
              │        ┌────────────────────────┤
              ▼        ▼                        ▼
       ┌────────────────────┐            ┌──────────┐
       │ PostgreSQL         │            │  Redis   │
       │ (separate tables)  │            │ ZSET     │
       └──────────┬─────────┘            └────┬─────┘
                  │ AFTER_COMMIT              │ pub/sub
                  └───────────────────────────┤
                                              ▼
                                      ┌───────────────┐
                                      │ WS Gateway ×N │ ──► 10,000 viewers
                                      └───────────────┘
```
 
**`core-service`** — authentication, users, cars, auctions, the auction-close scheduler.
 
**`bid-service`** — bid placement only. Extracted because its load profile is different: bursty, contention-prone, and scaling for reasons the rest of the system doesn't share. It stores `auctionId` as a plain UUID and never holds a reference to the `Auction` entity.
 
---
 
## Key design decisions
 
### Append-only bids instead of a running `highest_bid` column
 
The obvious design keeps a `highest_bid` column on `auctions` and updates it per bid. Postgres takes an exclusive row lock for each update, so every bid on the same auction serialises — roughly 100–300 TPS regardless of hardware, because the limit is lock contention rather than CPU.
 
Every bid is an `INSERT` into its own row instead. Different rows, different locks, no contention. Full bid history comes free, which matters for audit and disputes.
 
### Redis for validation, Postgres for truth
 
Validating "is this bid higher than the current highest?" against the database would reintroduce the contention the append-only design just removed. A Redis sorted set holds the current highest per auction and answers in ~0.1 ms.
 
Redis is a cache, not a system of record. If it goes down, it rebuilds from Postgres.
 
### Broadcast after commit, not before
 
Bid events publish from `@TransactionalEventListener(phase = AFTER_COMMIT)`. Publishing before commit means a rolled-back transaction can still show viewers a bid that never existed.
 
### Throttled WebSocket fanout
 
Broadcasting every bid to every viewer is `1,000 bids/sec × 10,000 sockets = 10M messages/sec`. Pushes are capped at ~5/sec per auction carrying only the latest state, which brings it to a workable ~50k/sec across horizontally scaled gateway instances.
 
### Outbox only where delivery must be guaranteed
 
Live bid updates are best-effort — a dropped message is corrected by the next one 200 ms later. Winner announcements, payment triggers, and emails are not. Those are written to an outbox table inside the same transaction as the state change and picked up by a poller.
 
### Virtual threads over WebFlux
 
WebFlux would force a move to R2DBC, giving up JPA, JPQL, and entity graphs — and it wouldn't address the actual bottleneck, which is in the database rather than the thread model. Virtual threads provide the same I/O efficiency with one line of configuration and no rewrite.
 
---
 
## Running locally
 
```bash
# Start Postgres (5433) and Redis (6379)
docker compose up -d
 
# core-service on :8080
cd core-service && ./mvnw spring-boot:run
 
# bid-service on :8081
cd bid-service && ./mvnw spring-boot:run
```
 
Postgres is mapped to **5433** to avoid colliding with a local install on the default port.
 
### Configuration
 
Credentials default to `bidsouq` for local development. Override with environment variables:
 
```
DB_USER=...
DB_PASSWORD=...
```
 
---
 
## Project layout
 
Feature-based packages rather than layer-based, so a module can be extracted into its own service without reshuffling files.
 
```
core-service/
  auth/      — JWT, login, security config
  user/      — User entity, repository, service
  car/       — Car, CarAttachment
  auction/   — Auction, scheduler
  common/    — enums, exceptions, config
 
bid-service/
  bid/       — Bid entity, placement, validation
  common/
```
 
---
 
## Status
 
Work in progress. Built as a learning exercise, so each stage is implemented naively first and the failure observed under load before the fix goes in.
 
- [x] Domain model and schema
- [x] Local infrastructure (Docker, Postgres, Redis)
- [ ] Repositories and CRUD endpoints
- [ ] Authentication and JWT
- [ ] Naive bid placement
- [ ] Load test — observe the race condition
- [ ] Redis-backed validation
- [ ] WebSocket fanout
- [ ] Auction close scheduler and outbox
 

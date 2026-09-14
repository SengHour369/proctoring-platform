# Proctoring Platform — Microservices

A Spring Cloud microservices split of the original single-module proctoring/exam data model
(82 JPA entities across 19 packages, documented in [`docs/`](docs/), originally scaffolded as
one Spring Boot app under `legacy-monolith/`). No `@Service`/`@Repository`/`@RestController`
layer existed before this split and none was added here — this is purely the module boundary
and infrastructure work; see [`docs/service-layer-plan.md`](docs/service-layer-plan.md) for
what still needs to be built inside each service.

---

## Architecture

```
                         ┌──────────────────┐
                         │   api-gateway     │  :8080  (Spring Cloud Gateway)
                         └─────────┬─────────┘
                                   │ lb://<service-id>  (via Eureka)
        ┌───────────┬──────────┬──┴───────┬───────────┬───────────┬────────────┬──────────────┬─────────────┐
        │           │          │          │           │           │            │              │             │
   identity      exam       attempt   proctoring    review      result      payment     notification     config
   :8081        :8082       :8083      :8084        :8085       :8086       :8087         :8088          :8089
        │           │          │          │           │           │            │              │             │
        └───────────┴──────────┴──────────┴───────────┴───────────┴────────────┴──────────────┴─────────────┘
                                       one Postgres DB per service

   eureka-server :8761   — service discovery, every service and the gateway register here
   config-server :8888   — centralised config, served from config-server/src/main/resources/config-repo/
```

## Service boundaries

Grouped from the original entity packages (see [`docs/entity-relationships.md`](docs/entity-relationships.md)
for the full FK map — every relationship is a plain `Long` column, never a JPA association, which is what
made this split possible without touching the entities' shape):

| Service | Packages moved in | Owns |
|---|---|---|
| `identity-service` | `auth`, `usergroup` | Users, roles/permissions, sessions, API clients, student groups |
| `exam-service` | `exam`, `question` | Exam definitions, assignments, invitations, question bank |
| `attempt-service` | `attempt`, `excel` | Exam attempts, answers, pauses/resumptions, Excel runtime |
| `proctoring-service` | `proctoring`, `detection`, `aimodel`, `precheck`, `identity`, `consent` | Live sessions, evidence, AI detections, system checks, consent |
| `review-service` | `risk`, `review`, `audit` | Risk scoring, review cases, audit log |
| `result-service` | `result`, `report` | Exam results, withholding, reports/timelines |
| `payment-service` | `payment` | Payment customers/cards/transactions, exam payment requirements |
| `notification-service` | `notification` | Notifications, templates, suppression rules |
| `config-service` | `config` | System settings, risk factor/threshold configuration |

A handful of entities reference an enum owned by another service purely for a typed
`@Enumerated` column (e.g. `result-service`'s `ExamResult.riskLevel` reusing `RiskLevel`,
which `review-service` owns). Rather than one service depending on another's JAR, that enum
is duplicated into a local `sharedenums` package in the consumer — the same reasoning the
entities already apply to FKs (an opaque value, not a live reference). Everything else the
entities used to reference across domains turned out to be dead imports once checked (e.g.
`ExamResult` importing the `ExamAttempt`/`Exam` entity types, when it only ever stores their
IDs) and was removed.

## Repository layout

```
build.gradle, settings.gradle   — multi-module Gradle root (Spring Boot 4 / Java 21 toolchain)
eureka-server/                  — service registry
config-server/                  — config server (native profile, config-repo/ per service)
api-gateway/                    — Spring Cloud Gateway, one route + circuit breaker per service
identity-service/ … config-service/   — the 9 business services, each a standalone Spring Boot app
docker/                         — shared Dockerfile + Postgres multi-DB init script
docker-compose.yml              — local dev stack: builds images locally, everything exposed on localhost
docker-compose.prod.yml         — production variant: pulls pre-built images, gateway is the only exposed
                                  port, DB password via a Docker secret file — see below
k8s/                             — Kubernetes manifests for the same stack (kubectl apply -k k8s/)
legacy-monolith/                — the original single-module app this was split from, kept for reference
docs/                           — entity/enum/schema reference docs (pre-existing, paths updated for the move)
```

## Running it

```bash
docker compose up --build
```

This brings up Postgres (one instance, one database per service), Zipkin (tracing UI on
`:9411`), Eureka (`:8761`), the Config Server (`:8888`), the gateway (`:8080`), and all nine
services. Hit anything through the gateway, e.g. `GET http://localhost:8080/api/identity/...`.

### Production deploy

`docker-compose.prod.yml` is the hardened variant for an actual host: it pulls pre-built images
(`${REGISTRY}/<module>:${TAG}`) instead of building locally, exposes only the gateway's `:8080`
to the host (everything else is reachable only on the compose network), and reads the Postgres
password from a Docker secret file instead of a plaintext env var.

```bash
cp .env.prod.example .env               # fill in REGISTRY and TAG
cp secrets/postgres_password.txt.example secrets/postgres_password.txt   # fill in a real password
docker compose -f docker-compose.prod.yml up -d
```

Build and push the images it expects (one per module, via the shared `docker/Dockerfile`) before
running this — see [`k8s/README.md`](k8s/README.md#1-build-and-push-images) for the loop that
does it. For an actual cluster instead of a single host, use `k8s/` (`kubectl apply -k k8s/`).

Each service also runs standalone for local development — `./gradlew :identity-service:bootRun`
— as long as `eureka-server`, `config-server`, and a `identity_db` Postgres database are
reachable (override `EUREKA_HOST` / `CONFIG_SERVER_HOST` / `DB_HOST` / `DB_USERNAME` /
`DB_PASSWORD` env vars as needed; see each service's `application.yml`).

## What's deliberately still open

- **No `@Service`/`@Repository`/`@RestController` classes yet** — same as before the split.
  `docs/service-layer-plan.md` and `docs/service-layer-logic.md` are the spec for that layer;
  it wasn't in scope for this restructuring.
- **`ddl-auto: update`** in every service's config-repo entry — convenient for spinning the
  stack up locally against empty databases, but swap it for Flyway/Liquibase migrations
  before this is anything but a dev sandbox.
- **Spring Cloud BOM version** (`2025.1.0` in the root `build.gradle`) — Spring Boot 4.x is
  very new; double-check the Spring Cloud train that's actually released to pair with your
  exact Boot version before deploying this for real, and bump the `springCloudVersion`
  property accordingly. The build was verified to compile with this pairing at the time of
  writing, but Boot/Cloud compatibility windows move fast.
- **Inter-service calls** — no service currently calls another (there's no business logic
  yet to make such calls). Once the service layer above is built, prefer `WebClient` +
  Eureka service IDs (`http://identity-service/...`) with the gateway's Resilience4j pattern
  mirrored on the caller side, rather than routing service-to-service traffic back out
  through the gateway.

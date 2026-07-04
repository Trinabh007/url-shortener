# URL Shortener Service

A production-ready URL shortening REST API built with Spring Boot, featuring Redis caching, click analytics, and a containerized deployment setup.

---

## Live Demo

> Deployment coming soon. Run locally using Docker Compose — instructions below.

---

## Features

- Shorten any URL to a compact Base62-encoded short code
- Redirect short URLs to their original destination (302)
- Click count analytics tracked per short URL
- Redis caching for fast redirects without hitting the database
- Scheduled flush job syncing Redis click counts to MySQL every 5 minutes
- Swagger UI for interactive API documentation
- Single-page HTML frontend served from the same server
- Fully containerized with Docker Compose (app + MySQL + Redis)
- GitHub Actions CI pipeline on every push and pull request

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1 |
| Database | MySQL 8.0 |
| Cache | Redis 7.0 (Lettuce client) |
| ORM | Spring Data JPA / Hibernate |
| Containerization | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| API Docs | SpringDoc OpenAPI / Swagger UI |

---

## Architecture

```
Client
  │
  ▼
Spring Boot App (port 8080)
  │
  ├── POST /api/shorten ──► MySQL (persist) + Redis (cache)
  │
  └── GET /api/{shortCode}
        │
        ├── Cache HIT  ──► Redis ──► 302 Redirect
        └── Cache MISS ──► MySQL ──► Redis (populate) ──► 302 Redirect
```

### Click Count Flow

```
GET /api/{shortCode}
  │
  ├── @Cacheable getOriginalUrl()   → returns URL (cache hit skips DB)
  └── incrementClickCount()         → Redis INCR (always executes)
        │
        └── @Scheduled every 5 min
              → flush Redis counts to MySQL
              → clear Redis click count keys
```

---

## API Reference

### Shorten a URL

```
POST /api/shorten
Content-Type: application/json
```

**Request:**
```json
{
  "originalUrl": "https://www.example.com/some/very/long/path"
}
```

**Response — 201 Created:**
```json
{
  "originalUrl": "https://www.example.com/some/very/long/path",
  "shortCode": "3f9Kz",
  "shortUrl": "http://localhost:8080/api/3f9Kz"
}
```

---

### Redirect to Original URL

```
GET /api/{shortCode}
```

**Response — 302 Found**
Redirects to the original URL via `Location` header.

---

### Swagger UI

```
GET /swagger-ui/index.html
```

Interactive API documentation available at the above path when the app is running.

---

## Running Locally with Docker

### Prerequisites

- Docker Desktop installed and running

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/Trinabh007/url-shortener.git
cd url-shortener
```

**2. Create a `.env` file in the project root**
```env
MYSQL_ROOT_PASSWORD=yourpassword
DB_URL=jdbc:mysql://mysql:3306/url_shortener
DB_USERNAME=root
DB_PASSWORD=yourpassword
REDIS_HOST=redis
REDIS_PORT=6379
BASE_URL=http://localhost:8080
```

**3. Start all services**
```bash
docker-compose up --build
```

This starts three containers:
- `mysql` — database on port 3307
- `redis` — cache on port 6379
- `app` — Spring Boot API on port 8080

**4. Access the app**

| What | URL |
|---|---|
| Frontend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| API | http://localhost:8080/api/shorten |

**5. Stop the app**
```bash
docker-compose down
```

---

## Design Decisions & Tradeoffs

### Base62 Encoding via Counter

Short codes are generated using a global counter stored in MySQL (`url_counter` table), converted to Base62. This guarantees uniqueness without collision checks and produces short, predictable codes.

**Tradeoff:** The counter is a single point of write contention under very high throughput. Acceptable for this use case; alternatives like hash-based or random generation add collision detection complexity.

---

### Redis Caching — The `@Cacheable` + Separate Increment Pattern

The most interesting design problem in this project.

**The problem:** Using `@Cacheable` on a method that both fetches the URL and increments the click count would cause cache hits to skip the method body entirely — click counts would never increment for cached responses.

**The solution:** Separate the concerns into two distinct methods:

```java
@Cacheable("urls")
public String getOriginalUrl(String shortCode) { ... }   // cached

public void incrementClickCount(String shortCode) { ... } // always runs
```

The controller calls both independently on every redirect. The cache serves the URL fast; the increment always executes regardless of cache state.

---

### Redis Click Count Flush — Eventual Consistency Tradeoff

Click counts are incremented in Redis using `INCR` (atomic, fast) and flushed to MySQL every 5 minutes via `@Scheduled`.

**Known tradeoffs accepted for this use case:**
- Click counts lag the real value by up to 5 minutes
- Counts accumulated in Redis are lost if Redis restarts before a flush
- This is acceptable — click analytics are not critical-path data

**Why not write to MySQL on every click?** Every redirect would require a database write, adding latency to the hottest path in the system. Redis INCR is O(1) and non-blocking.

---

### Docker Compose Service Dependencies

The app container waits for both MySQL and Redis to pass health checks before starting, using `depends_on` with `condition: service_healthy`. This prevents the common Spring Boot startup failure where the app tries to connect before the database is ready.

---

## CI/CD Pipeline

GitHub Actions runs on every push to `main` and every pull request.

**Pipeline steps:**
1. Checkout code
2. Set up Java 25 (Temurin)
3. Restore Maven dependency cache
4. Start MySQL and Redis as service containers
5. Build with Maven (`./mvnw clean package`)

Workflow file: `.github/workflows/ci.yml`

---

## Project Structure

```
src/main/java/com/example/url_shortener/
├── controller/     # REST endpoints
├── service/        # Business logic, caching, scheduling
├── repository/     # Spring Data JPA repositories
├── entity/         # JPA entities
├── dto/            # Request and response objects
├── encoding/       # Base62 encoding logic
├── exception/      # Global exception handling
└── config/         # Redis, CORS configuration

src/main/resources/
├── static/
│   └── index.html  # Frontend
└── application.properties
```

---

## Author

**Trinabh** — [github.com/Trinabh007](https://github.com/Trinabh007)

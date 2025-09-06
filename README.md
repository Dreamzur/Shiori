# Shiori — Manga Finder & Library (Spring Boot)

**Shiori** is a Spring Boot backend that integrates with the MangaDex API to search manga, fetch chapter feeds, and store entries in a PostgreSQL database. It supports creating, updating, and deleting library entries, with plans for a frontend, user accounts, and SMS/email notifications.

It exposes clean REST endpoints for a frontend (React, mobile, etc.) and keeps DB timestamps updated with `@PrePersist` / `@PreUpdate`.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Environment](#environment)
  - [Run with Docker Compose (persistent DB)](#run-with-docker-compose-persistent-db)
  - [Run Locally (no Docker)](#run-locally-no-docker)
- [Data Model](#data-model)
- [DTOs](#dtos)
- [API Reference](#api-reference)
  - [Local Library (CRUD)](#local-library-crud)
  - [MangaDex Proxy Endpoints](#mangadex-proxy-endpoints)
- [Error Handling](#error-handling)
- [Project Layout](#project-layout)
- [Roadmap](#roadmap)
- [License](#license)

---

## Architecture

```
[Client / Frontend]
        |
        v
  REST Controllers
   ├─ MangaController      (Local library CRUD)
   └─ MangaDexController   (MangaDex search/feed/latest)
        |
        v
     Services
   ├─ MangaService
   └─ MangaDexService  ← uses OpenFeign → MangaDex API
        |
        v
   JPA Repository
        |
        v
   PostgreSQL Database
```

---

## Tech Stack

- **Java 21**, **Spring Boot 3**
- Spring Web, Spring Data JPA
- **Spring Cloud OpenFeign** (MangaDex client)
- **PostgreSQL**
- Jackson for JSON mapping

---

## Getting Started

### Environment

Create a `.env` file at the project root:

```env
DB_USER=<your_username_here>
DB_PASSWORD=<your_password_here>
# Use db (service name) when running in Docker; use localhost if running locally without Docker
DB_URL=jdbc:postgresql://<host>:<port>/<db>

# Optional Spring overrides (will read from above by default)
SPRING_DATASOURCE_URL=${DB_URL}
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

> If you run Postgres locally without Docker, set `DB_URL=jdbc:postgresql://<host>:<port>/<db>`.

### Run with Docker Compose (persistent DB)

Create `docker-compose.yml`:

```yaml
services:
  db:
    image: postgres:latest
    environment:
      POSTGRES_DB: shiori
      POSTGRES_USER: <your_db_username>
      POSTGRES_PASSWORD: <your_db_password>
    ports:
      - "5332:5432"
    volumes:
      - db:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U <your_db_username> -d shiori"]
      interval: 5s
      timeout: 3s
      retries: 10

volumes:
  db:
```

Then:

```bash
docker compose up --build
# Stop (data persists thanks to the named volume)
docker compose down
```

### Run Locally (no Docker)

1. Start PostgreSQL and create a database named `shiori`.
2. Set `.env` for your local connection:
   ```env
   DB_USER=<your_user_name>
   DB_PASSWORD=<your_password>
   DB_URL=jdbc:postgresql://<host>:<port>/shiori
   SPRING_JPA_HIBERNATE_DDL_AUTO=update
   ```
3. Run the app:
   ```bash
   ./mvnw spring-boot:run
   ```
   Or via your IDE: run `ShioriApplication`.

---

## Data Model

### `Manga` (JPA Entity)

| Field           | Type               | Notes                                  |
|-----------------|--------------------|----------------------------------------|
| `id`            | `Long`             | PK, identity                            |
| `title`         | `String`           | Required                                |
| `mangadexId`    | `String` (len 36)  | Unique external ID (nullable)           |
| `year`          | `Integer`          | Optional                                |
| `coverImageUrl` | `String`           | Optional                                |
| `status`        | `MangaStatus` enum | `ONGOING` / `COMPLETED` / `HIATUS` / `CANCELLED` |
| `createdAt`     | `Instant`          | Set on create                           |
| `updatedAt`     | `Instant`          | Set on update                           |

Timestamps are maintained via `@PrePersist` and `@PreUpdate`.

---

## DTOs

### `MangaSearchResult`

```java
public record MangaSearchResult(
    String id,
    String title,
    Integer year,
    String coverUrl
) {}
```

Minimal card data for search results.

### `ChapterResult`

```java
public record ChapterResult(
    String chapterId,
    String chapter,
    String title,
    String volume,
    String readableAt,
    String groupName
) {}
```

Normalized chapter metadata from MangaDex.

---

## API Reference

**Base URL:** `http://localhost:8080/api`

### Local Library (CRUD)

#### List all
**GET** `/manga`  
**200 OK** → `List<Manga>`

```bash
curl http://localhost:8080/api/manga
```

#### Get by id
**GET** `/manga/{id}`  
**200 OK** → `Manga`  
**404 Not Found** if missing

```bash
curl http://localhost:8080/api/manga/1
```

#### Get by external (MangaDex) id
**GET** `/manga?mangadexId={uuid}`  
**200 OK** → `Manga`  
**404 Not Found** if missing

```bash
curl "http://localhost:8080/api/manga?mangadexId=abcd-efgh-1234"
```

#### Create
**POST** `/manga`  
Body:
```json
{
  "title": "Berserk",
  "mangadexId": "dead-beef-1234",
  "year": 1989,
  "coverImageUrl": "https://uploads.mangadex.org/covers/dead-beef-1234/cover.jpg",
  "status": "ONGOING"
}
```
**201 Created** → `Manga` with `Location: /api/manga/{id}`

```bash
curl -X POST http://localhost:8080/api/manga   -H "Content-Type: application/json"   -d '{"title":"Berserk","mangadexId":"dead-beef-1234","year":1989,"coverImageUrl":"https://...","status":"ONGOING"}'
```

#### Update
**PUT** `/manga/{id}`  
Body: same shape as create  
**200 OK** → updated `Manga`  
**404 Not Found** if missing

```bash
curl -X PUT http://localhost:8080/api/manga/1   -H "Content-Type: application/json"   -d '{"title":"Berserk (Deluxe)","mangadexId":"dead-beef-1234","year":1989,"coverImageUrl":"https://...","status":"COMPLETED"}'
```

#### Delete
**DELETE** `/manga/{id}`  
**204 No Content** if deleted  
**404 Not Found** if the id doesn’t exist (service throws `EntityNotFoundException`)

```bash
curl -i -X DELETE http://localhost:8080/api/manga/1
```

> **Note:** Delete is *not* silently idempotent in the current service; attempting to delete a missing id returns **404**.

---

### MangaDex Proxy Endpoints

**Base:** `http://localhost:8080/api/md`

#### Search titles (MangaDex)
**GET** `/search?title={q}&limit={n}`  
**200 OK** → `List<MangaSearchResult>`

```bash
curl "http://localhost:8080/api/md/search?title=one%20piece&limit=5"
```

#### Chapter feed (newest first)
**GET** `/manga/{id}/feed?limit={n}&lang={en|ja|...}`  
**200 OK** → `List<ChapterResult>`

```bash
curl "http://localhost:8080/api/md/manga/abcd-efgh-1234/feed?limit=10&lang=en"
```

#### Latest numbered chapter
**GET** `/manga/{id}/latest?lang={en|ja|...}`  
Finds the highest **numeric** chapter via MangaDex aggregate; falls back to newest item if none.  
**200 OK** → `ChapterResult` (or `null` if absolutely nothing found)

```bash
curl "http://localhost:8080/api/md/manga/abcd-efgh-1234/latest?lang=en"
```

---

## Error Handling

- **404 Not Found** — Entity lookups that miss (e.g., `GET /manga/{id}`, `PUT /manga/{id}`, `DELETE /manga/{id}`) raise `EntityNotFoundException` in `MangaService`.
- **400 Bad Request** — Malformed JSON / invalid enum values (e.g., `status`).
- **5xx** — Upstream MangaDex issues or unexpected parsing errors (rethrown from `MangaDexService`).

> Prefer idempotent deletes? Change `MangaService.deleteById` to no-op on missing IDs and return **204**.

---

## Project Layout

```
src/main/java/com/shiori/backend/
├─ ShioriApplication.java
├─ Manga.java                         # JPA entity + timestamps + status enum
├─ MangaRepository.java               # JpaRepository + findByMangadexId
├─ MangaService.java                  # CRUD + validation
├─ MangaController.java               # /api/manga CRUD
├─ dto/
│   ├─ MangaSearchResult.java
│   └─ ChapterResult.java
├─ MangaDexClient.java                # OpenFeign client (api.mangadex.org)
├─ MangaDexService.java               # JSON parsing + business logic
└─ MangaDexController.java            # /api/md/* (search/feed/latest)
```

---

## Roadmap

- 🔐 Authentication & per-user libraries  
- 🔔 Notifications (poller + email/SMS) for new chapters  
- 🧠 Caching of MangaDex responses  
- 📈 Observability (metrics, structured logs)  
- ✅ Integration tests + Postman collection  

---

# Student News Demo

Demo application for managing student news without authentication.

Supported operations:
- list news
- create news drafts
- edit existing news
- publish news
- archive news
- delete news

---

## Architecture

The project contains 3 services:

1. `db` - PostgreSQL database
2. `backend` - Spring Boot REST API (`/api/news`)
3. `frontend` - simple React app for interacting with the API

All services are containerized and run together via Docker Compose.

---

## Tech stack

- PostgreSQL 16
- Java 21 + Spring Boot 3
- Spring Data JPA + Hibernate
- React 18 + Vite
- Docker + Docker Compose

---

## Project structure

```text
.
├── backend/              # Spring Boot API
├── frontend/             # React UI
└── docker-compose.yml    # Local deployment
```

---

## Prerequisites

- Docker Desktop installed and running
- Docker Compose v2 (`docker compose`)

---

## Quick start

From the project root:

```bash
docker compose up --build
```

When containers are ready:

- Frontend: [http://localhost:5173](http://localhost:5173)
- Backend API: [http://localhost:8080/api/news](http://localhost:8080/api/news)
- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- PostgreSQL: `localhost:5432`

Stop everything:

```bash
docker compose down
```

Stop and remove DB volume (clean reset):

```bash
docker compose down -v
```

---

## API

Base URL: `http://localhost:8080/api/news`

OpenAPI specification is auto-generated from code and available at:
- `GET /v3/api-docs` (JSON)
- Swagger UI: `/swagger-ui/index.html`

### Endpoints

- `GET /api/news` - list all news
- `GET /api/news?status=DRAFT|PUBLISHED|ARCHIVED` - list by status
- `POST /api/news` - create draft news
- `PUT /api/news/{id}` - edit news title/content
- `PATCH /api/news/{id}/publish` - mark as published
- `PATCH /api/news/{id}/archive` - mark as archived
- `DELETE /api/news/{id}` - delete news

### Create/update payload

```json
{
  "title": "Exam schedule announced",
  "content": "The exam period starts on June 3."
}
```

### Example requests

Create draft:

```bash
curl -X POST http://localhost:8080/api/news \
  -H "Content-Type: application/json" \
  -d '{"title":"Campus open day","content":"Open day starts at 10:00"}'
```

Publish news with id `1`:

```bash
curl -X PATCH http://localhost:8080/api/news/1/publish
```

Archive news with id `1`:

```bash
curl -X PATCH http://localhost:8080/api/news/1/archive
```

Delete news with id `1`:

```bash
curl -X DELETE http://localhost:8080/api/news/1
```

---

## Database configuration

Default DB settings used by Docker Compose:

- database: `student_news`
- user: `student`
- password: `student`

The backend reads:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

---

## Notes

- This project is intentionally simple and does **not** include authentication/authorization.
- It is designed for demo and local development purposes.

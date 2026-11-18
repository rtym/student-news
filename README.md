# Student News Demo

[![CI](https://github.com/rtym/student-news/actions/workflows/ci.yml/badge.svg)](https://github.com/rtym/student-news/actions/workflows/ci.yml)
[![E2E (Playwright)](https://github.com/rtym/student-news/actions/workflows/e2e.yml/badge.svg)](https://github.com/rtym/student-news/actions/workflows/e2e.yml)

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

The architecture is described with the SEI **Views and Beyond** method in
[`architecture/docs/`](architecture/docs/README.md). Its [roadmap](architecture/docs/README.md)
says which document answers which question. Every diagram is UML, drawn with PlantUML; the
`.puml` sources sit next to the images in `architecture/docs/diagrams/`, and the colours are
explained in the [legend](architecture/docs/diagrams/legend.png).

| Document | What it answers | UML diagrams |
|----------|-----------------|--------------|
| [System overview](architecture/docs/system-overview.md) | What does the system do, and what shaped it? Use cases UC-1 to UC-6, constraints, quality scenarios | use case, component (context) |
| [Decomposition view](architecture/docs/view-module-decomposition.md) | Where does each piece of code live? | package |
| [Layered view](architecture/docs/view-module-layered.md) | Which backend class does what, and what may it call? | class |
| [Runtime view](architecture/docs/view-cnc-client-server.md) | What runs, how do the parts talk, and what is the REST interface? | component, sequence (UC-1, UC-2, UC-4), state machine |
| [Deployment view](architecture/docs/view-allocation-deployment.md) | Which container runs what, on which port, as which user? | deployment |
| [CI pipeline view](architecture/docs/view-allocation-ci.md) | Which job checks what before a merge? | activity |
| [Rationale](architecture/docs/rationale.md) | Why is it built this way? Six architecture decision records | — |

### Use cases

[![Use cases — Student News (UC-1 to UC-6)](architecture/docs/diagrams/system-overview-use-cases.png)](architecture/docs/system-overview.md#primary-functional-requirements-use-cases)

### Runtime components

[![Runtime — client-server components and connectors (UML component diagram)](architecture/docs/diagrams/view-cnc-client-server.png)](architecture/docs/view-cnc-client-server.md)

### Deployment

[![Deployment — Docker Compose stack on one host (UML deployment diagram)](architecture/docs/diagrams/view-allocation-deployment.png)](architecture/docs/view-allocation-deployment.md)

---

## Tech stack

- PostgreSQL 16
- Java 27 + Spring Boot 4
- Spring Data JPA + Hibernate
- React 19 + Vite
- Docker + Docker Compose
- Kubernetes on minikube (optional, see [Run on minikube](#run-on-minikube))

---

## Project structure

```text
.
├── backend/              # Spring Boot API
├── frontend/             # React UI
├── e2e/                  # Playwright UI E2E tests
├── docs/                 # BDD scenarios and teaching notes
├── architecture/         # Architecture description (Views and Beyond, UML)
├── k8s/                  # Kubernetes manifests for minikube
├── .env.example          # Documented Compose settings (copy to .env)
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

No setup is needed: without a `.env` file Compose uses the defaults described in [Configuration (.env)](#configuration-env).

When containers are ready:

- Frontend: [http://localhost:5173](http://localhost:5173)
- Backend API: [http://localhost:8080/api/news](http://localhost:8080/api/news)
- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- PostgreSQL: `localhost:5432`, published on `127.0.0.1` only, for a database client or a backend run outside Docker (the containers themselves do not need it)

Stop everything:

```bash
docker compose down
```

Stop and remove DB volume (clean reset):

```bash
docker compose down -v
```

---

## Containers

| Service | Base image | Runs as | Listens on (container) | Published on host |
|---------|------------|---------|------------------------|-------------------|
| `frontend` | `nginxinc/nginx-unprivileged:1.31-alpine` | `nginx` (uid 101) | 8080 | `5173` |
| `backend` | `eclipse-temurin:27-jre-alpine` | `app` (uid 10001) | 8080 | `8080` |
| `db` | `postgres:16-alpine` | `postgres` (server processes) | 5432 | `127.0.0.1:5432` |

Neither application container runs as root:

```bash
docker compose exec backend whoami    # app
docker compose exec frontend whoami   # nginx
```

The frontend's nginx listens on 8080 instead of 80, because binding a port below 1024 has traditionally required root. Compose maps it to 5173 on the host, so the URL does not change. The frontend and backend both listen on 8080 without clashing because each container has its own network namespace.

`docker compose exec db whoami` prints `root`: the Postgres image starts as root and its entrypoint switches to the `postgres` user before starting the server, but `exec` uses the image's default user.

---

## Run on minikube

The same three containers also run on a local Kubernetes cluster. The manifests are in [`k8s/`](k8s/): one commented file per component, numbered in the order they are applied. Docker Compose is not affected and works as before.

| File | Creates |
|------|---------|
| `00-namespace.yaml` | Namespace `student-news` |
| `01-secret.yaml.example` | Example of the Secret `db-credentials`, with placeholders (never applied) |
| `02-postgres.yaml` | PersistentVolumeClaim, Deployment (1 replica) and Service `db` |
| `03-backend.yaml` | ConfigMap, Deployment (2 replicas) and Service `backend` |
| `04-frontend.yaml` | Deployment (2 replicas) and Service `frontend` |

### In short

With Docker Desktop running, from the project root:

```bash
minikube start

kubectl apply -f k8s/00-namespace.yaml
kubectl create secret generic db-credentials \
  --namespace student-news \
  --from-literal=POSTGRES_DB=student_news \
  --from-literal=POSTGRES_USER=student \
  --from-literal=POSTGRES_PASSWORD='choose-your-own'

./k8s/up.sh   # build the images, load them, apply the manifests, wait
```

Then, each in its own terminal:

```bash
kubectl port-forward -n student-news service/backend 8080:8080
kubectl port-forward -n student-news service/frontend 5173:8080
```

Open [http://localhost:5173](http://localhost:5173). When you are done: `kubectl delete namespace student-news` and `minikube stop`.

The rest of this section goes through the same steps one command at a time and explains each.

### Prerequisites

- Docker Desktop installed and running
- [minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)

On macOS: `brew install minikube kubectl`.

### 1. Start the cluster

```bash
minikube start
kubectl config current-context   # must print: minikube
```

`kubectl` always talks to the *current context*. If the second command prints anything else, switch with `kubectl config use-context minikube` before you go on.

### 2. Build the images and load them into minikube

```bash
docker build -t student-news-backend:1.0 ./backend
docker build -t student-news-frontend:1.0 ./frontend

minikube image load student-news-backend:1.0
minikube image load student-news-frontend:1.0
```

minikube has its own container runtime and cannot see the images in Docker Desktop. There is no registry to pull from either, so `minikube image load` copies each image into the cluster. The tag is an explicit version, never `:latest`: for a `:latest` image Kubernetes by default pulls from a registry on every start, and these images only exist locally.

### 3. Create the namespace and the Secret

```bash
kubectl apply -f k8s/00-namespace.yaml

kubectl create secret generic db-credentials \
  --namespace student-news \
  --from-literal=POSTGRES_DB=student_news \
  --from-literal=POSTGRES_USER=student \
  --from-literal=POSTGRES_PASSWORD='choose-your-own'
```

The password is yours to choose, and the real Secret is never committed. Git only holds [`k8s/01-secret.yaml.example`](k8s/01-secret.yaml.example), which shows the same Secret as a manifest with placeholder values. If you prefer a file, copy it to `k8s/01-secret.yaml` (git-ignored) and edit the copy.

Postgres reads these values only when its data directory is first created, the same as under Compose. To change the password later, start from an empty volume: do the [teardown](#teardown) and begin again from this step.

### 4. Apply the manifests

```bash
kubectl apply -f k8s/
```

### 5. Wait for the rollout

```bash
kubectl rollout status deployment/db -n student-news
kubectl rollout status deployment/backend -n student-news
kubectl rollout status deployment/frontend -n student-news

kubectl get pods,svc,pvc -n student-news
```

Every pod should be `1/1 Running` and the PersistentVolumeClaim `Bound`. A `1` under `RESTARTS` for the backend pods is normal on the first start (see [Troubleshooting](#troubleshooting)).

Steps 2, 4 and 5 in one command: [`./k8s/up.sh`](k8s/up.sh).

### 6. Open the app

Run each command in its own terminal and leave it running:

```bash
kubectl port-forward -n student-news service/backend 8080:8080
```

```bash
kubectl port-forward -n student-news service/frontend 5173:8080
```

Then open [http://localhost:5173](http://localhost:5173). The API and Swagger UI are at the same addresses as under Compose, for example [http://localhost:8080/api/news](http://localhost:8080/api/news).

Why the backend needs a port-forward too: the React code runs in your browser, not in the cluster, and it calls `http://localhost:8080/api/news`, an address compiled into the JavaScript when the image is built. So it is the browser on your laptop that must reach the backend, and a Service name such as `backend` only resolves inside the cluster. The port-forward makes `localhost:8080` on your machine lead to the backend Service, exactly the address the frontend already uses, so no code changes.

A port-forward is a tunnel to **one** pod behind the Service. When that pod is deleted or replaced by a rollout, the command prints `lost connection to pod` and exits. The app is still running; start the port-forward again.

### Teardown

```bash
kubectl delete namespace student-news   # the app, the Secret and the data
minikube stop                           # stop the cluster, keep it for next time
minikube delete                         # or remove the cluster completely
```

### Troubleshooting

Start with `kubectl get pods -n student-news`, then `kubectl describe pod <name> -n student-news` and read the `Events` at the bottom.

| Symptom | Cause | Fix |
|---------|-------|-----|
| Backend or frontend pod is `ErrImagePull` / `ImagePullBackOff` | The image is not inside minikube: it was not loaded, or the tag in the manifest differs from the tag you built. A `:latest` tag has the same effect, because Kubernetes then pulls from a registry by default, and these images are in no registry. | `minikube image ls \| grep student-news` shows what the cluster has. Build with the tag from the manifest (`1.0`), run `minikube image load` again, and never use `:latest`. |
| Backend pods are `Error` or `CrashLoopBackOff` right after `kubectl apply`, with `RESTARTS` going up | The backend started before Postgres was ready. Spring Boot exits when it cannot connect, and Kubernetes restarts it. | Wait: it recovers once the `db` pod is `1/1`. `kubectl logs deployment/backend -n student-news --previous` shows `Connection to db:5432 refused`. If it never recovers, check the `db` pod and the Secret. |
| `postgres-data` stays `Pending`, and so does the `db` pod | Nothing is creating the volume: the cluster has no default StorageClass. | `kubectl get storageclass` should list `standard (default)`. If not: `minikube addons enable default-storageclass` and `minikube addons enable storage-provisioner`. `kubectl describe pvc postgres-data -n student-news` shows the reason. |
| Pods are `CreateContainerConfigError` | The Secret `db-credentials` does not exist in the namespace. | Create it (step 3). The pods start on their own. |
| The page opens but shows an error instead of the news list | The browser cannot reach `localhost:8080`: the backend port-forward is not running, or it exited when its pod was replaced. | Start the backend port-forward again (step 6). |

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

## Configuration (.env)

Docker Compose reads the database settings from environment variables, with defaults so the stack runs without any setup:

| Variable | Default | Used by |
|----------|---------|---------|
| `POSTGRES_DB` | `student_news` | `db` (creates the database), `backend` (JDBC URL) |
| `POSTGRES_USER` | `student` | `db`, `backend` |
| `POSTGRES_PASSWORD` | `student` | `db`, `backend` |

To change them, copy the documented example and edit the copy:

```bash
cp .env.example .env
```

`.env` is git-ignored; `.env.example` is committed and holds placeholders only. CI runs without a `.env` file, on the defaults.

Postgres applies these values only when its data volume is first created. After changing them, recreate the volume with `docker compose down -v`.

The backend reads:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Compose sets them from the variables above. The JDBC URL uses the service name as host name: `jdbc:postgresql://db:5432/student_news`. Started outside Docker, the backend falls back to `jdbc:postgresql://localhost:5432/student_news` with user and password `student` (see `application.properties`); if you changed the values in `.env`, set the three `SPRING_DATASOURCE_*` variables to match.

---

## Database migrations (Flyway)

Schema changes are managed with Flyway SQL migrations.

- migration folder: `backend/src/main/resources/db/migration`
- naming format: `V{number}__{description}.sql`
- example: `V2__add_news_summary_column.sql`

Current setup:
- Flyway runs automatically on backend startup
- Hibernate uses `validate` mode (it validates schema, but does not create/update tables)

To add a new migration:
1. Create a new SQL file in `db/migration` with next version number.
2. Put DDL/DML changes in this file.
3. Rebuild/restart backend (`docker compose up --build`).

If you already have a non-empty database without Flyway history, `baseline-on-migrate=true` is enabled for easier local onboarding.

---

## Testing (backend)

From `backend/`:

```bash
mvn test
```

- **Unit tests** (`NewsServiceTest`): Mockito-based; no database.
- **E2E REST tests** (`NewsApiE2ETest`): `@SpringBootTest` + `@AutoConfigureMockMvc` + **Testcontainers** PostgreSQL. They exercise the full stack (HTTP → controller → service → JPA → real DB, with Flyway migrations). **Docker must be running** so Testcontainers can start Postgres.

Shared Testcontainers configuration lives in `AbstractPostgreSQLIntegrationTest`.

### UI E2E tests (Playwright)

Browser tests drive the real React UI (clicks, typing) against a **Docker Compose** stack (Postgres + API + Nginx frontend), so the full path **UI → REST → DB** is exercised.

From the repository root:

```bash
cd e2e
npm ci
npx playwright install chromium
npm test
```

Default behaviour:

- **Global setup** runs `docker compose up -d --build` from the repo root, then waits until `http://localhost:8080/api/news` and `http://localhost:5173/` respond.
- **Global teardown** runs `docker compose down -v` (also mirrored in CI with `if: always()`).

If the stack is already running and you want to **skip** Compose management:

```bash
cd e2e
E2E_SKIP_DOCKER=1 npm test
```

Optional environment variables (defaults suit local Docker Compose port mapping):

| Variable | Purpose |
|----------|---------|
| `E2E_SKIP_DOCKER` | Set to `1` to skip `docker compose up` / `down` in Playwright global hooks |
| `PLAYWRIGHT_BASE_URL` | UI base URL (default `http://localhost:5173`) |
| `E2E_API_NEWS_URL` | REST base for test cleanup via `APIRequestContext` (default `http://localhost:8080/api/news`) |
| `E2E_API_HEALTH_URL` | URL polled in global setup (default `http://localhost:8080/api/news`) |
| `E2E_WEB_HEALTH_URL` | URL polled in global setup (default `http://localhost:5173/`) |

HTML report (after a run): `cd e2e && npx playwright show-report`

### CI (GitHub Actions)

Workflow [`.github/workflows/e2e.yml`](.github/workflows/e2e.yml) runs on pushes and pull requests to `main` / `master`: installs Playwright Chromium, runs `npm test` in `e2e/` (which starts Docker via global setup), uploads the Playwright HTML report on failure, and always runs `docker compose down -v`.

---

## Notes

- This project is intentionally simple and does **not** include authentication/authorization.
- It is designed for demo and local development purposes.

---

## Functional requirements (BDD)

BDD scenarios are documented in separate files:

- [API testing scenarios (BDD)](docs/api-testing-scenarios-bdd.md)
- [Frontend user scenarios (BDD)](docs/frontend-user-scenarios-bdd.md)

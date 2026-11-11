# Docker teaching notes — Week 12

Notes for the Week 12 lecture on containers and Docker Compose. Everything
below refers to real files in this repository. "Before" means commit `6e6d3eb`,
the last commit before the container hardening; "after" is the current tree.

## Before / after

| | Before (`6e6d3eb`) | After |
|---|---|---|
| Backend image size | 606 MB | 400 MB |
| Frontend image size | 76.1 MB | 76.3 MB |
| Backend process runs as | `root` | `app` (uid 10001) |
| Frontend processes run as | `root` master, `nginx` workers | `nginx` (uid 101), all of them |
| Backend rebuild after a one-line Java change | 76 s, of which the Maven step 72.8 s (every dependency downloaded again) | 10–28 s over three runs; Maven step 5.7–9.6 s, dependency layer `CACHED` |
| Frontend build context | 48 MB (local `node_modules` included) | 1.2 kB |
| React version in a locally built frontend image | 18.3.1 (copied from a stale local `node_modules`) | 19.3.1, as pinned in `package-lock.json` |
| Java: build / runtime / CI | JDK 26 / JRE 25 / JDK 21 | 21 / 21 / 21 |
| Database credentials | hard-coded twice in `docker-compose.yml` | `${POSTGRES_*:-default}` variables, documented in `.env.example` |
| Postgres port on the host | `0.0.0.0:5432` | `127.0.0.1:5432` |

Measured with Docker Desktop (Engine 29.9.3) on Apple Silicon, with base images
already pulled and a warm build cache. Total rebuild times vary from run to
run, mostly because exporting the image took anywhere from 2 to 12 s; the
Maven step is the stable signal.

## What changed, and which principle it shows

**`.dockerignore` in `backend/` and `frontend/` — send only what the image
needs.** The frontend Dockerfile does `COPY . .`, so without an ignore file the
whole directory went into the build context, local `node_modules` included
(48 MB). It was also copied *over* the clean install inside the image: a clone
whose `node_modules` had not been refreshed since the React upgrade produced an
image bundling React 18.3.1 while `package.json` asks for 19.3.1. An image that
depends on the state of the developer's disk is not reproducible. For the
backend the file is a safeguard: BuildKit already sent only `pom.xml` and
`src/` (35 kB), because those are the only paths the Dockerfile copies, but a
future `COPY . .` would otherwise pick up `target/` and IDE files.

**Backend: dependencies before code — layer caching.** Each instruction is a
layer, and a layer is rebuilt when its inputs change, along with every layer
after it. `pom.xml` and `src/` used to be copied together before
`mvn package`, so editing one Java line threw away the downloaded dependencies.
Now `pom.xml` is copied alone and `mvn dependency:go-offline` runs before
`src/` is copied, so the dependency layer is reused until the pom changes.
`go-offline` does not resolve every plugin dependency, which is why the
package step still runs online rather than with `-o`.

**Backend: one Java version and a smaller runtime image — minimal base
image.** The image compiled with JDK 26 and ran on JRE 25, while `pom.xml`
targets 21 and CI runs the unit tests on JDK 21, so CI never tested the JVM
that shipped. Both stages now use 21, an LTS release. The runtime stage uses
`eclipse-temurin:21-jre-alpine`: a JRE without compiler or Maven, on Alpine
instead of Ubuntu. That is the 606 → 400 MB. Most of what remains is the JRE
itself and the 60 MB application jar. The multi-stage build is what makes this
possible: Maven and the JDK exist only in the `build` stage, which is
discarded.

**Backend: non-root user — least privilege.** The runtime stage creates an
`app` user and switches to it with `USER 10001:10001`. A numeric id lets
Kubernetes check `runAsNonRoot` (Week 14). The jar is still owned by root and
only readable by the process, so a compromised application cannot replace its
own code.

**Frontend: `npm ci` instead of `npm install` — reproducible builds.**
`npm install` may resolve newer versions than the lock file. `npm ci` installs
exactly what `package-lock.json` pins, the same as the CI lint and build jobs,
and fails if the lock file is missing or out of date.

**Frontend: unprivileged nginx — non-root user.** The official `nginx` image
starts its master process as root. `nginxinc/nginx-unprivileged:1.27-alpine` is
the same nginx release with the same default configuration, but every process
runs as uid 101. It listens on 8080 instead of 80, because binding a port below
1024 has traditionally required root. Compose maps `5173:8080`, so the URL is
unchanged. Frontend and backend both listen on 8080 without clashing because
each container has its own network namespace.

**Compose: settings from the environment, `.env.example` committed — no
secrets in images or in git.** The database name, user and password were
written into `docker-compose.yml` twice. They are now `POSTGRES_DB`,
`POSTGRES_USER` and `POSTGRES_PASSWORD`, shared by `db` and `backend`, with
`${VAR:-default}` fallbacks equal to the old values, so CI and a fresh clone
need no `.env`. `.env.example` lists each variable with a comment; `.env` is
git-ignored. Credentials reach the containers at run time, so neither image
contains them: `docker image inspect` and `docker history` on the backend image
show no password.

**Compose: Postgres published only on localhost — service names as host
names.** The backend reaches the database as `db:5432` over the project
network that Compose creates; it never used the published port, and neither do
the tests (Testcontainers starts its own database). The port stays published
because a backend run from the IDE connects to `localhost:5432`, the default in
`application.properties`. Binding it to `127.0.0.1` keeps the demo credentials
off the network.

## Live demo — 15 minutes

**Before class.** Run `docker compose down -v`, then `docker compose build`
once, so base images and the dependency layer are cached and the demo is not
waiting on the network. Prepare the old backend Dockerfile for the comparison
in step 6 and build it once:

```bash
git show 6e6d3eb:backend/Dockerfile > /tmp/Dockerfile.before
docker build -f /tmp/Dockerfile.before -t student-news-backend:before backend
```

1. **Build (2 min).** Images and layers.

   ```bash
   docker compose build
   docker images | grep student-news
   docker history student-news-backend
   ```

   An image is a read-only stack of layers, one per Dockerfile instruction.
   The history shows only the runtime stage: the JRE base layers (163 MB is the
   JRE), the `adduser` layer and the 60 MB jar. Maven, the JDK and the
   downloaded dependencies stayed in the `build` stage, which is not part of
   the image.

2. **Three containers (2 min).** One command for the whole stack.

   ```bash
   docker compose up -d
   docker compose ps
   ```

   Two images were built here; `postgres:16-alpine` came from a registry
   (Docker Hub). Open http://localhost:5173 and create a news item in the UI.

3. **`db` in the JDBC URL (2 min).** Service names are host names.

   ```bash
   docker compose exec backend sh -c 'echo $SPRING_DATASOURCE_URL'
   docker compose exec backend getent hosts db
   ```

   Compose put the three containers on one network and registered each
   service name in its DNS. The backend never touches `localhost:5432`.

4. **Data survives `down` / `up` (2 min).** Containers are disposable; the
   named volume is not.

   ```bash
   curl -s -X POST localhost:8080/api/news -H 'Content-Type: application/json' \
     -d '{"title":"Still here?","content":"Written before docker compose down"}'
   docker compose down
   docker volume ls | grep student-news
   docker compose up -d
   until curl -sf localhost:8080/api/news >/dev/null; do sleep 2; done
   curl -s localhost:8080/api/news
   ```

   `down` removed the containers and the network but not
   `student-news_postgres_data`.

5. **`down -v` wipes it (1 min).**

   ```bash
   docker compose down -v
   docker compose up -d
   until curl -sf localhost:8080/api/news >/dev/null; do sleep 2; done
   curl -s localhost:8080/api/news     # []
   ```

6. **One-line change: before and after (4 min).** Layer order decides what a
   rebuild costs.

   ```bash
   echo "// demo" >> backend/src/main/java/com/example/studentnews/StudentNewsApplication.java
   time docker compose build --progress=plain backend
   time docker build --progress=plain -f /tmp/Dockerfile.before -t student-news-backend:before backend
   git checkout -- backend/src
   ```

   In the first build `dependency:go-offline` shows `CACHED` and the Maven step
   takes seconds. In the second, the old Dockerfile copies `src/` before Maven
   runs, so the same one-line change downloads every dependency again (about
   75 s here, longer on classroom Wi-Fi).

7. **`whoami` (2 min).** Nothing we built runs as root.

   ```bash
   docker compose exec backend whoami     # app
   docker compose exec frontend whoami    # nginx
   docker run --rm --entrypoint whoami student-news-backend:before   # root
   docker image inspect student-news-backend --format '{{.Config.Env}}'
   ```

   The last command shows that the image carries no database password: it
   arrives at run time from Compose. Clean up with
   `docker compose down -v` and `docker image rm student-news-backend:before`.

## Things that will come up

- **"The frontend image did not get smaller."** It was not meant to: it is the
  same nginx on the same Alpine. The change there was the user, not the size.
- **"`docker compose exec db whoami` says root."** The Postgres image starts as
  root and its entrypoint switches to the `postgres` user before starting the
  server; `ps` inside the container shows the server processes as `postgres`.
  `exec` uses the image's default user, which is root.
- **"I changed `.env` and now the backend cannot log in."** Postgres applies
  `POSTGRES_*` only when the volume is first created. Run
  `docker compose down -v` after changing them.
- **"So environment variables are secret?"** No. `docker inspect` on a running
  container shows them. They keep credentials out of images and out of git;
  real secret handling (Compose `secrets:`, Kubernetes Secrets) comes later.
- **Dependabot will propose newer Java base images** (it has proposed 25 and 26
  before). Moving the images alone would split build, runtime and CI again;
  change `pom.xml`, `ci.yml` and both Dockerfile stages together or not at all.

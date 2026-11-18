# Getting started

## 1. Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 21 (Temurin) | `java -version` |
| Node.js | 24 | `node -v` |
| npm | 11 | `npm -v` |
| Docker (optional) | 29 | `docker version` |

Maven is **not** needed: each Java service ships a Maven wrapper (`mvnw` / `mvnw.cmd`) that downloads Maven 3.9.12 on
first use.

## 2. Clone and install

```bash
git clone https://github.com/JaNigam/stackcraft.git
cd stackcraft
npm ci --prefix apps/web
```

## 3. Run the three services (no Docker)

Open three terminals in the repo root.

```bash
cd services/runner && ./mvnw spring-boot:run
```

```bash
cd services/api && ./mvnw spring-boot:run
```

```bash
cd apps/web && npm run dev
```

Then open http://localhost:5173.

| Service | Port | Notes |
|---|---|---|
| runner | 8091 | binds to 127.0.0.1 only; never reachable from the browser |
| api | 8080 | `dev` profile: H2 file database in `services/api/data/`, content hot reload |
| web | 5173 | Vite dev server; proxies `/api` to the api |

**Port 8080 taken?** (common on Windows, where Hyper-V reserves port ranges) Run the api on another port and point the
web proxy at it:

```bash
cd services/api && PORT=8081 ./mvnw spring-boot:run
```

```bash
cd apps/web && VITE_API_PROXY=http://localhost:8081 npm run dev
```

In the Claude desktop app, `.claude/launch.json` already does this (api on 8081).

**Windows PowerShell:** use `.\mvnw.cmd spring-boot:run` and `$env:PORT=8081` instead.

## 4. Or run everything with Docker

```bash
docker compose -f infra/docker-compose.yml up --build
```

Open http://localhost:8088. This also starts Postgres; the api uses the `prod` profile.

## 5. Offline mode (no backend at all)

Start only the web app (`npm run dev`), or add `?offline` to any URL. The header shows **Offline mode**:

- content comes from the bundle (the same YAML the api reads, compiled into the site);
- progress, XP and the Trap Board are saved in your browser;
- code blocks show their verified output instead of running.

## 6. On your phone, no setup

Open https://janigam.github.io/stackcraft/ . It is the offline-mode build, redeployed on every push to `main`.
To build the same thing locally (Git Bash needs `MSYS_NO_PATHCONV=1` or it rewrites `/stackcraft/` into a Windows path):

```bash
cd apps/web && MSYS_NO_PATHCONV=1 VITE_BASE=/stackcraft/ VITE_STATIC_SITE=true npm run build && npx vite preview
```

## 7. Smoke test

1. Home page shows the Java Core series and the header says **Online**.
2. Open Lesson 0.1, press **Load the tour** in the playground: the HashSet prints `[banana, apple, kiwi, mango]`.
3. Press **Run** on the tour code: status **OK**, output identical to the stored one.
4. Answer the first warm-up question: the XP bar moves and survives a page reload.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Header says **Offline mode** while the api runs | the web proxy points at the wrong port: set `VITE_API_PROXY` |
| Run always shows **Stored output** | the runner is down, or the circuit breaker is open: wait 15 s after starting the runner |
| api fails with `Content root not found` | start it from `services/api`, or set `STACKCRAFT_CONTENT_ROOT` to the absolute `content` path |
| api fails with an invalid chapter | run `node scripts/verify-content.mjs`; it names the file and field |
| `Port 5173 is already in use` | an old Vite is still running; stop it (Vite uses `strictPort` so the proxy setup stays predictable) |

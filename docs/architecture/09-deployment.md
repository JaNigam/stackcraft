# 9. Deployment

## Images (`infra/docker/`)

| Image | Build | Runtime |
|---|---|---|
| `web.Dockerfile` | `node:24-alpine`: `npm ci`, copies `content/` and `docs/api`, `npm run build` | `nginx:1.29-alpine` serving `dist/`, proxying `/api` to `api:8080` |
| `api.Dockerfile` | `eclipse-temurin:21-jdk`: Maven wrapper `package` | `eclipse-temurin:21-jre`, non-root user, `content/` baked in, `SPRING_PROFILES_ACTIVE=prod` |
| `runner.Dockerfile` | `eclipse-temurin:21-jdk`: Maven wrapper `package` | `eclipse-temurin:21-jdk` (needs `javac` + `java`), non-root user, `RUNNER_BIND=0.0.0.0` |

All three use the repository root as the build context. Health checks hit `/actuator/health/readiness`.

## Compose (`infra/docker-compose.yml`)

```mermaid
flowchart LR
  subgraph edge network
    web[web :8088] --> api
  end
  subgraph internal network, no outside access
    api --> db[(postgres:17)]
    api --> runner
  end
```

- Only `web` publishes a port (8088). The runner and the database are on an `internal: true` network.
- Start order is driven by health checks: db → runner → api → web.
- The runner gets defence in depth: read-only root, 64 MB tmpfs `/tmp`, `pids_limit`, memory and CPU limits,
  `cap_drop: ALL`, `no-new-privileges`.
- `POSTGRES_PASSWORD` defaults to a dev-only value; set it in the environment for anything real.

```bash
docker compose -f infra/docker-compose.yml up --build
```

## Static-only hosting

`apps/web/dist` works on its own: with no `/api` it boots in offline mode. `.github/workflows/pages.yml` publishes it
to **https://janigam.github.io/stackcraft/** on every push to `main` that touches the web app or content: it runs the
web tests and `verify-content`, builds with `VITE_BASE=/stackcraft/` and `VITE_STATIC_SITE=true` (skip the api probe,
header shows "Static preview"), and copies `index.html` to `404.html` so deep links work. No api or runner is deployed.

## What the Cloud series adds

- CI in `.github/workflows/`: the four checks from [Building and testing](../building-and-testing.md) on every PR,
  images pushed to a registry.
- `infra/k8s/`: Deployments with the readiness and liveness probes already exposed, a NetworkPolicy that only lets
  the api reach the runner, HPA on the runner by CPU.
- `infra/terraform/`: managed Postgres, a registry, the cluster.
- The per-run sandbox (ADR 0003) and authentication, before anything is public.

## Interview angle

"Take this from docker compose to production." Probes, network policies, secrets from a manager, horizontal scaling
of the stateless tiers, and isolating the untrusted tier on its own node pool.

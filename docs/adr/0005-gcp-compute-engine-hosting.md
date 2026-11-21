# ADR 0005: Host on a Google Compute Engine VM with Docker Compose

- Status: Proposed (infrastructure not created yet)
- Date: 2026-11-22

## Context
The full stack (web, api, runner, Postgres) should be reachable on the public internet, and deploying it is also the
learning goal of the Cloud series: projects, billing, IAM, networking, registries, secrets, CI/CD and Terraform on GCP.
The budget is the $300 new-account trial plus the monthly credit that comes with Google AI Pro, so cost has to stay
small and visible. ADR 0003 says the runner must not be public until snippets are sandboxed.

## Options considered
| Option | For | Against |
|---|---|---|
| **Compute Engine VM + Docker Compose** | Same compose file as local; teaches VPC, firewall, IAM, service accounts; one bill line; runner can be isolated | We patch and operate the VM |
| Cloud Run (api, web) + Cloud SQL | Managed, scales to zero | Cloud SQL is not free; the runner needs a separate private service; many moving parts for a first deploy |
| GKE | The industry platform | Control-plane and node cost; far more than a portfolio needs. A later chapter, not the first deploy |

## Decision
One Compute Engine VM (start with `e2-medium`, 2 vCPU and 4 GB; the free-tier `e2-micro` is too small for the JVMs)
running `infra/compose.prod.yml`:

- **Caddy** is the only published service (ports 80 and 443). It gets and renews a TLS certificate automatically and
  forwards everything to the `web` container. `web` serves the SPA and forwards `/api` to the api, exactly as locally.
- `api`, `runner` and `db` live on the `internal` Docker network, which has no route to the internet. The runner is
  never published (ADR 0003).
- Images are built in GitHub Actions and stored in **Artifact Registry**; the VM only pulls them.
- Secrets (JWT signing key, database password) live in **Secret Manager** and are written to the VM's `.env` at
  deploy time. Nothing secret is in Git or in an image.
- GitHub Actions authenticates to GCP with **Workload Identity Federation** (short-lived tokens, no JSON key file).
- Infrastructure is declared in **Terraform** (`infra/terraform`), so it can be recreated and destroyed on demand.
- A billing **budget alert** is created first.

## Runner isolation before it is public
Public access to Run needs all of these, in this order of cost:
1. Sign-in required to run, plus a per-user rate limit in the api.
2. The runner container: no internet (internal network), read-only filesystem, capped processes and memory, no
   privileges (already in the compose file).
3. gVisor (`runsc`) as the container runtime for the runner, so snippets never touch the VM's real kernel.
Until 3 is verified on the VM, the public site keeps Run disabled and shows the stored, verified output.

## Consequences
- We own OS updates, Docker upgrades and backups (nightly `pg_dump` to a Cloud Storage bucket).
- A single VM is a single point of failure. Acceptable for a portfolio; the HLD series can discuss what changes at scale.
- Moving to Cloud Run or GKE later is a deployment change only, because the images and ports do not change.
- Costs are estimates until the Pricing Calculator and the first invoice confirm them.

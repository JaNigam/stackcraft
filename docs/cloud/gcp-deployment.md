# Deploying Stackcraft to Google Cloud (a learning path)

Decision and trade-offs: [ADR 0005](../adr/0005-gcp-compute-engine-hosting.md). This page is the step-by-step, with
**what you do**, **what the repo does**, and **what you learn** in each phase. Costs below are estimates: confirm them
in the [Pricing Calculator](https://cloud.google.com/products/calculator) and watch the billing report.

## The mental model (read this first)

Think of GCP as a **city**.

| GCP word | City analogy | In Stackcraft |
|---|---|---|
| **Billing account** | The bank account every bill is charged to | Holds the trial credit, the AI Pro credit and a card |
| **Project** | One building you rent. Everything inside shares a bill, permissions and an address | `stackcraft-prod` |
| **IAM** (principal + role) | Key cards: *who* may do *what* on *which* thing | You (owner), a deployer service account, the VM's own account |
| **Service account** | A key card for a program instead of a person | The VM pulls images with one; GitHub Actions deploys with another |
| **VPC network + firewall** | The building's walls and doors | Only ports 80 and 443 (and SSH from your IP) are open |
| **Compute Engine VM** | A rented flat with its own front door | Runs Docker Compose |
| **Artifact Registry** | A warehouse for container images | `web`, `api`, `runner` images |
| **Secret Manager** | A safe | JWT signing key, database password |
| **Cloud DNS / hostname** | The street name | `stackcraft.<yourdomain>` or an IP-based name |
| **Budget alert** | A smoke alarm for money | Emails at 50, 90 and 100 percent |

The rule that explains most GCP errors: **a request succeeds only if the right principal has the right role on the right
resource, in the right project, with the API enabled.** "Permission denied" almost always means one of those four.

## Phases

### Phase 0: Account, safety nets (you, about 30 minutes)
1. Sign in at console.cloud.google.com, activate the free trial, and claim the Google AI Pro credit on the
   Developer Program "My Benefits" page (attach it to the same billing account). A card is required.
2. Create a **budget alert** (Billing, Budgets and alerts) *before* creating anything else.
3. Create the project and note its **project ID** (it differs from the name and cannot be changed).
4. Install the `gcloud` CLI and run `gcloud auth login` and `gcloud config set project <id>`.

*You learn:* billing accounts vs projects, why budgets exist, and how the CLI, Console and Terraform all call the same APIs.

### Phase 1: Prove the stack runs in production mode, locally (repo, this branch)
`docker compose -f infra/docker-compose.yml up --build` already does this. `infra/compose.prod.yml` is the version for
the VM: it pulls images instead of building them and puts Caddy in front for HTTPS.

*You learn:* the difference between a build step and a deploy step, and why images are immutable.

### Phase 2: Images in Artifact Registry
1. Enable the Artifact Registry API; create a Docker repository in one region.
2. Build and push `web`, `api` and `runner` (by hand first with `gcloud builds submit` or `docker push`, then by CI in Phase 5).

*You learn:* enabling APIs, registries and image tags, and why the Git commit SHA is a good tag.

### Phase 3: The VM and the network
1. Create an `e2-medium` Debian/Ubuntu VM with a **dedicated service account** that may only read Artifact Registry
   and Secret Manager (least privilege), and a static external IP.
2. Firewall: allow tcp 80 and 443 from anywhere; SSH only from your IP (or use IAP tunnelling).
3. Install Docker, copy `infra/` to the VM, create `infra/.env` from [env.example](../../infra/env.example), run the compose file.
4. Point a hostname at the IP (a domain you own, or a name like `34-1-2-3.sslip.io`) and set `SITE_ADDRESS`.

*You learn:* VPCs, firewall rules, service accounts vs your own account, and how Caddy proves to Let's Encrypt that
you own the name (it answers a challenge on port 80).

### Phase 4: Secrets and the runner sandbox
1. Move the JWT key and DB password into Secret Manager; the VM reads them at deploy time.
2. Install gVisor on the VM, enable `runtime: runsc` for the runner, and test that a snippet cannot read the host or
   reach the internet. Only then enable Run for signed-in users, with a per-user rate limit.

*You learn:* secret rotation, why "no secret in an image" matters, and container isolation (namespaces, cgroups, gVisor).

### Phase 5: Terraform and CI/CD
1. Describe Phases 2 to 4 in `infra/terraform` so the whole environment can be created with `terraform apply`
   and destroyed with `terraform destroy` (a good habit for staying inside the budget).
2. GitHub Actions: on merge to `main`, build and push images, then deploy to the VM. Authenticate with
   **Workload Identity Federation**: GitHub proves who it is with a short-lived token, so there is no JSON key to leak.

*You learn:* infrastructure as code, state files (never commit them), federated identity, and rollbacks by re-deploying an old tag.

### Phase 6: Operate it
Nightly `pg_dump` to a Cloud Storage bucket, an uptime check, log viewing in Cloud Logging, and OS updates.

## Interview angle
"Walk me through deploying a Spring Boot service on GCP." Answer with this page: image in a registry, a VM or Cloud Run
service running under a least-privilege service account, secrets from Secret Manager, a firewall that exposes only the
proxy, CI that deploys by short-lived federated identity, and a budget alert. Then say what you would change at
scale: managed Postgres (Cloud SQL), Cloud Run or GKE, a load balancer, and a separate sandbox service for untrusted code.

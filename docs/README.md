# Stackcraft docs

Start here, then go as deep as you need.

| Read this | When |
|---|---|
| [Getting started](getting-started.md) | You want it running on your machine in 10 minutes |
| [Building and testing](building-and-testing.md) | You are about to change code and want to know what proves it still works |
| [Authoring chapters](authoring-chapters.md) | You are writing or editing a lesson |
| [Architecture](architecture/README.md) | You want to know how the pieces are built and stitched together |
| [Style guide](STYLE-GUIDE.md) | You are drawing a diagram or building a widget |
| [API contract](api/openapi.yaml) | You are changing anything between web and api |
| [Decisions (ADRs)](adr/) | You want to know *why* something is the way it is |

## Architecture chapters

1. [System overview](architecture/01-system-overview.md): three services, one repo, and why
2. [Request flows](architecture/02-request-flows.md): what happens on load, answer, and Run
3. [The api service](architecture/03-api-service.md): hexagonal layers, features, ports and adapters
4. [The runner service](architecture/04-runner-service.md): compiling and running untrusted code safely
5. [The web app](architecture/05-web-app.md): feature-sliced React, services, renderer registry
6. [Content pipeline](architecture/06-content-pipeline.md): from a YAML file to a graded answer
7. [Data and progress](architecture/07-data-and-progress.md): the XP log, idempotency, schema
8. [Resilience and security](architecture/08-resilience-and-security.md): timeouts, circuit breaker, limits
9. [Deployment](architecture/09-deployment.md): Docker images, compose, and what the Cloud series adds

Each chapter ends with "Interview angle": the system-design or LLD question that part of Stackcraft answers.

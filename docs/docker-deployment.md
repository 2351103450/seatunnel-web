# Docker deployment

This document describes how to build and run SeaTunnel Web with Docker before a formal release.

## Prerequisites

- Docker Engine with BuildKit support
- Docker Compose v2
- Network access to download Maven, Node.js, and container dependencies during the first build

## Quick start

```bash
cp .env.example .env
docker compose up -d --build
```

SeaTunnel Web is available at <http://localhost:9527> by default.

## Configuration

The Compose stack reads settings from `.env`:

| Variable | Default | Description |
| --- | --- | --- |
| `SEATUNNEL_WEB_IMAGE` | `seatunnel-web:latest` | Image name used by Compose. |
| `SEATUNNEL_WEB_PORT` | `9527` | Host port mapped to the web service. |
| `MYSQL_ROOT_PASSWORD` | `seatunnel_root` | MySQL root password. |
| `MYSQL_DATABASE` | `seatunnel_web` | Application database name. |
| `MYSQL_USER` | `seatunnel` | Application database user. |
| `MYSQL_PASSWORD` | `seatunnel` | Application database password. |
| `MYSQL_PORT` | `3306` | Host port mapped to MySQL. |
| `JAVA_OPTS` | empty | Extra JVM options. |
| `APP_OPTS` | empty | Extra Spring Boot command-line options. |

## Build only

```bash
docker build -t seatunnel-web:latest .
```

The root `Dockerfile` builds the frontend, packages the backend distribution, and
copies the generated distribution into a Java 21 runtime image.

## Database initialization

The Compose stack mounts `docker/mysql/init/01-seatunnel-web.sql` into MySQL's
entrypoint initialization directory. It is executed only when the `mysql-data`
volume is created for the first time. To re-run initialization locally, remove
volumes before starting the stack again:

```bash
docker compose down -v
docker compose up -d --build
```

## Useful commands

```bash
docker compose ps
docker compose logs -f seatunnel-web
docker compose down
```

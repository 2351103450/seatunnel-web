# Docker assets

This directory contains files used by the local Docker Compose deployment.

## MySQL initialization

`mysql/init/01-seatunnel-web.sql` is mounted into the official MySQL image at
`/docker-entrypoint-initdb.d`. MySQL runs it automatically when the `mysql-data`
volume is empty.

If the SQL changes after a previous local start, recreate the database volume:

```bash
docker compose down -v
docker compose up -d --build
```

# Database migrations

SeaTunnel Web keeps versioned database changes in this directory. New schema changes must be added as Flyway-style migration files instead of editing already released SQL in place.

Recommended rules:

- Back up the database before applying a new release.
- Use `V<version>__<description>.sql` for one-time schema/data migrations.
- Use `R__<description>.sql` only for repeatable reference data.
- Never mix future `ALTER TABLE` statements into an already released init script.
- Document rollback limitations and any job-content JSON migrations in release notes.

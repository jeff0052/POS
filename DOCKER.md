# Docker Run Guide

## Services

- `mysql`
- `pos-backend`
- `pc-admin`

## Start

```bash
docker compose up --build
```

## URLs

- PC Admin: http://localhost:5173/
- Backend: http://localhost:8080/
- MySQL: localhost:3306

## Notes

- Backend runs with `SPRING_PROFILES_ACTIVE=mysql` in Docker.
- MySQL credentials:
  - user: `root`
  - password: `root`
  - database: `pos_db`
- Current backend code expects tables to exist already.
- Initial demo tables and seed data are mounted from `./mysql/init/001-init.sql`.

## Next recommended step

Add database initialization SQL or Flyway migrations before relying on MySQL-backed APIs.

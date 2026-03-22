# AWS Deployment Guide

## Target
Deploy the current internal POS stack to a single Linux server on AWS.

This guide assumes:
- Ubuntu server
- Docker and Docker Compose installed
- code cloned from GitHub
- first deployment is for internal testing or staging

## Stack
- `nginx`
- `pc-admin`
- `pos-backend`
- `mysql`

## 1. Prepare the server

Install Docker and Compose first.

Example checks:
```bash
docker --version
docker compose version
```

## 2. Clone the repository
```bash
git clone https://github.com/jeff0052/POS.git
cd POS
```

## 3. Prepare environment variables
```bash
cp .env.example .env.prod
```

Edit `.env.prod` and set real values:
- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `APP_PORT`

For the current stack, keep:
- `MYSQL_USERNAME=root`

## 4. Start the stack
```bash
chmod +x deploy.sh
./deploy.sh
```

## 5. Check running services
```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f
```

## 6. Access
If `APP_PORT=80`, open:
- `http://your-server-ip/`

Health check:
- `http://your-server-ip/healthz`

The backend is reverse proxied under:
- `/api/`

## 7. Recommended next step
After first successful deployment, add:
- domain name
- HTTPS
- backups for MySQL
- a separate managed database for production

## 8. Notes
- This setup is suitable for internal testing and early pilot use
- It is not yet hardened for full production
- The current backend still contains scaffold-level business logic

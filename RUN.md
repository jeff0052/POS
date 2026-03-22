# Run Guide

## Version
`v0.1.0-internal-preview`

## Goal
This guide explains how to launch the current internal preview environment.

## Main Parts
- `android-preview-web`
- `pc-admin`
- `pos-backend`
- `docker-compose.yml`

## Recommended First View
Use the Android preview web first for UI review.

### Android Preview Web
Project:
- `/Users/ontanetwork/Documents/Codex/android-preview-web`

Typical local run:
```bash
cd /Users/ontanetwork/Documents/Codex/android-preview-web
npm install
npm run dev -- --host 0.0.0.0 --port 4175
```

Expected access:
- `http://localhost:4175/`

## Merchant Admin + Backend + MySQL
From the workspace root:
```bash
cd /Users/ontanetwork/Documents/Codex
docker compose up --build
```

Expected access:
- Merchant admin: `http://localhost:5173/`
- Backend: `http://localhost:8080/`

## Notes
- This is an internal preview environment
- Some screens are visual prototypes rather than production flows
- Payment and printer integrations are not connected yet

## Recommended Demo Order
1. Open Android preview web
2. Review Table Management
3. Review Ordering
4. Review Order Review
5. Review Payment flow
6. Open merchant admin if needed

## Release Files
- `VERSION`
- `RELEASE_NOTES.md`
- `RUN.md`

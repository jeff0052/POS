# Developer Templates

Use these templates when the user asks for a starter document set, an SOP pack, or a filled example for a new project.

## Default File Set

```text
docs/
  01-project.md
  02-requirements.md
  03-architecture.md
  04-tasks.md
  05-dev-log.md
  06-test-checklist.md
  07-release-checklist.md
```

## 01-project.md

```md
# Project Overview

## Goal
- What problem this project solves:
- Who it is for:
- Why it matters now:

## MVP Outcome
- The smallest version that is worth shipping:

## Non-Goals
- Not doing in this version:

## Constraints
- Time:
- Budget:
- Platform:
- Stack:

## Success Criteria
- User can:
- System can:
- Release is considered successful when:
```

## 02-requirements.md

```md
# Requirements

## Core Features
1. Feature:
   Done when:
2. Feature:
   Done when:

## Supporting Features
1. Feature:
   Done when:

## Deferred
- Feature:
- Feature:

## Risks and Unknowns
- Risk:
  Mitigation:
```

## 03-architecture.md

```md
# Architecture

## Stack
- Frontend:
- Backend:
- Database:
- Hosting:

## Modules
- Module:
  Responsibility:
- Module:
  Responsibility:

## Data Model
- Entity:
  Key fields:

## External Dependencies
- Service:
  Reason:

## Technical Risks
- Risk:
  Decision:
```

## 04-tasks.md

```md
# Task Plan

## Setup
- [ ] Task
  Done when:

## Core Flow
- [ ] Task
  Done when:

## Quality
- [ ] Task
  Done when:

## Release
- [ ] Task
  Done when:
```

## 05-dev-log.md

```md
# Development Log

## YYYY-MM-DD
- What changed:
- Why:
- Problems found:
- Follow-up:
```

## 06-test-checklist.md

```md
# Test Checklist

## Main Flow
- [ ] User can complete the main journey end to end
- [ ] Data is saved correctly
- [ ] Error messages are understandable

## Edge Cases
- [ ] Empty input
- [ ] Invalid input
- [ ] Retry after failure

## Release Confidence
- [ ] Logs are visible
- [ ] Critical paths were retested after latest change
```

## 07-release-checklist.md

```md
# Release Checklist

## Before Deploy
- [ ] Environment variables are set
- [ ] Database migration is ready
- [ ] Backup or rollback path is clear
- [ ] Monitoring or logs are accessible

## Deploy
- [ ] Deploy completed
- [ ] Smoke test passed

## After Deploy
- [ ] Core flow works in production
- [ ] Errors are monitored
- [ ] Next fixes or follow-ups captured
```

## SOP Output Shape

When the user asks for a custom SOP, use this outline:

```md
# [Project Name] Developer SOP

## Phase 1: Scope
- Entry criteria:
- Actions:
- Exit criteria:

## Phase 2: Plan
- Entry criteria:
- Actions:
- Exit criteria:

## Phase 3: Build
- Entry criteria:
- Actions:
- Exit criteria:

## Phase 4: Verify
- Entry criteria:
- Actions:
- Exit criteria:

## Phase 5: Release
- Entry criteria:
- Actions:
- Exit criteria:

## Phase 6: Iterate
- Entry criteria:
- Actions:
- Exit criteria:
```

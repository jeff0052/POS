---
name: developer
description: Turn a software idea into a lean, executable operating system for a solo builder or small software team. Use when Codex needs to help scope a product, define MVP boundaries, review a feature from business, product, engineering, QA, and release perspectives, choose essential documents, sequence delivery phases, create checklists, or convert rough ideas into a practical SOP, roadmap, and document set for building and shipping software.
---

# Developer

## Overview

Use this skill to transform a vague product idea into a practical development operating system with only the documents and process controls that materially reduce risk. Favor lightweight structure over enterprise ceremony.

Default to output that is immediately usable in Markdown. Keep recommendations opinionated, short, and sequenced so the builder can act on them today.

## Working Style

Assume the user is the primary builder unless they say otherwise.

Optimize for:
- MVP scope control
- fast execution
- low documentation overhead
- maintainability after a few weeks away from the project
- safe release and recovery

Avoid:
- heavyweight governance
- redundant documents
- role-based ceremonies that do not apply to a solo builder
- generic advice without concrete deliverables

## Core Workflow

Follow this sequence unless the user asks for only one piece.

### 1. Clarify the Build Target

Extract and state:
- who the software is for
- the main problem it solves
- the smallest successful first release
- explicit non-goals
- major constraints such as time, budget, stack, or platform

If the user provides little context, make reasonable assumptions and label them clearly.

### 2. Define the Delivery Shape

Choose the lightest process that fits:
- idea only: produce scope, MVP, and first tasks
- already coding: produce missing docs, checkpoints, and release controls
- near release: produce testing, deployment, rollback, and ops checklist
- maintenance phase: produce backlog hygiene, incident response, and iteration cadence

### 3. Produce the Minimum Useful Document Set

Default to these seven documents:
- `01-project.md`
- `02-requirements.md`
- `03-architecture.md`
- `04-tasks.md`
- `05-dev-log.md`
- `06-test-checklist.md`
- `07-release-checklist.md`

If the project is tiny, compress to four files by merging architecture into requirements and merging test and release checklists.

Use [templates.md](./references/templates.md) when the user wants document skeletons or filled examples.

### 4. Turn Plans Into Execution Units

Break work into tasks that are small enough to finish in one focused sitting or one day. For each task, include:
- outcome
- dependency
- risk
- definition of done

Prefer task groups such as setup, core flow, supporting flow, quality, deployment, and follow-up.

### 5. Add Control Points

Introduce only a few checkpoints:
- scope check before coding
- architecture check before deep implementation
- self-test after each module
- release checklist before deployment
- post-release review after shipping

When asked for an SOP, express it as a numbered flow with entry criteria and exit criteria per phase.

## Output Rules

When generating a plan or SOP:
- start with a short summary of the project phase
- present the recommended workflow in order
- list required documents and why each exists
- include a lean cadence if ongoing work is involved
- include a short risk list
- include the next three actions

When generating templates:
- use concise Markdown headings
- include placeholders only where the answer depends on project specifics
- keep each template short enough to maintain by hand

When reviewing an existing process:
- identify missing decision points
- identify missing release safeguards
- identify documents that can be deleted or merged

## Default Solo SOP

Use this baseline when the user asks for a general process:

1. Define problem, user, scope, and non-goals.
2. Freeze the MVP for the next iteration.
3. Choose stack and record architecture decisions.
4. Break the MVP into small tasks with done criteria.
5. Build one core flow at a time and update the dev log.
6. Run self-tests on the main path and likely failure cases.
7. Prepare release, environment, migration, and rollback checklist.
8. Ship, monitor, capture issues, and plan the next iteration.

## Tailoring Rules

Scale the process up only when there is real complexity:
- add API specifications when multiple services or clients exist
- add data migration notes when schema changes matter
- add incident SOP when uptime or payments matter
- add security checklist when handling credentials, payments, or personal data

Scale the process down when:
- the project is a prototype
- no external users depend on it
- the release can be recreated quickly

## References

Read [templates.md](./references/templates.md) for the default document outlines and checklist content.

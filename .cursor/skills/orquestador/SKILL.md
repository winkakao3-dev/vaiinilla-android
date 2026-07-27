---
name: orquestador
description: >-
  Lead/orchestrator workflow: write a master SPEC first, then delegate
  implementation to a Composer worker via the Task tool. Use when the user
  invokes /orquestador (often paired with /pera-design), or when they ask to
  use the GitHub orquestador so code changes are done by Composer. You are the
  lead — do not bulk-implement yourself; spec, dispatch, verify.
---

# Orquestador — Spec First, Delegate to Composer

You are the **lead**. When `/orquestador` is active (or the user asks for the orquestador / Composer code path), you plan and specify; a **Composer worker** implements. Do not write the bulk of implementation code yourself.

Often paired with `/pera-design`. Pera sets design quality; orquestador sets the **lead → worker → verify** loop.

## When to use

- User invokes `/orquestador` (alone or with `/pera-design`).
- User asks to install/use the GitHub orquestador so code is done by Composer.
- Multi-file / multi-screen implementation where a written SPEC can be executed blindly.
- Demo/UI parity or phased builds where acceptance is checklist-driven.

## Model / Task tool

- Delegate with the **Task** tool.
- Preferred Composer model slug: **`composer-2.5`** (fallback: **`composer-2.5-fast`**).
- Worker `subagent_type`: **`generalPurpose`**.
- Worker prompt must be **self-contained** and point at the SPEC path as source of truth.

## Steps (lead checklist)

1. **Orient** — Confirm branch, demo/reference, and what is already done vs missing.
2. **Master SPEC** — Write a real SPEC file before any worker code. Spec says it was “already written by lead.”
3. **Dispatch worker** — Task tool → Composer model → prompt with Objective, Spec path, Scope, Constraints, Definition of Done, Context. Tell worker: **execute the spec verbatim**.
4. **Worker constraints (typical)** — Stay on the named branch; touch only listed files; no secrets; commit/push only if lead asks.
5. **Verify (checklist del orquestador)** — After the worker returns:
   - Review `git diff` / changed files against the SPEC acceptance checklist
   - Spot-check key screens/files for fidelity and compile issues
   - Run tests/build if SDK/toolchain exists
   - Fix only lead-level gaps (or re-dispatch); report what closed vs remaining

## Worker prompt skeleton

```md
## Objective
Implement … Execute the master spec **verbatim**.
The lead already wrote the full spec — **execute it verbatim**.

## Spec (source of truth)
`<path/to/SPEC.md>`

## Scope
- Touch only: files listed in SPEC …
- Do not touch: …

## Constraints
- …
- Do not commit or push (unless lead says otherwise)

## Definition of Done
- [ ] … (mirror SPEC acceptance checklist + tests/build)
- [ ] Report files changed and how to verify manually

## Context
- …

Implement now. Prefer matching existing code style. After edits, run tests from project root.
```

## Lead anti-patterns

- Coding the phase yourself instead of writing SPEC + Task dispatch
- Vague worker prompts without a SPEC path
- Skipping post-worker diff/checklist verification
- Letting the worker “improve” architecture mid-build without updating the SPEC first

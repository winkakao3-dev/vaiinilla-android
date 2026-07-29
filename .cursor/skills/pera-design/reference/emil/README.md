# Emil Kowalski — vendored skills

Opinionated design-engineering and animation craft from [emilkowalski/skills](https://github.com/emilkowalski/skills), vendored under Pera so one skill covers both composition and motion finish.

MIT licensed — see `LICENSE`. Upstream pin in `SOURCE.md`.

## Authority

**Pera wins** on brand, first-viewport composition, chrome neutrality, type roles, signatures, anonymity/swap test, and ship-bar verification.

**Emil wins** on animation craft details when Pera is silent: easing choice, frequency gating, spring interruptibility, gesture velocity, review/audit checklists, and the curated UI-library picks.

If both name a number and they disagree, keep Pera's table in `reference/motion.md` for the page budget and cite Emil's value only when reviewing or fixing a specific transition.

## When to load

| Path | Read when |
|------|-----------|
| `emil-design-eng/SKILL.md` | Building or polishing UI interaction/animation detail; general design-eng taste check |
| `review-animations/SKILL.md` + `STANDARDS.md` | Reviewing a motion diff; need exact easing/duration/spring citations |
| `improve-animations/SKILL.md` + `AUDIT.md` + `PLAN-TEMPLATE.md` | Auditing a codebase's motion and writing executable fix plans (read-only on app code) |
| `find-animation-opportunities/SKILL.md` | Asking "what should animate here?" — proposes recipes, does not implement |
| `animation-vocabulary/SKILL.md` | User describes a motion loosely and needs the precise term |
| `apple-design/SKILL.md` | Gesture sheets, springs, momentum, translucent materials, Apple-fluid web motion |
| `pick-ui-library/SKILL.md` | Choosing a library (toasts, cmdk, virtualization, DnD, etc.) from Emil's curated list |

Load only the folder the task needs. Do not dump the whole tree into context.

## Adapter notes (Pera)

These files are upstream content with two Pera-local adaptations:

1. **No greeting gate.** `emil-design-eng` upstream opens with a one-line ready message and waits. Under Pera, skip that — answer the task.
2. **Invocation flags ignored.** Upstream `disable-model-invocation` on review/library skills does not apply inside this skill; the routing table above decides when they load.

Do not re-litigate Pera's composition rules with Emil's examples. Use Emil to make the motion correct once the composition is already Pera.

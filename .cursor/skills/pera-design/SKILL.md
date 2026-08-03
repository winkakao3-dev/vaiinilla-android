---
name: pera-design
description: Opinionated premium UI/UX system — neutral chrome with color from content, three type roles, one scroll-scrubbed signature per page, and real finish on every control. Includes vendored Emil Kowalski motion/design-eng satellites. Use for landing pages, portfolios, SaaS and product UI, redesigns, micro-interactions, scroll effects, animation review/audit, and canvas/WebGL atmosphere.
---

# Pera Design

Interfaces that feel premium, restrained, and alive. Distilled from 8 page extracts, 10 photos and 15 motion clips in `references/`.

Pera fails in exactly two directions. **Unfinished** — bare browser controls, missing states, "polish later". **Anonymous** — competent but forgettable, nothing a visitor would screenshot. Every rule below exists to prevent one or the other. Where no rule applies, use judgement and match the surrounding work.

## The system

1. **Chrome is neutral; content carries color.** Nav, buttons, borders, icons, text stay achromatic or warm-neutral. Hue comes from photography, video, product screenshots — or at most one accent.
2. **First viewport is one composition.** Full-bleed dominant plane; brand + one headline + one support line + one CTA group. Not a grid of widgets, cards, stat strips or floating badges.
3. **Three type roles.** display ≠ body ≠ meta. One face at three weights is not a system.
4. **Every control is finished.** Full state set, tokens, material, tactile feedback, keyboard access, reduced motion. Nothing ships as a browser default — see `reference/craft.md`.
5. **Ideas come from the subject, not the catalog.** Before choosing any layout or effect, inventory the subject's own places, rituals, words, materials and time-of-day; section names and the flagship signature are derived from that list. Never invent facts to be interesting — see `reference/imagination.md`.
6. **Every page has 2–4 signature details**, one of them scroll-scrubbed (plays in both directions), plus the always-on fingerprint layer — see `reference/signature.md`.
7. **Motion is budgeted**: ~3 intentional families per page, compositor props only, decorative motion gated behind `prefers-reduced-motion`.
8. **Reduced-motion and no-JS land on the complete page** — fully inked text, correct theme, legible everything.

Pick one direction per project and stay in it: studio mono, warm editorial, or product narrative (`reference/directions.md`).

## Working method

For anything larger than a single component, **spec before code**: decide in numbers while it is still cheap, then execute. Copy `reference/spec-template.md`, fill it, build it, verify against its own acceptance checklist. A brief with adjectives is not a spec; a spec has hexes, durations, easings, from→to values, and named animations.

Small isolated components don't need the ceremony — they still need the craft.

## Ship bar

The first delivered version is the final one. Before handing off: zero placeholders, real copy; render and look at it (1440×900 and 390×844); tab through every interactive element; scroll top-to-bottom and back up; check the reduced-motion and no-JS end state.

Reading your own code is not verification — the failures that ship are the ones that look correct in the source. Render it, measure it with the probes in `reference/verify.md`, and say plainly which checks you skipped.

When the subject already exists — a brand, a show, a product, someone's work — source the real artwork and the real copy with a browser before inventing any of it (`reference/assets.md`).

## Reference map

Load only what the task needs.

| File | Read when |
|------|-----------|
| `reference/imagination.md` | Starting a project, or the page feels anonymous — the subject inventory and the swap test |
| `reference/directions.md` | Starting a project — direction, composition, brand, shared vocabulary |
| `code/tokens.css` | Always — color, type, space, radius, material tokens for all three directions |
| `reference/craft.md` | Any UI element is involved — per-control recipes, state matrix, viewport and chrome arithmetic |
| `reference/verify.md` | Before every handoff — browser probes, scrubbed-state sampling, a11y-tree audit |
| `reference/assets.md` | The subject already exists — harvesting real art and copy, slicing sprites, rights |
| `reference/signature.md` | Shipping a page — signature catalog, budgets, QA |
| `code/signature.css`, `code/signature.js` | Implementing a signature — working ink-fill, spine, clip reveal, theme flip, scrub fallback |
| `code/verify.js` | Running the ship bar — callable probes, `pera.report()` |
| `reference/motion.md` | Timing, easing, spring values |
| `reference/patterns.md` | Choosing a layout or component pattern |
| `reference/spec-template.md` | Before building a page or multi-section feature |
| `reference/canvasui.md` | Brief asks for liquid / particles / glass / WebGL atmosphere |
| `reference/amicro.md` | Brief asks for fancy buttons, toggles, card decks |
| `reference/antipatterns.md` | Reviewing your own work, or the output feels generic |
| `reference/emil/README.md` | Index for Emil Kowalski motion/design-eng satellites — load only the folder the task needs |
| `reference/emil/emil-design-eng/SKILL.md` | UI polish, interaction detail, design-eng taste while building |
| `reference/emil/review-animations/` | Reviewing a motion diff — load `STANDARDS.md` for exact values |
| `reference/emil/improve-animations/` | Auditing codebase motion into executable plans (read-only on app code) |
| `reference/emil/find-animation-opportunities/SKILL.md` | "What should animate here?" — proposes recipes, does not implement |
| `reference/emil/animation-vocabulary/SKILL.md` | Vague motion description → precise term |
| `reference/emil/apple-design/SKILL.md` | Gesture sheets, springs, momentum, translucent materials |
| `reference/emil/pick-ui-library/SKILL.md` | Curated library pick (toasts, cmdk, DnD, virtualization…) |
| `sources/*.md` | You need the raw evidence behind a rule |

When Emil and Pera disagree: Pera owns composition/brand/chrome; Emil owns animation craft detail. See `reference/emil/README.md`.

Code references are the source of truth over prose: `references/examples/uber-navbar-replica.html` is the finish density every control should meet, and `code/*` is copy-paste ready.

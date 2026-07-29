# Master spec — template and language rules

For a page or multi-section feature, decide everything in numbers before writing code. Rushed micro-decisions made mid-build default to the generic look; decisions made in the spec are cheap to change and force you to have Pera's docs open.

Write it as `SPEC.md` next to the build. It is for you — don't paste it in chat unless asked.

**Scale it to the work:** full landing page 150–400+ lines, single section 40–80, one component 20–40. A single isolated control doesn't need a spec, only the craft.

## Language rules

- **Numbers, not adjectives.** "Soft shadow" → `0 30px 80px rgba(0,0,0,0.5)`. "Snappy" → `150ms ease-out`. "Big headline" → `clamp(44px, 4.5vw, 74px)`, tracking `-0.02em`, line-height `1.11`.
- **Name every animation** (`ink-fill`, `heroExit`, `cellReveal`) with duration, easing, delay, stagger, from→to.
- **Exact quantities.** "43 photos, skip #8", "8 blur layers", "40 bars with this peak array" — counts are decisions.
- **States are specified, not implied**, per the matrix in `reference/craft.md`.
- Assets are named with real URLs or a generation plan. No invented placeholders, no empty `<img>`, no colored rectangles.

> **Wrong:** "The headline fades in elegantly on load."
>
> **Right:** "Headline chars wrapped in spans at runtime (do NOT pre-split in JSX). Per char: opacity 0→1, translateY 24px→0, blur 6px→0, 0.9s cubic-bezier(0.22,1,0.36,1), stagger 30ms, base delay 0.9s after fonts ready. Reduced motion: chars render at final state."

## Skeleton

```md
# MASTER SPEC — <project> (<direction>)

<Identity paragraph: what the page is, for whom, the mood, the direction,
and the one-line quality bar. "The spec below is the source of truth.">

## 0. Subject inventory
The six rows from reference/imagination.md — places, rituals, voice,
material, time, tension — filled from real source material, each one
naming the page decision it drives. Section names come from here.

## 1. Stack & global setup
Framework + version, styling approach, animation library, fonts (names,
weights, and one load method — <link> or package, pick one), routes.

## 2. Assets manifest
Every image, video and icon with its exact URL or how it gets made.

## 3. File structure
Every file to create, one line each, with its job.

## 4. Design tokens
The actual CSS variable block, adapted from code/tokens.css. Hexes, not
"warm neutrals".

## 5. Sections
### 5.1 Hero
Layer hierarchy and z-index, exact sizes/positions/paddings, type roles with
sizes and tracking, every animation named with duration/easing/delay/
stagger/from→to, responsive behavior, its one job.
### 5.2 <Section> — same treatment
### 5.n Signature set
Flagship + supporting + fingerprint layer, with scrub ranges.

## 6. Cross-section rules
Breakpoints, cursor rules, motion budget, reduced-motion policy, scroll
library, theme-flip wiring.

## 7. Footguns
Numbered DO-NOTs specific to this build. "Do NOT give buttons a hover lift —
this design has none." "Load fonts via <link> only, never CSS @import."

## 8. Acceptance checklist
10–20 observable behaviors: "scrolling back up un-fills the manifesto text",
"glass cursor appears only ≥lg and only over cards". Include the Pera ship
bar: hero budget, chrome neutrality, signature budget, reduced-motion end
state, and the one detail that fails the swap test.

Build this as a faithful reproduction of the spec. Do not improve timings,
change tokens, or refactor mid-build.
```

## Execution

The spec is the source of truth; code implements it. A genuinely better idea updates the spec first, then the code — never fork silently. Final QA walks the acceptance checklist item by item, plus the ship bar in `SKILL.md`.

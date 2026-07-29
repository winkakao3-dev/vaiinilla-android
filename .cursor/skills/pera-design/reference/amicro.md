# Amicro — micro-transitions

Local source: `references/amicro/Micro-transitions/` · upstream [Subhan-code/Amicro](https://github.com/Subhan-code/Amicro--Micro-transitions-) · catalog snapshot `references/amicro/catalog.json`.

React 19 + Motion + Tailwind 4 + Lucide: **35 button demos across 13 interaction types** and **15 card spreads/carousels**. The demo ships a code generator, so the components are copy-paste sources rather than a dependency.

```bash
# published CLI
npx amicro@latest add card-cover-flow-mono

# or copy from the repo (always works)
#   src/components/AnimatedButton.tsx + src/data/buttons.tsx   → buttons
#   src/components/cards/*.tsx + src/data/cards.ts             → spreads, carousels
#   src/utils/codeGenerator.ts                                 → ready Motion snippets

# or run the demo
cd references/amicro/Micro-transitions && npm install && npm run dev
```

## Using it under Pera

Amicro is ammunition for the "nice moment" in `reference/craft.md`, not a replacement for finish. Restyle to Pera tokens — the demo's `#1f1f1f` and rainbow Lucide accents go. Prefer `*-mono` card variants. One micro-family per control cluster; it counts toward the motion budget. Under reduced motion, drop the hover choreography and keep the instant end state. Amicro already gates hover with `(hover: hover)` and handles touch — preserve that.

## Button types (13)

| Type | Feel | Pera use |
|------|------|----------|
| `morph` | Icon1 ↔ Icon2 crossfade | **First choice** for toggles: play/pause, copy→check, theme, mic |
| `slide-arrow` | Icon exits, arrow slides in | Primary CTA — continue, download |
| `sparkle` | Secondary icon pops | Star / favorite, accent kept tame |
| `pulse` | Scale pulse | Soft emphasis, low amplitude |
| `rotate` | Icon spins | Settings, reload |
| `shake` | Short horizontal shake | Destructive affordance |
| `ring` | Bell rings | Notification subscribe |
| `color-morph` | Fill/color state change | Like, bookmark — single accent |
| `glare` | Shine sweep | Occasional premium CTA |
| `text-reveal` | Label choreography | Marketing CTA |
| `magnetic` | Eases toward the cursor | Hero CTA only, one per view |
| `expand-ring` | Ring expands | Link or share punctuation |
| `focus-blur` | Siblings blur except the hovered one | Inline link rows |

Demos included: Download for Mac, Star on GitHub, Deploy, Copy Hash, Sponsor, Share, Preview, Settings, Delete, Subscribe, Search, Theme, Microphone, Camera, Volume, Lock, Directory, Visibility, Save Later, Like, Download, Upload, Account, Submit, Edit, Network, Power, Expand, Reload, Favorite, Glare Shine, Text Reveal, Magnetic Field, Expand Ring, Focus Blur Links.

## Cards (15)

**Spreads** — `card-arc-5`, `card-arc-7`, `card-long-arc-5`, `card-linear-spread`, `card-corner-fan`, `card-stamp-arc`, `card-cascade-stagger`, `card-scatter-spread`, `card-wheel-fan`.

**Carousels and stacks** — `card-carousel` (arc 3D + dots), `card-cover-flow`, `card-time-machine` (depth stack + scrubber), plus the `-mono` variants of all three.

CLI id maps 1:1: `npx amicro@latest add <id>`.

## First choice under Pera

Buttons: `morph`, `slide-arrow`, `rotate`, `shake` for destructive, `color-morph` on a single accent, `focus-blur` for link rows. Cards: the `*-mono` carousels, or `card-linear-spread` / `card-arc-5` for quiet galleries — in galleries, case decks and media pickers, not the hero.

## Workflow

Pick one button type or one card pattern for the cluster → copy from `codeGenerator.ts` or the component file → restyle to tokens → wire the full craft states (focus-visible, disabled, reduced motion). Never use Amicro flash to cover for unfinished chrome.

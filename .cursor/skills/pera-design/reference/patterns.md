# Patterns

Layouts and components observed across the reference set. Pick, don't stack.

## Page sections

**Hero (marketing)** — full-bleed media or atmospheric field, large brand wordmark, one headline, one sentence, one CTA group (primary pill + optional ghost). Nothing floating on the media.

**Case-study grid** — edge-to-edge tiles; black or dark gutters read more editorial than gray. Image or video first, then title, one-line description, service tags. Hover may reveal tags or lift softly.

**Work archive list** — columns of Client/Campaign · Category · Year. Text as interface, imagery optional on hover, transitions ~150ms. May honestly declare desktop-only.

**Capability bento** — strict repeated card template (icon, display heading, body, illustration). Grayscale skeleton illustration is fine. Micro-motion loops inside the illustration only; chrome stays still.

**Product feature dual-card** — two mockups side by side with an ambient dim↔sharp pulse alternating focus. Captions stay fully legible at all times.

**Pinned scroll narrative** — long sticky viewport, content swaps on scroll. Product storytelling only, paired with a smooth-scroll library.

**Kinetic type strip** — one idea broken into word spans with staggered reveal. Keep line count readable. The scrubbed version (A2) reads more expensive.

**Signature moment** — one flagship scrubbed detail per page. Recipes: `reference/signature.md`.

## Navigation

| Pattern | When |
|---------|------|
| Minimal text links + one pill CTA | Marketing |
| Segmented pill toggle (2 glyphs) | Portfolio: works ↔ profile |
| Floating dock + merged submenu | Desktop product tools |
| Bottom tab + grown popover | Mobile product |
| Bottom dock with sliding active pill | App chrome |
| Grouped sidebar, selected row lifted | Admin, settings |
| Category tags as nav | Digital garden, archive |

Selection prefers a morphing pill over color. Menu rows snap. Active admin items are a lifted surface, not a loud fill.

## CTAs and controls

Primary = filled pill, secondary = ghost pill. Loading CTA morphs into progress-inside-pill with optional Cancel; success morphs to a check and reverts. When the type system is three-tier, a distinct display-sans may be reserved for CTAs. Every control type follows `reference/craft.md`; button and card micro-motion comes from `reference/amicro.md`.

## Interactive collections

Hover fans, arcs, CoverFlow and Time Machine stacks belong to galleries, case decks and media pickers — not the first viewport. Prefer mono variants.

## Overlays

Dark frosted glass over busy canvases; light opaque matte over blurred color. Letter or document metaphor for personal bios. Redact sensitive numbers with asterisks while keeping structure. Open and close with blur-focus plus scale/fade, focus trap, Esc.

## Lists and data

Quiet row hover, snap highlights inside menus, morphing pill only on top-level tabs. Tabular nums in tables. Custom checkbox craft when rows are selectable. Stagger the first paint when the list is a featured moment.

## Forms

Inputs with idle, hover, focus, error and disabled. Intentional caret and placeholder. Search may morph from icon to field. Labels and helper text are designed, not browser leftovers.

## Proof and trust

"With: [collaborator]" credits, service tags, a named exec quote with a stat callout, a compliance badge row near the footer, a live local-time clock in the header, device chrome around screenshots.

## AI and process UI

Frosted status pills stacked over content ("Building context…"). Reasoning timeline: timestamp, bold micro-heading, full sentence. A floating "Ask AI" affordance that morphs into search. Odometer number-flow for stats.

## Media treatment

Autoplay muted case-study video thumbs. Browser frame with traffic lights, tab and URL pill. Nested mockups (phone inside card, browser inside card). Side-by-side desktop and mobile proof.

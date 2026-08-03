# Anti-patterns — the single list

Everything Pera bans lives here, so no other file has to repeat it. Read this when reviewing your own work or when output starts to feel generic.

## The AI-generic tells

- Purple → indigo gradient themes
- Cream `#F4F1EA` + terracotta + serif as a lazy "premium" combo
- Broadsheet cosplay: hairline rules, zero radius, dense newspaper columns
- Rainbow gradient text fills
- Generic 3D blobs or stock filler as the main visual idea
- Fade-up-on-scroll on every section as the only scroll idea — this is *the* tell
- Inter/Roboto/Arial as the face that defines the brand
- A single sterile flat hex as the page background

## Anonymity

- Sections named Hero / Features / About / Gallery / CTA when the subject has nouns of its own
- A flagship signature picked from the catalog with no fact about the subject behind it
- Dark mode, particles, gradients or 3D used as a substitute for having a specific idea
- Describing the subject in adjectives the subject never uses about itself
- Inventing facts, quotes or lore to make a page more interesting — research harder instead
- Generic ornament added while usable material already sits unused in the asset set
- A page where nothing fails the swap test: every element would fit equally well somewhere else

## Composition

- Hero as a card collage or inset media panel
- Stats strips, schedules, address blocks or promo chips in the first viewport
- Floating badges, stickers or callout boxes on hero media
- Cards for static, non-interactive content; cards inside cards
- Multiple competing display faces in one viewport
- Multi-accent rainbow palettes
- Glassmorphism everywhere; glow stacks; multi-layer shadows; emoji as decoration
- Rounded-full pill *clusters* — one pill CTA is fine, a row of them is clutter
- Apologetic broken mobile instead of an honest desktop-first note

## Layout traps

Every one of these shipped in a real Pera build, read as correct in the source, and was caught by a probe from `verify.md`.

- A full-height first viewport (`100svh`) combined with in-flow sticky chrome — the page is then chrome-height taller than the screen and the headline or CTA falls below the fold. Subtract the chrome, and give the chrome a **constant** height
- Sticky chrome that changes height when it compacts; compact by material and background, never by size, or every viewport-height calculation drifts on scroll
- A composition element left as a sibling of the layout grid instead of a child of it — it lands after the section, offscreen, while the CSS looks right
- Sizing art by percentage of a parent whose height never resolves, which silently collapses it to nothing
- A section that is one column of text with the other half empty. If a column has no content, fill it with art or collapse the layout — dead half-screens read as unfinished, not as breathing room
- Cards clipped by the top and bottom of a pinned viewport, or a pinned section's heading hidden behind the sticky bar
- Scroll-triggered state bound to a wide scroll band, so short neighbouring sections inherit it — bind to the element's own bounds crossing a line
- `loading="lazy"` on images inside a transformed, pinned or horizontally-translated track
- The same label printed twice on one card because a name appears in both the heading and its own field

## Craft

- An interactive card built as `<article tabindex="0">` — if it opens a dialog it is a button, and the accessibility tree is where you find out
- Icon rails and nav items with placeholder accessible names (`Personaje 2`, `Link`, `Button`)
- Bare browser controls pretending to be product UI
- Crafting only the navbar while buttons, lists and dropdowns stay generic
- "Simple X" as an excuse to skip states, materials or press feedback
- "I'll polish in a follow-up" — if it is on screen, it is finished
- Spinner-only loading when the skeleton shape is known
- Sliding pill highlights inside dropdown menus (menus snap)
- Removing focus styles, or letting an effect hide them

## Motion and signature

- Five scroll effects fighting each other — the budget is 2–4, one flagship
- Scroll-jacking: hijacked wheel speed, forced snap on marketing pages
- Half-inked text as the reduced-motion or no-JS end state
- Animated or flickering grain — static pass only
- Parallax on type, or media parallax above ~8%
- Ambient motion fighting moving buttons and labels
- Desktop-hover-only signatures with no mobile equivalent
- Effects that break text selection, anchor links or keyboard scrolling
- Confetti, neon, novelty spam: 3D tilt everywhere, magnetic cursor on all text

## Libraries

- Stacking 2+ Canvas UI WebGL effects on one page
- Rainbow liquid on calm product UI; `glitch`/`vhs` on form chrome
- Canvas UI used as a substitute for unfinished buttons and lists
- Amicro card fans in the hero
- A different Amicro interaction type on every adjacent button; magnetic or glare on all of them
- Rainbow Lucide icon accents on mono chrome
- Copying idiosyncratic fonts or brand IP (SF Pro homage, MEK custom faces) without license or brief

## Spec

- Coding straight from the brief with no spec, on work that warrants one
- Adjective soup ("clean, modern, premium") with no values
- "etc.", "TBD", "something like", unresolved asset references
- A spec covering only the happy path — no states, no responsive, no reduced motion
- Writing the spec after the build to justify it
- A beautiful spec, then improvising in the editor anyway

## Handoff

- Handing back a first draft that was never rendered and looked at
- Reporting a check as passed when only the code was read
- Implying full coverage instead of naming what went unverified
- "Fixing" a blank image seen in a single screenshot without re-shooting and asking the DOM whether it actually loaded

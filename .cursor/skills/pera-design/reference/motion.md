# Motion

Springs for content reveals, fast ease-out for chrome. One ambient family per view, isolated from the chrome. Phase-offset duplicate loops so they never lockstep. Blur plus dim beats a flat scrim. Budget: ~3 intentional families per page.

## Timing table

| Motion | Duration | Easing | Notes |
|--------|----------|--------|-------|
| Tooltip / icon crossfade | 120–200ms | ease-out | Opacity only; retarget instantly |
| Button state morph (load→success) | 200–350ms per phase | ease-in-out | Width animates with the label |
| Popover / submenu from anchor | 250–350ms | ease-out, slight overshoot | Shared radius throughout |
| FAB ↔ panel shell | 300–450ms | ease-out open, ease-in close | Tiny settle bounce on close |
| Menu row hover | 0–80ms | snap | Native feel, no sliding pill |
| Tab label expand | ~200ms | ease | Icon-only ↔ icon+label |
| Background blur focus-in | 200–300ms | ease-out | 0 → 10–20px blur + dim |
| Content spring appear | 800–1200ms | spring, bounce ≈ 0.2 | Framer: damping 85, stiffness 500 |
| Staggered list reveal | 60–150ms stagger | ease-out | +4–8px Y; pair with a live counter |
| Numeral roll | 150–400ms | velocity blur → sharp | Blur scales with the value delta |
| Skeleton breathe | ~2600ms loop | ease-in-out | Opacity 1 → 0.58 → 1 |
| Ambient dim↔sharp pulse | 4–8s half-cycle | sine | Phase-offset across siblings |
| Infinite marquee | constant | linear | Duplicate the dataset for a seamless loop |
| Chrome opacity / color | 150–400ms | ease-out | `--dur-chrome` |
| Theme flip | 600–900ms | ease | `--dur-theme` |

## Spring appear (content)

```text
type: spring, bounce 0.2, damping 85, stiffness 500
from: opacity 0, y 12–24px (y -150 for a dramatic drop-in)
stagger: 80–200ms between siblings
delay: up to 1.2–2s for staged entrances
```

## Shape continuity

Expanding surfaces keep the same radius family — a pill stays a pill; a panel grows from a FAB with interpolating radius. Docks and submenus merge visually: flush corners, no shadow seam.

## Focus overlays

Background `filter: blur(0 → 12–20px)` plus dim; foreground scales 0.9 → 1 (0.8 → 1 for badges) with a short overshoot; reverse both on dismiss.

## Ambient and generative

Allowed inside a bounded decorative region — card header, hero band: ASCII/halftone/noise breathing (6–10s), infinite 3D-tilted marquees (linear only), idle mascot drift ±2–4px, or one Canvas UI overlay (`reference/canvasui.md`). Surrounding buttons, labels and nav stay static and fully crafted while ambient runs.

## Physical metaphors

Delete and destroy only: crumple into a vessel, shred with gravity and fade, ~1–1.5s total. Never block the next interaction.

## Scroll storytelling

Lenis or equivalent for long narrative landings. Pinned sections for sequential product stories, sparingly. `scroll-snap: proximity` for case-study sequences. Under reduced motion, fall back to normal scroll with no pin theatre.

Scroll-linked signature timings live with their recipes in `reference/signature.md` and `code/signature.css`.

## Reduced motion

The global kill-switch is in `code/tokens.css`. In JS and GSAP, prefer the subtler path: skip springs, marquees and ambient loops, keep essential state fades ≤150ms.

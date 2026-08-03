# Craft — finish on every element

Pera is not "minimal and empty". It is restrained chrome with museum-grade finish on **everything**: buttons, lists, dropdowns, inputs, toggles, tabs, modals, toasts, tables, sliders, search, empty states.

Finish density reference (real code, read it): `references/examples/uber-navbar-replica.html`. That is how finished *one* control can be — apply the same care to all of them, not only the nav.

**The test:** if this element sat alone on a black stage, would it feel premium — and still look like Pera? No → add craft. Yes but noisy → strip gimmicks, keep precision.

## Universal layers

| Layer | Habit |
|-------|-------|
| Tokens | Named vars for color, size, radius, easing — no magic numbers |
| Shell | Border, fill, blur, inset highlight, shadow as a system |
| Feedback | Selection and progress via shape, weight or morph — not hue alone in mono chrome |
| Motion | Named easing, transform/opacity, deliberate duration |
| Press | `scale(var(--press-scale))` or equivalent on every pressable |
| Icons | SVG `currentColor`, optically sized — no emoji chrome |
| Type | Tracking, weight shifts, tabular nums, ellipsis where needed |
| A11y | Roles, labels, expanded/current/checked, focus-visible |
| Responsive | Tokens rescale; the feel is rebuilt, not blindly shrunk |
| Reduced motion | Always |

## Viewport and chrome

The first viewport is a composition, which makes it an arithmetic problem before it is a taste problem. Get the arithmetic wrong and the taste never gets seen.

**Give sticky chrome a constant height** and let it compact by material — background, blur, hairline — never by size. Chrome that shrinks on scroll changes every height that was derived from it, so the hero grows or shrinks under the reader.

**Subtract the chrome from the full-height section.** In-flow sticky chrome pushes the section down, so `100svh` overflows by exactly the bar's height. `calc(100svh - var(--chrome-h))` fits; `100svh` plus a `min-height` large enough to matter fails on short laptops first, which is where you are least likely to be looking.

**Structure the hero as rows, not as absolute overlays.** A grid of `1fr auto` — content centered in the first row, staged art in the second — keeps the art on a shared baseline and keeps everything inside the section by construction. Absolute positioning against a section that is taller than the viewport puts art below the fold while the CSS reads as correct.

**Stage figures on a ground line.** Objects sharing a baseline at different scales read as one scene with depth; the same objects centered in equal boxes read as a spec sheet. Depth comes from scale, overlap and framing elements at the edges — not from shadows.

Verify all of it with numbers, not with a glance: `reference/verify.md`.

## Minimum state set

| Control | Idle | Hover | Press | Focus | Disabled | Extra |
|---------|------|-------|-------|-------|----------|-------|
| Button | ✓ | ✓ | ✓ | ✓ | ✓ | loading, success |
| Icon button | ✓ | ✓ | ✓ | ✓ | ✓ | tooltip |
| Nav / tab | ✓ | ✓ | ✓ | ✓ | — | selected morph |
| Dropdown | ✓ | ✓ | ✓ | ✓ | ✓ | open, active option |
| List row | ✓ | ✓ | ✓ | ✓ | ✓ | selected |
| Input | ✓ | ✓ | — | ✓ | ✓ | error, filled |
| Checkbox / switch | ✓ | ✓ | ✓ | ✓ | ✓ | checked, on |
| Modal | — | — | — | trap | — | open, close |
| Toast | — | pause | — | ✓ | — | enter, exit |

## Recipes

Each: the minimum bar, then one deliberate "nice" moment. Timings live in `reference/motion.md`.

**Buttons / CTAs** — pill primary, ghost secondary; weight ≥500; loading morphs width + inner content, success morphs to a check and reverts in ~1.5–2s. Nice: label↔icon crossfade during load, never color alone.

**Icon buttons / toolbars** — hit target ≥44px; inset-aware focus ring. Nice: tooltip swaps in place when hopping to a neighbour, no slide.

**Links in prose** — intentional underline strategy (none in chrome, considered in copy); hover moves toward `--fg` or weight, not default blue. See the drawn underline in `code/signature.css`.

**Nav, docks, tabs** — morphing active pill via transform, press scale, custom SVG, safe-area when fixed, `aria-current`. Inactive dock tabs may collapse to icon-only. Nice: merged shape when a submenu grows from the active tab.

**Dropdowns / selects / menus** — anchored panel with `--radius-panel`, shadow plus hairline; open scales/fades from the anchor; chevron rotates 180°; row hover is a **snap** highlight, never a sliding pill; active item marked by check or weight; `aria-expanded`. Nice: ↑↓ keyboard plus typeahead and a focus trap.

**Popovers / context menus** — same materials as dropdowns; blur-focus the background when modal-ish. Nice: continuous radius merged with the trigger.

**Lists / rows** — consistent row height, optically aligned leading icon, quiet hover via elevation or soft fill, separators by hairline *or* spacing, not both. Nice: staggered enter on first paint.

**Steppers / timelines / reasoning logs** — timestamp + bold micro-heading + full sentence; dashed connector; phase-offset loaders. Nice: live counter (`3/14`).

**Inputs / search** — intentional caret and placeholder; idle, hover, focus, error, disabled. Nice: search morphs icon button → full-width field via shared layout; clear × has its own press.

**Checkboxes / radios / switches** — custom drawn; check scale-pops; thumb slides ease-out; hit area larger than the glyph. Nice: label weight syncs with checked.

**Chips / filters** — pill; selected filled or heavier, idle outlined or muted; removable chips focusable. Nice: morphing background across a segmented set.

**Segmented controls** — shared track, thumb morphs via transform, labels don't jump. Same family as the nav pill.

**Modals / sheets / drawers** — scrim plus blur, fade + slight Y or scale, focus trap, Esc closes. Nice: sheet snaps with spring while the backdrop blur eases in sync.

**Toasts** — auto-dismiss timed, pause on hover. Nice: 1.05 scale overshoot then settle.

**Tables** — tabular numerals, quiet row hover, sorted column marked by weight or caret. Row checkboxes get the same craft as standalone ones.

**Empty / loading / error** — skeleton breathe over spinners when the shape is known; empty = one sentence plus one crafted CTA, no clipart; error stays calm mono with accent only when destructive.

**Scroll / overflow** — optional edge fade masks; custom scrollbar only when quiet and on-brand. Nice: `scroll-snap: proximity` for case-study strips.

**Tooltips** — material matches the system, opacity-only transition, flip placement near edges, instant retarget between neighbours.

## Per-element checklist

- [ ] Tokens for color, size, radius, easing
- [ ] Full state set for its type
- [ ] Material shell, not a flat default
- [ ] Feedback via shape/weight/morph where the chrome is mono
- [ ] Tactile cue if pressable
- [ ] Optical SVG icons
- [ ] Type optics: tracking, weight, ellipsis, tabular
- [ ] Hit targets and tap highlight considered
- [ ] Tokens rescale responsively
- [ ] Keyboard and aria complete
- [ ] One deliberate "nice" moment

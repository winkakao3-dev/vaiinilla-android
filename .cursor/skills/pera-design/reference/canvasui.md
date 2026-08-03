# Canvas UI — WebGL atmosphere

[canvasui.dev](https://canvasui.dev) · registry `https://canvasui.dev/r/registry.json` · local snapshot `references/canvasui/` (full descriptions and all flavors in `catalog.json`).

Open-source HTML-in-canvas + WebGL effects for React, Vue, Svelte and vanilla. Installs via the shadcn CLI, so the code lands in the repo.

```bash
npx shadcn@latest add https://canvasui.dev/r/{id}-{react|vue|svelte|vanilla}.json
```

Default install path is `components/canvasui/` — confirm the CLI output.

## Using it under Pera

One effect per viewport, wrapping a hero or section — never inside controls. It counts as the page's flagship signature and toward the motion budget. Keep tints mono or single-accent (`rainbow: false`), mount only when visible, and fall back to static content under `prefers-reduced-motion`. Some HTML-in-canvas paths need Chrome flags, so the content underneath must stay usable and interactive without the effect.

## First choice

| Id | Use when |
|----|----------|
| `liquid` | Pointer-driven fluid wash over a hero or dark stage |
| `particle-reveal` | Cursor restores crisp UI from dust — portfolio hero |
| `clouds` | Soft fog atmosphere, cursor parts the mist — editorial |
| `glass` | Cursor lens / crystal zoom on a focal element — product spotlight |
| `magnify` | Scanner read of dense content — sparingly, HUD quiet |
| `ripple` | Click ripples on a still pond hero |
| `peel` | Edge peel to a second layer — case study, before/after |
| `droplets` | Subtle rain refraction — weather or mood hero |

## Second choice — stronger personality

`bubble` (metaball glass cursor) · `cloth` (fabric ripple poster) · `grid` (tile ripple) · `bend` (cube-edge scroll fold) · `laser` (content prints behind a beam) · `particle-scroll` (dissolve to sand and rebuild) · `retro-dither` · `asciify`.

## Special brief only

`blaze` (only if the brand is literally heat) · `vhs` / `glitch` (glitch, music, art — never SaaS chrome) · `shatter` (climax moments) · `hex-float` (sci-fi, game) · `dithered-object` / `glass-object` / `particle-object` (three.js GLB/SVG sculpture, one object max).

## Full catalog (24)

`asciify` live ASCII around cursor · `bend` scroll folds over cube edges · `blaze` fire and heat distortion · `bubble` metaball droplet cursor · `cloth` fabric wind and cursor waves · `clouds` procedural fog · `dithered-object` 1-bit dithered GLB · `droplets` rain refracting the page · `glass` glass lens and crystal zoom · `glass-object` liquid-glass GLB/SVG · `glitch` slice/RGB/noise bursts · `grid` 3D tile ripple · `hex-float` beveled hex floor · `laser` beam hides and reveals on scroll · `liquid` pointer WebGL fluid · `magnify` scanner HUD magnifier · `particle-object` particle cloud from GLB/SVG · `particle-reveal` dust to crisp UI · `particle-scroll` sand dissolve on scroll · `peel` edge peel · `retro-dither` dither lens · `ripple` click water ripples · `shatter` glass shards · `vhs` tape playback look.

## Workflow

Confirm the stack (React if unclear) → pick one first-choice id unless the brief names another → install → wire as a section wrapper → tune toward mono and lower intensity → gate reduced motion to render children only → keep the controls on top fully crafted.

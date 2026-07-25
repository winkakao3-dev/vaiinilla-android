# Signature — one-of-one details

A signature detail is a small precise moment only this page has: a headline whose letters ink up as you scroll, a section that flips the page dark, a 2px progress spine, text that inverts when selected. It is what separates "competent" from "screenshotted".

Working code for everything here: `code/signature.css` + `code/signature.js`.

## Budget and rules

- **2–4 per page**: one flagship (usually scrubbed type or a theme flip), 1–2 supporting details, plus the fingerprint layer — which is always on and does not count.
- **Scrubbed beats triggered.** A scroll detail must track scroll position continuously and reverse with the thumb. A one-way fade-in is a reveal, not a signature.
- **Never the same set twice.** A different combination per project, or two Pera pages read as one template.
- Signatures live in grayscale, warm neutral, or the single accent.
- A Canvas UI atmosphere counts as the flagship when used. Don't stack WebGL *and* a theme flip *and* an ink-fill.

## Implementation ladder

1. **Pure CSS scroll-driven animation** (`animation-timeline: view()` / `scroll()`), gated with `@supports`.
2. **JS fallback** writing a single `--p` custom property — `initScrub()` in `code/signature.js`. Styling stays in CSS.
3. **GSAP ScrollTrigger** (`scrub: 0.5–1`, + Lenis) only when the page already ships GSAP for storytelling.

## Catalog

### A. Scroll-scrubbed type (flagship tier)

| Id | What it is | Notes |
|----|-----------|-------|
| A1 **Ink fill** | Manifesto line fills from ~18% ink to full `--fg` in reading order | The canonical Pera flagship. Once per page. Best on 3–8 word display lines |
| A2 **Word scrub** | Per-word opacity 0.2→1 tied to scroll | Reads more expensive than the triggered kinetic strip |
| A3 **Sticky headline exchange** | Heading pinned while supporting lines swap | Swaps are fast chrome fades; the pin is the signature. Desktop-first |
| A4 **Character cascade** | Chars rise 0.4em + blur 6px→0, stagger 18–28ms, once | A reveal — allowed as support, never as the flagship |
| A5 **Underline draw** | In-content links draw their underline | Supporting detail, nearly free |

### B. Page-mood shifts (flagship tier)

| Id | What it is | Notes |
|----|-----------|-------|
| B1 **Theme flip** | Page ground inverts while an "act" crosses mid-viewport | One act per page. Tokens must be theme-aware; sync `theme-color`; test focus rings on both grounds. Warm editorial's best flagship |
| B2 **Scroll spine** | 2px progress line, top edge or side rail | Product narrative's cheapest signature |
| B3 **Hero exit** | Headline drifts up 6–10% + blurs ~4px; media scales 1→1.06 and dims | Depth without parallax cliché |
| B4 **Clip reveal** | Media un-clips `inset(12% round 24px)`→`inset(0)` with scale 1.12→1 | Radius family must match the tokens |
| B5 **Horizontal scrub strip** | Sticky 100vh section translates a row horizontally | Desktop-only; declare it in copy. Never hijack wheel speed — only map progress→translateX |
| B6 **Parallax whisper** | Media offset ≤8% of section height | Media only. Type never parallaxes. Above ~8% reads as 2015 portfolio |

### C. Fingerprint layer (always on)

Most of this is already in `code/tokens.css` and `code/signature.css`.

- Inverted `::selection` — people notice when they select text
- Custom focus ring token, never the browser default
- `<meta name="theme-color">` matching the ground, synced on B1 flips
- Section index numerals (`01 — 05`) in the meta face
- Live local-time clock or coordinates in header/footer (studio presence)
- Oversized footer wordmark cropped at the baseline
- Static grain at 2–4% over hero or page
- Type optics: `text-wrap: balance` on headings, tabular nums on counters, real quotes and dashes, stylistic sets when the face has them
- At most one quiet easter egg: a console line or a `document.title` detail

### D. Numbers

Numeral roll on view (odometer + velocity blur, timing in `reference/motion.md`), once. Live counters (`3/14`) on progressive reveals. Asterisk redaction that preserves layout for sensitive figures.

## Defaults per direction

| Direction | Flagship | Supporting | Fingerprints |
|-----------|----------|-----------|--------------|
| Studio mono | A1 or B5 | B4, A4 | inverted `::selection`, footer wordmark, live clock |
| Warm editorial | B1 | A2, A5 | grain pass, index numerals, drawn underlines |
| Product narrative | B3 + B2 | D numeral roll, B4 | theme-color sync, accent `::selection`, balanced headings |

## QA per signature

- Scrubs forward and backward smoothly; no layout work per frame (check DevTools performance)
- Mobile: works with touch scroll; ranges retuned for short viewports
- Reduced motion and no-JS land on the complete final state — fully inked text, correct theme
- If B1: body contrast ≥4.5:1 on both grounds; chrome and focus rings survive the flip
- Nothing intercepts clicks or text selection — audit `pointer-events` on overlays and grain
- The set differs from the last Pera project

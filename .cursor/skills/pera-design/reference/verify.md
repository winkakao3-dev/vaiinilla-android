# Verify — look at it, then measure it

Read this before handing off any page. Reading your own CSS is not verification: the failures that ship are exactly the ones that look correct in the source. A hero can be perfect in the stylesheet and still push its headline below the fold; a card can be styled beautifully and be invisible to the keyboard. Both are one probe away from being caught.

Two habits carry this whole file. **Render it and look at it.** **Then measure the things eyes are bad at** — a screenshot tells you the composition is wrong, a geometry probe tells you by how many pixels and why.

## Setup

Serve over HTTP, never `file://` — module scripts, canvas pixel reads and fetches all behave differently on the file protocol, so `file://` bugs are fake bugs and `file://` passes are fake passes.

```bash
python3 -m http.server 4321
```

**Pin the viewport before you screenshot anything.** An unpinned browser view reports one size and lays out at another, and you will misread centering, crops and overflow. Force it, and use the same two sizes every time:

```js
// CDP — desktop
Emulation.setDeviceMetricsOverride { width: 1440, height: 900, deviceScaleFactor: 1, mobile: false }
// CDP — mobile
Emulation.setDeviceMetricsOverride { width: 390, height: 844, deviceScaleFactor: 2, mobile: true }
```

Clear the override when you finish so you don't leave the browser in a strange state.

## Measure, don't eyeball

One `Runtime.evaluate` returning numbers settles arguments that screenshots start. The probes below ship as callable code in `code/verify.js` — load it once and run the lot:

```js
fetch('/verify.js').then(r => r.text()).then(t => (0, eval)(t));
pera.report({ hero: '.hero', inFirstViewport: ['h1', '.hero__cta'] })
```

Read them here to know what each one is actually asserting.

**First viewport fits.** Rule 2 of the system says the first viewport is one composition — that means the headline, the support line and the CTA group are all *inside* it, not merely present in the DOM.

```js
(() => {
  const r = s => { const b = document.querySelector(s).getBoundingClientRect();
                    return { top: Math.round(b.top), bottom: Math.round(b.bottom) }; };
  return { vh: innerHeight, hero: r('.hero'), cta: r('.hero__cta'), art: r('.hero__stage') };
})()
```

Every `bottom` must be ≤ `innerHeight`. If the hero's bottom exceeds the viewport by exactly the height of your sticky header, see the chrome trap in `antipatterns.md`.

**No horizontal overflow.** `document.documentElement.scrollWidth <= innerWidth`, at 390 as well as 1440. A single absolutely-positioned decoration is usually the culprit.

**Nothing is clipped inside a pin.** For pinned or sticky sections, a full card must clear the chrome at the top and the viewport at the bottom: `rect.top >= chromeHeight && rect.bottom <= innerHeight`.

**Images actually decoded.** Before you believe a missing asset, ask the DOM:

```js
[...document.querySelectorAll('img')].map(i => ({ src: i.getAttribute('src'), ok: i.complete && i.naturalWidth > 0 }))
```

Judge only what is near the viewport — a lazy image five sections down is pending, not broken. One that is on screen and still undecoded usually means `loading="lazy"` inside a transformed track.

## Screenshots occasionally lie

A screenshot taken during a repaint — especially the retry after one timed out — can come back with images blank that are perfectly loaded. Re-shoot before you "fix" anything, and confirm against `complete && naturalWidth`. Chasing a paint artifact wastes a full edit cycle.

## Smooth-scroll libraries change how you drive the page

With Lenis, GSAP ScrollSmoother or similar installed, wheel events get clamped: a 4000px scroll command may advance the page 200px, so scroll-and-screenshot loops crawl and you conclude the section is empty when you simply never arrived.

Jump directly instead, then force the listeners to run:

```js
const y = document.querySelector('#section').getBoundingClientRect().top + window.scrollY;
window.scrollTo(0, y - 150);
window.dispatchEvent(new Event('scroll'));   // handlers bound to the library may not fire on their own
```

Then assert the state you expect — a class, a transform, a progress value — rather than trusting that it happened.

## Scrubbed and triggered states get sampled, not glanced at

Anything scroll-scrubbed has to play in both directions, and anything that flips a state has to flip back. Sample it as a table: for each section, jump there and record the state, then return to scroll 0 and record again.

```js
[...document.querySelectorAll('.scene')].map(s => {
  window.scrollTo(0, s.getBoundingClientRect().top + window.scrollY - 150);
  window.dispatchEvent(new Event('scroll'));
  return { id: s.id, dark: document.documentElement.classList.contains('theme-dark') };
})
```

The expected result is explicit before you run it: day, night, day, day — and day again at the top. A neighbouring section inheriting the state means the trigger is bound to a scroll band instead of to the element's own bounds.

## The accessibility tree is the fastest craft audit

Take the accessibility snapshot and read the interactive list. It answers, in seconds, the question that styling hides: **is this thing a control, or does it just look like one?**

- Anything that opens, toggles or navigates must appear in that list with a real name. A card that opens a dialog and is absent from it is `<article tabindex="0">` cosplay — make it a button.
- Generic names are failures: `Personaje 2`, `Link`, `Button`, `image`. If the name doesn't say what activating it does, rewrite the label.
- Count the interactive elements against what the design promises. Twelve character cards and one pause control means thirteen entries; if you count one, they aren't reachable.

Then tab through for real: every control takes focus, the ring is visible against both the light and dark grounds, dialogs trap focus and return it to the trigger on close.

## The loop

Render → probe → screenshot → fix → re-probe. Iterate until it looks genuinely good; a first draft handed back unverified is not a delivery. Then do the last pass at 390×844, where pinned tracks, side-by-side columns and absolutely-positioned art fail differently than they do on desktop.

## Report what you did not check

Some things a headless browser cannot tell you: how hover feels under a real cursor, real touch behaviour, font rendering on the target OS, and whether the motion is *pleasant* rather than merely present. State plainly which checks you ran and which you skipped. "Verified at both widths, no overflow, keyboard path complete; did not test with reduced-motion enabled" is a useful handoff. Silence implying full coverage is not.

/* Pera signature runtime — the only JS the signature layer needs.
   Pairs with code/signature.css. Prose catalog: reference/signature.md

   Contract: JS never styles anything. It writes one custom property per
   element (--p, element scroll progress 0→1) and toggles one root class.
   All appearance stays in CSS, so the no-JS end state is already correct. */

const REDUCED = matchMedia("(prefers-reduced-motion: reduce)").matches;
const CSS_SCROLL = CSS.supports("animation-timeline", "view()");

/**
 * Scrub: write --p (0→1) on every [data-scrub] element as it crosses the
 * viewport. No-op when the browser has native scroll-driven animations or the
 * user asked for reduced motion — CSS already handles both cases.
 *
 * data-scrub-start / data-scrub-end (0–1, fractions of viewport height)
 * retune the range; defaults suit a full-height entry.
 */
export function initScrub(root = document) {
  if (CSS_SCROLL || REDUCED) return;
  const els = [...root.querySelectorAll("[data-scrub]")];
  if (!els.length) return;

  let ticking = false;
  const update = () => {
    const vh = innerHeight;
    for (const el of els) {
      const r = el.getBoundingClientRect();
      const start = Number(el.dataset.scrubStart ?? 0.85) * vh;
      const end = Number(el.dataset.scrubEnd ?? 0.35) * vh;
      const p = (start - r.top) / (start - end);
      el.style.setProperty("--p", Math.min(1, Math.max(0, p)).toFixed(4));
    }
    ticking = false;
  };
  const onScroll = () => {
    if (!ticking) { ticking = true; requestAnimationFrame(update); }
  };

  addEventListener("scroll", onScroll, { passive: true });
  addEventListener("resize", onScroll);
  update();
}

/**
 * Theme flip (B1): the page ground inverts while `section` owns the middle of
 * the viewport, and flips back on the way up. Syncs <meta name="theme-color">.
 * One flip act per page.
 */
export function initThemeFlip(section, { rootMargin = "-45% 0% -45% 0%" } = {}) {
  const el = typeof section === "string" ? document.querySelector(section) : section;
  if (!el) return;
  const meta = document.querySelector('meta[name="theme-color"]');

  new IntersectionObserver(([entry]) => {
    document.documentElement.classList.toggle("theme-dark", entry.isIntersecting);
    if (meta) {
      meta.content = getComputedStyle(document.documentElement)
        .getPropertyValue("--bg").trim();
    }
  }, { rootMargin }).observe(el);
}

/**
 * Word split for A1/A2 — wraps words in spans carrying --i so CSS can stagger
 * them. Run before paint; preserves the original text as the no-JS state.
 */
export function splitWords(el) {
  const words = el.textContent.trim().split(/\s+/);
  el.textContent = "";
  words.forEach((w, i) => {
    const span = document.createElement("span");
    span.textContent = w;
    span.style.setProperty("--i", i);
    el.append(span, document.createTextNode(" "));
  });
}

/** Scroll spine fallback for browsers without animation-timeline: scroll(). */
export function initSpine(spine = ".spine") {
  if (CSS.supports("animation-timeline", "scroll()")) return;
  const el = typeof spine === "string" ? document.querySelector(spine) : spine;
  if (!el) return;
  const update = () => {
    const max = document.documentElement.scrollHeight - innerHeight;
    el.style.setProperty("--sp", max > 0 ? (scrollY / max).toFixed(4) : 1);
  };
  addEventListener("scroll", update, { passive: true });
  update();
}

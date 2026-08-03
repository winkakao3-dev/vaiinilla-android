/* pera-design — verification probes
 *
 * Paste into the console, or evaluate the file contents via CDP Runtime.evaluate,
 * then call the probes. Everything returns plain data so it survives returnByValue.
 *
 *   pera.report({ hero: '.hero', inFirstViewport: ['h1', '.hero__cta'] })
 *
 * Prose, context and the traps these catch: reference/verify.md
 */
(() => {
  const round = n => Math.round(n);
  const box = el => {
    const b = el.getBoundingClientRect();
    return { top: round(b.top), bottom: round(b.bottom), left: round(b.left), right: round(b.right),
             width: round(b.width), height: round(b.height) };
  };
  const one = sel => (typeof sel === 'string' ? document.querySelector(sel) : sel);

  /* Every listed element must sit fully inside the first viewport. */
  function firstViewport(selectors = []) {
    const vh = innerHeight;
    const items = selectors.map(sel => {
      const el = one(sel);
      if (!el) return { sel, found: false, pass: false };
      const b = box(el);
      return { sel, found: true, ...b, pass: b.bottom <= vh && b.top >= 0 };
    });
    return { vh, pass: items.every(i => i.pass), items };
  }

  /* Horizontal overflow — run at 1440 and again at 390. */
  function overflow() {
    const docW = document.documentElement.scrollWidth;
    const offenders = docW > innerWidth
      ? [...document.querySelectorAll('body *')]
          .filter(el => el.getBoundingClientRect().right > innerWidth + 1)
          .slice(0, 10)
          .map(el => ({ tag: el.tagName.toLowerCase(), cls: el.className?.toString().slice(0, 60),
                        right: round(el.getBoundingClientRect().right) }))
      : [];
    return { vw: innerWidth, docW, pass: docW <= innerWidth, offenders };
  }

  /* Nothing clipped: each match must clear the chrome above and the fold below.
   * Run this while the section is actually pinned on screen, not from the top of the page. */
  function uncropped(selector, chromeHeight = 0) {
    const items = [...document.querySelectorAll(selector)].map(el => {
      const b = box(el);
      return { ...b, pass: b.top >= chromeHeight && b.bottom <= innerHeight };
    });
    return { chromeHeight, vh: innerHeight, count: items.length,
             pass: items.length > 0 && items.every(i => i.pass), items };
  }

  /* Decoded, not merely present — settles "is that image broken or mid-repaint?"
   * Only images near the viewport are judged; lazy ones further down are reported
   * as pending rather than failing, so this is meaningful at any scroll position. */
  function images(selector = 'img') {
    const near = el => {
      const b = el.getBoundingClientRect();
      return b.bottom > -innerHeight && b.top < innerHeight * 2;
    };
    const all = [...document.querySelectorAll(selector)].map(i => ({
      src: i.getAttribute('src'), near: near(i), decoded: i.complete && i.naturalWidth > 0,
    }));
    const judged = all.filter(i => i.near);
    return { count: all.length, judged: judged.length,
             pending: all.filter(i => !i.near && !i.decoded).length,
             failing: judged.filter(i => !i.decoded),
             pass: judged.every(i => i.decoded) };
  }

  /* Is it a control, or does it only look like one? */
  const GENERIC = /^(link|button|image|img|click here|read more|item|element)\b|^\W*\d+\W*$|\b\d+$/i;
  function controls() {
    const nodes = [...document.querySelectorAll(
      'a[href], button, input, select, textarea, summary, [tabindex]:not([tabindex="-1"]), [role="button"]'
    )];
    const items = nodes.map(el => {
      const name = (el.getAttribute('aria-label') || el.title || el.alt ||
                    el.textContent || '').trim().replace(/\s+/g, ' ').slice(0, 60);
      return { tag: el.tagName.toLowerCase(), name,
               generic: !name || GENERIC.test(name),
               fakeControl: el.tagName === 'ARTICLE' || el.tagName === 'DIV' || el.tagName === 'LI' };
    });
    return { count: items.length,
             unnamed: items.filter(i => i.generic),
             notRealControls: items.filter(i => i.fakeControl),
             pass: items.every(i => !i.generic && !i.fakeControl), items };
  }

  /* Sample a scroll-driven state per section, then again back at the top.
   * read() runs after each jump, e.g. () => document.documentElement.className */
  function sampleStates(sectionSelector, read, offset = -150) {
    const y0 = window.scrollY;
    const jump = y => { window.scrollTo(0, y); window.dispatchEvent(new Event('scroll')); };
    const samples = [...document.querySelectorAll(sectionSelector)].map(s => {
      jump(s.getBoundingClientRect().top + window.scrollY + offset);
      return { id: s.id || null, state: read() };
    });
    jump(0);
    const atTop = read();
    window.scrollTo(0, y0);
    return { samples, atTop };
  }

  function report({ hero, inFirstViewport = [], pinned, chromeHeight = 0 } = {}) {
    const out = {
      viewport: { w: innerWidth, h: innerHeight },
      overflow: overflow(),
      images: images(),
      controls: controls(),
    };
    if (hero || inFirstViewport.length) {
      out.firstViewport = firstViewport([...(hero ? [hero] : []), ...inFirstViewport]);
    }
    if (pinned) out.uncropped = uncropped(pinned, chromeHeight);
    out.pass = Object.values(out).every(v => typeof v !== 'object' || v.pass !== false);
    return out;
  }

  window.pera = { firstViewport, overflow, uncropped, images, controls, sampleStates, report };
  return 'pera verification probes ready — try pera.report()';
})();

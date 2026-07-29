# Assets — go get the real artefact

The reference sites agree on this and it is in `sources/pages.md`: color and life come from the real thing — photography, video, product screenshots, actual artwork — never from generated filler or a 3D blob standing in for an idea. Rule 1 of the system depends on it. Neutral chrome only reads as premium when the content it frames is worth framing.

So when the brief is about something that already exists — a brand, a show, a game, a product, a person's work — the first move is not to invent a visual language. It is to **go and get the real assets with a browser**, along with the real names, the real descriptions, the real numbers. Sourced material beats invented material on every axis that matters: it is accurate, it is already art-directed, and it is the difference between a page about a subject and a page that merely mentions it.

## Harvest

Prefer the official site over a wiki, and a wiki over stock. Work in a real browser session (ego-browser, the IDE browser, any CDP driver) and take the **copy as well as the images** — names, roles, one-line descriptions, credits. Data you transcribe is data you cannot get wrong; data you paraphrase from memory is where fabrication enters.

**Images are frequently not `<img>` tags.** Illustrated sites put their best art in CSS backgrounds and lazy attributes, so an `img` sweep comes back with nothing but the logo and social icons. Sweep computed styles too:

```js
(() => {
  const out = new Set();
  document.querySelectorAll('*').forEach(el => {
    const bg = getComputedStyle(el).backgroundImage;
    if (bg?.includes('url(')) [...bg.matchAll(/url\("?([^")]+)"?\)/g)].forEach(m => out.add(m[1]));
  });
  const html = document.documentElement.outerHTML;
  const lazy = [...html.matchAll(/(?:data-src|data-original|srcset)="([^"]+\.(?:png|jpg|jpeg|webp|avif))"/g)].map(m => m[1]);
  return { backgrounds: [...out], lazy: [...new Set(lazy)] };
})()
```

**Interactive galleries hide their content behind clicks.** A character picker or tabbed showcase usually renders one item at a time. Click each item in turn and read the resulting state — that single pass gives you the whole set of names, roles and descriptions in one structured pull.

**Download, never hotlink.** Vendor the files into the project (`public/assets/…`), so the page keeps working when the source site reorganizes, and so you can slice and optimize freely.

## Slicing a spritesheet

Illustrated sites often ship one wide PNG with every figure in a row. Cut it losslessly rather than positioning backgrounds by hand.

Find the boundaries by alpha — scan columns for any non-transparent pixel, group the runs, then find each run's vertical extent:

```js
(async () => {
  const img = new Image(); img.crossOrigin = 'anonymous'; img.src = SPRITE_URL; await img.decode();
  const c = document.createElement('canvas'); c.width = img.width; c.height = img.height;
  const x = c.getContext('2d'); x.drawImage(img, 0, 0);
  const d = x.getImageData(0, 0, c.width, c.height).data;
  const on = i => { for (let j = 0; j < c.height; j++) if (d[(j * c.width + i) * 4 + 3] > 10) return true; return false; };
  const cols = [...Array(c.width)].map((_, i) => on(i));
  const runs = []; let s = null;
  cols.forEach((v, i) => { if (v && s === null) s = i; if (!v && s !== null) { runs.push([s, i - 1]); s = null; } });
  if (s !== null) runs.push([s, c.width - 1]);
  return runs.filter(([a, b]) => b - a > 40).map(([a, b]) => {
    let top = c.height, bot = 0;
    for (let j = 0; j < c.height; j++) for (let i = a; i <= b; i++)
      if (d[(j * c.width + i) * 4 + 3] > 10) { if (j < top) top = j; if (j > bot) bot = j; break; }
    return { x: a, y: top, w: b - a + 1, h: bot - top + 1 };
  });
})()
```

Then crop with a tool that preserves the alpha channel. On macOS `sips` is already installed and does it without a dependency:

```bash
sips -c $H $W --cropOffset $Y $X sprite.png --out slice.png
```

**Look at every cut before naming it.** The order of the boxes is the order they appear in the sheet, which usually — but not always — matches the site's own ordering. Open two or three and confirm the mapping, then rename to semantic slugs (`hachiware.png`, not `cut_02.png`) and keep any small thumbnails in a parallel set rather than overwriting the large art.

## Using cutouts

Transparent cutouts on a colored ground are the whole trick — they stage as objects in a scene rather than as pictures in boxes. Treat them accordingly.

- **Never distort them.** Set one dimension and let the other be `auto`; `object-fit: contain` with `object-position: bottom center` keeps a figure standing rather than floating.
- **Give them a ground.** Figures aligned to a shared baseline read as a composition; figures centered in equal boxes read as a spec sheet.
- **Size by intent, in explicit units.** Percentage heights against a parent with no resolved height silently collapse. A cutout of 300–460px of source art comfortably carries a 240px display height.
- **Keep the `width`/`height` attributes** from the real file so the layout doesn't shift while they load, and skip `loading="lazy"` for anything inside a transformed or pinned track — see `antipatterns.md`.
- **Depth comes from scale and overlap**, not from drop shadows. A soft shadow at low opacity is a hint that the figure sits on the ground; a hard one turns the artwork into a sticker.

## Rights

Sourced art is someone's work and the page has to say so.

- Carry the original copyright line verbatim, in the footer, in its original script.
- For tribute or fan work, state it plainly and non-commercially — one sentence naming the rights holders.
- Never present sourced art as original, and never strip a signature or watermark.
- If the brief is commercial, this whole route needs licensed assets or commissioned work instead. Say that out loud rather than shipping borrowed art into a paid product.

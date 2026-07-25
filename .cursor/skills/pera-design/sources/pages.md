# Design Reference Analysis — `sources/pages.md`

*Extracted from 8 offline-captured pages in `references/pages/`. All hex/rgb values, font names, and CSS values below are taken directly from `page-extraction.json` (`styles.computed`), `captured-live.css`, `site-index.json`, and rendered DOM text — not guessed.*

---

## 1. Bakers Studio — `bakers.studio`

**Identity:** *"We're the culinary masters of your digital kitchen"* — an independent design & engineering studio (15+ yrs) building brand/product work for crypto, fintech, and AI startups (Giza, Quantinium, Tinyman, Stacker).

**Mood/aesthetic:** Confident, minimal, tech-forward agency portfolio. Content-is-the-color: the UI itself is pure black/white and lets client-project imagery (crypto/fintech branding) supply all color.

**Typography:**
- Suisse Int'l family, self-hosted via Framer: **Suisse Intl Medium**, **Suisse Intl Book** (weight 450), **Suisse Intl Regular**, **Suisse Int'l Semibold** (weight 600) — a Swiss-grotesk workhorse stack.
- Fallback chain includes full **Inter** unicode-range split (Cyrillic/Greek/Vietnamese subsets), suggesting locale-safe fallback engineering (Framer default).
- No serif anywhere. All-sans, weight does the hierarchy work rather than size contrast alone.

**Color palette:** Effectively monochrome UI — only `rgb(0,0,0)`, `rgb(255,255,255)`, and a stray `rgb(0,0,238)` (unstyled browser default link blue — residual, not intentional brand color) plus `rgba(0,0,0,0.2)` for overlays/shadows. Color arrives entirely through project screenshots/video.

**Layout & composition:** Portfolio grid of case-study cards (image + title + one-line client description + service tags: "Website design," "Branding," "Full-stack design & development"). A **live local clock in the header** ("IST 21:03:17") signals global/always-on studio presence. Tag cloud of verticals served (Finance, Crypto, Blockchain, AI, Education, SaaS, Art, Fashion) doubles as a capabilities list and a piece of graphic texture.

**Components/patterns:** Card-based case studies with hover-reveal service tags; footer CTA block repeats the "digital kitchen" copy line for memorability; embedded **Spline 3D scene** (`<spline-viewer>` web component) as an interactive hero object — a strong "premium/technical" signal.

**Motion clues:** Framer's appear-animation system: spring transitions (`type:"spring", bounce:0.2, duration:1`) on scroll-into-view, opacity 0→1 + slight y-offset. `#__framer-editorbar-label` fade uses `opacity 0.4s ease-out`. Responsive breakpoint-driven variant swapping (4 breakpoints: 1600/1200/810px).

**Copy for skill:** the "chrome is monochrome, imagery carries color" discipline; live-timezone clock as a trust/credibility micro-detail; tag-cloud-as-capability-list; spring-based appear animation curve (bounce 0.2, ~1s).
**Idiosyncratic (skip):** Spline 3D embed is a heavy, site-specific flex — don't default to 3D unless brief calls for it.
**Confidence:** High for palette/type (directly from computed styles + captured CSS); medium on motion timing values (framer internal JSON, representative not exhaustive).

---

## 2. Bridge — `bridge.surf`

**Identity:** *"Bridge Intent and Done."* — an "everything agent" AI product (personal AI that operates apps/desktop/browser). Next.js + Tailwind v4 + shadcn/ui architecture.

**Mood/aesthetic:** Clean SaaS-AI landing page: light theme, one confident blue accent, heavy scroll-driven storytelling (pinned/sticky sections), consumer-friendly (not enterprise-cold).

**Typography:** **Inter** only (self-hosted via `next/font`, variable `inter_9de777d8-module`). No secondary display face — hierarchy is built from weight/size/tracking utility classes (Tailwind `text-[20px] font-medium leading-[1.2]` etc.), not custom type.

**Color palette:** Tailwind v4 in `lab()`/`oklab()` color space (unusual — modern wide-gamut CSS color).
- Brand: `--brand-primary` / `--brand-accent` = **`#006fe6`** (electric blue), `--brand-primary-foreground` = `#fff`.
- Neutrals: `rgb(251,251,250)` near-white bg, `lab(90.952% 0 -0.0000119209)` hairline border, `lab(34.924% 0 0)`/`lab(48.496% 0 0)` mid-grays, `rgb(0,0,0)` text.
- Full shadcn-style token set present but unused live: `--card`, `--sidebar-primary-foreground`, `--popover`, `--ring` — indicates a shadcn/ui component library under the hood even though the shipped page is custom-styled.

**Layout & composition:** Full-bleed sections, several **pinned/sticky scroll sections** (`height: 500dvh` scroll-linked "section-five-six" combo — content changes as user scrolls through one long pinned viewport). Cross-surface diagram (Browser / Phone / Desktop) as a hero visual metaphor. Integration icons row: Slack, Discord, WhatsApp, Telegram, iMessage.

**Components/patterns:** Nav = logo + Blog/Pricing + "Join Waitlist" CTA (repeated twice, header + footer). Rotating "task chip" list (30+ example prompts like *"Summarize my notes," "Clean up my messy desktop"*) as social-proof/breadth demonstration. FAQ accordion. Timeline strip: *Perceptron/1957 → Deepblue/1997 → Transformer/2017 → ChatGPT/2022 → Personal Agentic AI/Today* — a "history of AI" motif used as trust-building narrative device.

**Motion clues:** `lenis` class on `<html>` = **Lenis smooth-scroll library** in use. CSS: `transition-opacity duration-200`, `backdrop-filter: blur(1px)` mask-fade at section edges, `transform: scaleY(0)`→`1` reveal on decorative gradient blocks, skeleton "breathe" keyframe (`opacity 1→0.58`, 2600ms infinite ease-in-out) for loading placeholders, animated cursor-path keyframe (translate3d looping demo of the product "clicking" through a UI).

**Copy for skill:** single confident accent blue + neutral gray scale (no decorative color); rotating example-prompt chips for breadth; pinned/scroll-narrative section pattern; Lenis-smooth-scroll baseline; skeleton "breathing" opacity loader.
**Idiosyncratic (skip):** the specific AI-timeline motif and 500dvh pinned scroll height are content-specific, not a generic layout to copy verbatim.
**Confidence:** High (Tailwind CSS vars + DOM text extracted directly); medium on exact spring/duration constants (minified JS, inferred approximate).

---

## 3. creatoroly.com

**Identity:** Motion designer / video editor portfolio ("@creatoroly, motion that feels smooth"). Framer site.

**Mood/aesthetic:** Apple-coded restraint — "sharp typography, generous whitespace... every element earns its place" (their own words about a client project, but it's clearly their design philosophy too). Confident, minimal, video-led.

**Typography:** **SF Pro Display** family self-hosted: Bold (700), Semibold (600), Medium (500), Regular (400) — an Apple-system-font homage reinforcing the "Apple-coded" positioning. Secondary: **Fragment Mono** (monospace, Google Fonts) used for small tag/meta labels ("[ CLICK TO EXPAND ]", client names) — mono-for-metadata is a deliberate "technical/credits" register shift from the display sans.

**Color palette:** Grayscale-only UI: `rgb(0,0,0)`, `rgb(255,255,255)`, `rgb(13,13,13)`, `rgb(15,15,15)`, `rgb(184,184,184)`, `rgb(197,197,197)`, `rgb(230,230,230)`, `rgb(247,247,247)`, `rgba(13,13,13,0.25)`, plus stray unstyled link-blue `rgb(0,0,238)`. Again: **all color comes from embedded video reels**, not UI chrome.

**Layout & composition:** Case-study cards with autoplay video previews ("Pool Concept," "Moonshot x Bone," "ether.fi x Bone," "iOS 27 Mini Concept"), each with a client-voice micro-essay ("Crypto videos tend to shout... ether.fi wanted the opposite") — copywriting-as-differentiator. Kinetic-type section: "Motion / design / do / not / need / a / reason / to / exist. / They / just / need / to / look / clean." broken into single-word spans (likely staggered scroll-reveal). Client logo wall (WHOP, MOONSHOT, ETHER.FI, TRADEZELLA, JIGSAW). "Assets & Lab" downloadable resource grid (Apple PF Pack, project files) — portfolio-as-utility.

**Components/patterns:** `[ CLICK TO EXPAND ]` micro-CTA in mono caps on every card; nav = Projects / Products / Get in Touch; footer signs off in lowercase voice: *"oly is the most addicted person to clean animations in the world."*

**Motion clues:** Framer spring appear-animations (`bounce:0.2, damping:85, stiffness:500`, staggered delays 1.2–1.5s across elements — a deliberate cascade/stagger), `transition: opacity 0.4s ease-out` for UI chrome.

**Copy for skill:** mono-for-metadata / sans-for-display type pairing; word-by-word kinetic-type reveal section; grayscale chrome + color-via-video-content discipline; lowercase, personal-voice microcopy in CTAs/footer.
**Idiosyncratic (skip):** SF Pro Display self-hosting only makes sense if explicitly doing an Apple homage — don't default to it.
**Confidence:** High.

---

## 4. Depth Stack Archive — `depth-stack-archive.vercel.app` (studio.seven — Work Archive)

**Identity:** "Selected work from studio.seven" — a pure work-archive/index page (28 campaign entries: Nike, Coca-Cola, Apple, Rolex, Tesla, Chanel, NASA, etc.), each tagged with category + year.

**Mood/aesthetic:** Radically restrained, editorial, list-first. Explicitly **desktop-only**: *"Mobile support is coming soon. Please view on desktop."* — a confident constraint rather than an oversight.

**Typography:** **Helvetica Neue** (system) only — no custom webfont loaded at all. Monospace fallback stack present (`ui-monospace, SFMono-Regular, Menlo...`) for any code/label text.

**Color palette:** Strict black/white/gray: `#000`, `#fff`, `rgb(240,240,240)` (hover/row bg), `rgba(0,0,0,0)` transparent. Tailwind v4 tokens: `--color-black:#000`, `--color-white:#fff`, `--ease-out: cubic-bezier(0,0,.2,1)`, `--default-transition-duration:.15s`.

**Layout & composition:** Single scrolling list/table: `Client + Campaign Name` / `Category, Medium` / `Year`, nav = Work / Studio / Services / Contact. No hero imagery captured in this snapshot — text-as-interface, likely with imagery revealed on hover (typical of Awwwards-style archive sites) though not present in the static capture.

**Motion clues:** Tailwind default easing tokens only (`ease-out`, `.15s` default transition) — implies short, snappy hover states (150ms) rather than showy animation; no keyframes captured.

**Copy for skill:** the "constrain to desktop, own it in copy" pattern; category+year metadata tagging convention for case-study lists; ultra-fast 150ms micro-transitions as the *only* motion in a minimal archive.
**Idiosyncratic (skip):** the specific 28-brand list is placeholder/demo content, not a real client roster — treat as a generic "archive index" template, not literal copy.
**Confidence:** Medium — this capture is thin (no CSS/JS deeply inspected beyond Tailwind tokens; hover-image-reveal behavior is inferred from genre convention, not directly observed).

---

## 5. Bao To — `www.baothiento.com/timer`

**Identity:** Personal portfolio micro-tool: a **Pomodoro-style focus timer** widget ("Timer - Bao To").

**Mood/aesthetic:** Warm, minimal, personal — a single-purpose utility styled like a designer's side-project, not a marketing page.

**Typography:** **PP Mori** (Regular + SemiBold, self-hosted `.otf`) — a popular indie/portfolio grotesk — plus system `ui-sans-serif` fallback.

**Color palette:** Warm cream/brown, not cold gray: bg `rgb(245,241,236)`, text in `lab(15.1099 1.63209 4.23907)` (a warm near-black brown) at varying opacity (0.15/0.2/0.22/0.25/0.4/0.45) for layered UI states, `#fff`/`#000` extremes, `rgba(42,37,32,0.7)` for secondary text.

**Layout & composition:** Extremely sparse single-screen widget: big numeral countdown (`25` / `00`), labeled mode ("focus"), three text-button controls (`start`, `reset`, `frog`), and an `AUTO ○` toggle. "frog" is a distinctive naming choice (Brian Tracy's "eat the frog" productivity technique) — personality injected into utilitarian UI.

**Motion clues:** Not directly observable from this thin capture (no captured-live.css); Tailwind-based (Next.js) so likely relies on utility transitions; the live page failed to hydrate in headless fetch, suggesting client-heavy interactivity (state machine for timer, `AUTO` mode toggle).

**Copy for skill:** warm off-white + brown-black (not pure gray) as a "personal/human" palette alternative to studio-monochrome; giving a playful, personality-driven name to a secondary action (rename "skip"/"break" → "frog"); huge single numeral as the entire visual hierarchy for a focused utility screen.
**Idiosyncratic (skip):** "frog" naming is a personal brand joke — don't copy literally, but the *technique* (personality in microcopy) is transferable.
**Confidence:** Low-medium — smallest, thinnest capture of the eight; full CSS not present, so most of the visual system is unverified beyond font/color tokens.

---

## 6. MEK.txt — `www.mek.gallery`

**Identity:** Personal site of Michael Alexander ("MEK.txt," b.1990), Indonesian visual artist/typographer/designer/developer. Sections: ABOUT, PIXEL, DESIGN, DEV, FONTS.

**Mood/aesthetic:** Retro-computing / early-internet nostalgia, deliberately worn-in: *"Appreciating and adopting beauty of older software & hardware systems and aesthetics."* Type-obsessed digital-garden portfolio.

**Typography — the standout feature of this set:** an entire **custom type foundry** self-hosted via Framer:
- **MEK Sans Regular** / **MEK Sans Italic** — primary custom grotesk.
- **MEKZANTINE Regular** — a display/blackletter-style face for headers.
- **MEKMODE Text** / **MEKMODE Dings** (dingbat/icon glyph font) / **MEK Mono**.
- Plus Google Fonts **Instrument Serif** (incl. italic) for editorial accents and **Redressed** (handwriting script) for informal annotations.
- Deliberately keeps unstyled default blue hyperlink color in places (`rgb(0,0,238)`) — an intentional retro-web signifier, not an oversight, consistent with the stated old-software aesthetic.

**Color palette:** Warm cream/tan neutral base — `rgb(238,233,220)` bg, `rgb(217,210,193)` tan, `rgb(204,200,192)`/`rgb(156,149,135)`/`rgb(99,96,89)` taupe-gray text scale, `rgb(46,46,46)` near-black — punched with **one vivid accent**: `rgb(255,64,1)` (**#ff4001**, hot orange-red). Plus the deliberate legacy link-blue.

**Layout & composition:** Tag-based nav (ABOUT/PIXEL/DESIGN/DEV/FONTS) functioning as content-type filters. Timestamped changelog/log entries ("Jul 2026 / DEV / 1.0," "Aug 2022 / PIXEL / Landscape Explorations") — a devlog/journal structure rather than a portfolio grid. Modal announcement overlay ("Introducing 1.0: Moodboard & Movement") with explicit close-X.

**Components/patterns:** Log-entry list (date + category tag + title + optional description) doubling as an activity feed; "HOW TO SUPPORT" + "WORK WITH ME" utility links in footer; custom form (`MEK Sans Regular`, 18px, 12px padding, 40px height inputs) with focus border color token.

**Motion clues:** Framer appear animations with **negative initial Y offset** (`y:-150`) and long delay (`delay:2s`) — content drops in *after* a pause, unlike other Framer sites here that fire immediately; skeleton `opacity 1→0.58` breathing loader (2600ms) shared with creatoroly (same Framer component library).

**Copy for skill:** category-tag-as-nav pattern; devlog/timestamped-entry structure as an alternative to static "About" pages; "one hot accent on warm neutrals" palette formula; intentional custom type family naming/branding (a whole "font family" as a design flex) — even without literal custom fonts, the *idea* of a signature display+mono+script trio is portable.
**Idiosyncratic (skip):** the specific custom glyph fonts (MEKZANTINE, MEKMODE Dings) are personal IP — don't reuse assets, only the pattern of "pair a display serif + a script accent + a systematic sans."
**Confidence:** High for palette/type (rich captured-live.css); medium for exact animation timing generalization.

---

## 7. Over-Stimulated — `www.over-stimulated.com`

**Identity:** *"A design engineering studio building products and websites for AI and future-tech companies. Teams bring us in when taste, feel, and care matter."*

**Mood/aesthetic:** Self-aware, ironic-maximalist framing ("Over-Stimulated") paired with an actually calm, taste-forward visual execution — the copy plays with the tension ("the internet is `.over_engineered()` / `.over_complicated()`... this is `{ .over_stimulated() }`" — code-syntax-as-poetry).

**Typography:** **Inter** (full self-hosted variable weight range 100–900) as the workhorse UI font, plus a **custom variable display font** (`__myFont_696fb3`, CSS-module-scoped — likely a serif given `Times` fallback) for headline/editorial moments — classic "grotesk body + serif display" pairing. (Note: `Antartica` variable font and `Times` also appear but are loaded by the third-party **Ballpark** user-testing widget, not the studio's own brand type — don't attribute it to their identity.)

**Color palette:** Warm off-white/blush background `#fdf9f8` (declared `theme-color`), near-black text `rgb(31,31,31)` / `#1f1f1f88` (translucent gray token named `--os-gray`), pure white `#fff`, `rgb(253,249,248)`. A restrained, low-contrast, "paper" palette — no loud brand color despite the "over-stimulated" name (intentional irony).

**Layout & composition:** Fixed 12-column header nav (`lg:grid-cols-12`) with hamburger on mobile; case-study index (1X, NollaMD, Impulse Space, Ballpark OS Tools, Neural Sphere, HCP, Solomon, JKANE, PERMANENT©) each credited "With: [collaborator]" — attribution-as-credibility pattern. Grid tokens: `--columnGap:20px`, `--gutter:32px`. Uses `scroll-snap` (`proximity`) — snap-scrolling case-study sections. `number-flow` library present (odometer/rolling-digit number animation, likely for stat counters).

**Components/patterns:** Persistent fixed header with globe/language icon; "About OS" self-description block; sub-labelled project types ("Moment" appended to several entries — e.g., "Neural sphere - Moment," "Robot vision - Moment" — signaling short-form/interaction-study pieces distinct from full case studies).

**Motion clues:** `scroll-snap-strictness: proximity`; `number-flow` CSS vars (`--_number-flow-d`, `-dx`, `-d-opacity`) for animated digit counters; `--overlay-wipe-pos: -100%` suggests a wipe-transition overlay (likely page/section transition curtain effect).

**Copy for skill:** code-syntax-as-copywriting device (`.over_engineered()`); "With: [collaborator]" credit line on every project; serif-display + grotesk-body pairing; snap-scroll case-study sections; animated digit/counter (number-flow) for stats; sub-typing project entries (e.g. "— Moment") to differentiate depth/scope of work shown.
**Idiosyncratic (skip):** the third-party Ballpark widget font/UI is not part of their design system — exclude from any "look" reference.
**Confidence:** High for palette/grid tokens; medium for the exact custom serif's letterforms (module-hashed, filename not resolvable from this capture).

---

## 8. Rulebase — `www.rulebase.co`

**Identity:** *"The revenue workforce for financial services"* — enterprise AI-agent B2B SaaS (fintech ops: KYC/KYB, reactivation, retention, QA).

**Mood/aesthetic:** Warm-serious enterprise fintech: not cold corporate-blue SaaS, but a cream/paper editorial palette with a red/orange accent pair — trustworthy but not sterile. Product screenshots (dashboards, chat threads, timelines) do heavy lifting to prove real functionality.

**Typography — the most systematic token set of the eight:**
- **Inter** — UI/body (`--font-body: "Inter", "Inter Fallback", system-ui, sans-serif`).
- **ABC Diatype** (self-hosted `.ttf`) — secondary grotesk.
- **STK Bureau Serif** (self-hosted `.ttf`, `Book` weight) — editorial serif for headlines (`--font-bureau: "STK Bureau Serif", Georgia, "Times New Roman", serif`).
- **Hedvig Letters Sans** — a distinctive *display sans reserved specifically for CTAs* (`--font-cta`), separate from body Inter — a three-tier type system (serif headline / sans body / display-sans CTA) that's unusually disciplined for a B2B site.
- Full numeric type-scale tokens: `--text-2xl:60px`, `--text-lg:40px`, `--text-md:36px`, `--text-sm:28px`, `--text-body:14px`; tracking tokens `--tracking-2xl:-1.2px`, `--tracking-body:-.09px`, `--tracking-tag:1px`; leading tokens `--leading-xl:56px`, `--leading-md:41px`.

**Color palette (fully named tokens — rare in this set):**
- `--bg` / `--color-bg`: **`#efedeb`** (warm putty/greige)
- `--cream`: **`#f7f7f4`**
- `--color-dark`: **`#1a1716`** (warm near-black)
- `--red`: **`#cc3542`** (alert/urgency accent)
- `--orange`: **`#d85210`** (secondary warm accent, also appears as `color(display-p3 0.394 0.157 0.06)` — wide-gamut P3 variant for richer screens)
- `--card-bg`: `#e8e5e3`, `--tile-bg`: `#e4e1df` — subtle warm-gray surface elevation steps.
- Full 4/8-based spacing scale: `--space-4:16px` → `--space-16:64px`; `--section-x:100px` (generous horizontal section padding).

**Layout & composition:** Dense, screenshot-driven feature grid — each capability (Reactivation agent, AutoQA, Coaching, Complaint intake) shown as a mini realistic product-UI card rather than an icon+text block — "show the product, don't illustrate it" philosophy. Testimonial cards with named exec quotes + stat callouts (`95% reduction`, `20hrs saved`). Compliance badge row (SOC 2, GDPR, PCI DSS) near footer for trust signaling. Live chat/transcript UI recreated as a design element (not just a screenshot — styled bubbles, timestamps).

**Components/patterns:** Nav = Product (mega-menu with agent sub-items) / Media / Careers / Contact / "Get Started" CTA; embedded Cal.com scheduling widget (`.cal-embed`); MCP server URL snippet displayed as a copy-pasteable code block (developer-credibility signal for an AI product).

**Motion clues:** Minimal captured CSS (`captured-live.css` only 257 bytes — essentially just the Cal.com embed reset), meaning **most motion is applied via Tailwind/Framer-Motion at runtime**, not visible in this static capture. PostHog session-recording + surveys scripts loaded (analytics-heavy stack, unrelated to visual design).

**Copy for skill:** the three-tier type system (serif headline / grotesk body / distinct display-sans reserved only for CTA buttons) is the single most valuable, reusable typographic idea in this set; the fully-named warm-neutral + red/orange accent token system; "recreate the real product UI as the marketing visual" instead of abstract icons; compliance-badge trust row; stat-callout + named-exec testimonial card.
**Idiosyncratic (skip):** the specific fintech-ops product screenshots/copy are domain-specific — the *system* (token architecture, type hierarchy) is what to copy, not the content.
**Confidence:** Highest of all eight — extremely rich, explicit design-token JSON was captured directly (named CSS variables, not inferred).

---

# CROSS-SITE SYNTHESIS

### 1. Chrome is neutral; color comes from content or is spent on exactly one accent
Five of eight sites (Bakers Studio, creatoroly, Depth Stack Archive, Bridge, Over-Stimulated) run **near-grayscale UI chrome** — black/white/gray only in `styles.computed.colors` — and let photography, video, or product screenshots carry all saturation. The two sites that *do* use color (MEK.gallery's `#ff4001` orange-red on cream; Rulebase's `#cc3542` red / `#d85210` orange on `#efedeb` putty) each commit to **one accent hue against warm neutrals**, never a multi-color brand palette. **Principle for the skill: default to a near-monochrome base + at most one accent color; never invent a 3+ color brand palette unless the brief explicitly calls for it.**

### 2. Type hierarchy comes from a *system of named tokens*, not ad-hoc sizing
Rulebase and Bridge (and to a lesser extent bakers/creatoroly via Framer's internal breakpoint variants) all expose systematic scales: `--text-2xl/lg/md/sm/body`, `--tracking-*`, `--leading-*`, `--space-4…16`. **Principle: build type/spacing as a token scale (4 or 8px base unit), not one-off pixel values.**

### 3. Multi-tier type pairing is the norm, not single-font minimalism
Every site with a real design system pairs **at least two, often three, type roles**: a workhorse grotesk for body/UI (Inter appears in 5/8 sites), a distinct display face for headlines (serif at Rulebase/Over-Stimulated, custom grotesk at MEK/creatoroly), and frequently a **third mono/script face reserved for metadata, tags, or CTAs** (Fragment Mono at creatoroly, Hedvig Letters Sans for CTAs at Rulebase, Redressed script at MEK). **Principle: assign type roles deliberately — body ≠ display ≠ label/CTA — rather than one font at multiple weights.**

### 4. Motion is spring-based and appear-on-scroll, almost never linear/ease
Wherever animation internals were observable (Framer sites: bakers-studio, creatoroly, MEK.gallery), the transition type is **spring physics** (`type:"spring", bounce:0.2, damping:85, stiffness:500`) triggered on scroll-into-view with staggered delays, not CSS `ease`/`linear`. Where CSS transitions are used directly, they're short and utilitarian (`opacity 0.4s ease-out` for UI chrome, `.15s` for Tailwind defaults) — **reserve springs for content reveals, keep UI-state transitions fast and simple (150–400ms ease-out).**

### 5. "Breathing" skeleton/loading opacity pulses recur
Both creatoroly and MEK.gallery ship an identical shared component: `opacity: 1 → 0.58`, ~2600ms, `ease-in-out`, infinite — a subtle loading/skeleton "breathe." **Worth adopting as the default loading-state animation** instead of a spinner.

### 6. Smooth-scroll libraries + pinned/sticky narrative sections for product sites
Bridge.surf uses **Lenis** smooth-scroll plus a `500dvh` pinned section where content changes as the user scrolls through one sticky viewport — a pattern typical of the current AI-product-landing-page genre. Over-Stimulated uses CSS `scroll-snap` for case-study sections. **Anti-pattern to avoid: default native scroll with no easing on long narrative/product pages** — these references consistently add scroll smoothing or pinning for anything trying to tell a sequential story.

### 7. Corner-shape / squircle awareness
Multiple Framer-built sites (bakers-studio, creatoroly) carry the CSS custom property `--one-if-corner-shape-supported: "1"` and MEK.gallery exposes `--framer-input-corner-shape` — evidence of the **CSS `corner-shape` (squircle) feature** being wired in as a progressive enhancement. **Principle: prefer superellipse/squircle corners over plain `border-radius` circles when the target browsers support it — this is an active "premium UI" signal in 2026 tooling, not a legacy habit.**

### 8. Real product/portfolio content *is* the hero — decoration is minimal
Rulebase recreates actual dashboard/chat UI as marketing visuals rather than abstract illustrations; Bakers Studio/creatoroly/Depth-Stack-Archive lead with real case-study thumbnails/video, not stock imagery or 3D abstraction (Bakers' one Spline 3D embed is the exception, used sparingly as a hero accent, not throughout). **Anti-pattern: generic 3D blob illustrations or stock photography — these references consistently prefer showing the real artifact (product screenshot, video reel, campaign still).**

### 9. Attribution and credibility micro-copy is everywhere
"With: [collaborator]" (Over-Stimulated), service-tag lists on every card (Bakers Studio, Depth-Stack-Archive), named exec testimonials with stat callouts (Rulebase), compliance badge rows (Rulebase), live local-time display (Bakers Studio). **Principle: bake trust/credibility signals into the component (a card, a header) rather than isolating them to a single "About" or "Trust" section.**

### 10. Desktop-first constraints are stated confidently, not apologized for
Depth Stack Archive: *"Mobile support is coming soon. Please view on desktop."* — presented as a stylistic choice, plainly worded, no hedging. **Principle for the skill: if a layout genuinely needs desktop width (dense archive tables, pinned scroll storytelling), say so plainly rather than degrading gracefully into a broken mobile compromise.**

### 11. Retro/idiosyncratic details are intentional signals, not sloppiness
MEK.gallery's unstyled default blue link color is a deliberate nod to old-web aesthetics matching its stated "appreciation of older software" ethos — a reminder that **when auditing any reference, an apparently "unstyled" element may be a considered choice tied to the site's stated concept**, and should be flagged as idiosyncratic-to-that-brand rather than copied as a general best practice.

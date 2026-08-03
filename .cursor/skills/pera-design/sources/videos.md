# `sources/videos.md`

> Analysis of 15 design-reference video loops (1fps frame extraction), covering ~184 frames. Timings below are estimated as `frame N ≈ second N` unless the clip clearly loops faster/slower than 1 frame/sec (noted per-video). All videos appear to be short (2–30s) looping UI/motion demos, likely captured from Twitter/X, Dribbble, or product marketing sites.

---

## 1. `CCbi_2q59vrXWG19` (2 frames)

**Context:** macOS-style note-taking app card (looks like "Bear" app — has the logo/wordmark "Evening"), toolbar in top-right corner with icon buttons (edit, duplicate, more "…", sidebar toggle).

**Chronological motion:**
- Frame 1 (t≈0s): Cursor hovers the "…" (more) button, a dark tooltip "More…" is fully visible above it, opaque.
- Frame 2 (t≈1s): The same tooltip is captured mid-*crossfade* with a second tooltip label ("...ting window" ghosted behind it) — evidence of a **tooltip swap/crossfade** as the cursor moves between two adjacent icon buttons.

**UI/design notes:**
- Soft neutral off-white toolbar pill floating over a vivid blue/teal gradient background (macOS Big Sur–style wallpaper).
- Tooltip: dark near-black pill, white text, small corner radius, drop shadow, positioned above the icon with a small arrow/pointer.
- Two-frame sample is too short to see full easing, but the double-exposure ghosting in frame 2 implies the tooltip **crossfades in place rather than sliding** when hopping between adjacent triggers.

**Motion recipe (extracted):**
- Tooltip crossfade: opacity 0→1 over ~150–200ms, no movement, retriggers instantly on new hover target (overlapping fade, not sequential).

**What to copy:** Tooltip behavior for icon-toolbar clusters — instant retarget + fast opacity crossfade instead of tooltip "sliding" between targets.

---

## 2. `8674g6CBFaebKcVx` (3 frames)

**Context:** A promo/referral card — "Get Extra Credits" bonus banner promoting a mobile app download, with checklist ("How it works") and a CTA button.

**Chronological motion (t=0,1,2s):**
- Frame 1: Cursor sits mid-card; CTA button "Download App" is **green**.
- Frame 2: Cursor moved to the right edge; mascot sticker in top-right (green blob character) has shifted slightly and the halftone dot texture behind it changed density — a subtle **idle drift/parallax** of the decorative sticker.
- Frame 3: CTA button has swapped from green → **black**, cursor now sits to the left of the card. Sticker continues to idle-animate.

**UI/design notes:**
- Card: white rounded card (large radius ~20px) on light gray-green background; header zone has a mint-green gradient panel with rounded corners, "Exclusive Bonus" pill badge (green text on pale green pill), bold headline, subtext.
- Decorative elements: a small yellow diamond/sparkle and a small yellow square floating above/inside the header — likely looping float/rotate.
- Mascot sticker (a green blob face with sunglasses) sits layered on a white sticker card, slightly rotated, peeking from top-right corner — has a halftone-dot shadow/texture that appears to animate (dissolve/shift), suggesting a **noise-dither reveal** or idle jitter.
- CTA button **color-morphs between brand green and black** — likely a state loop (default vs. hover/pressed) cycling automatically for the demo reel, with a smooth background-color tween (~200–300ms).

**Motion recipe:**
- Button color tween: `background-color` cross-fade green (#2FBD6F-ish) ↔ black, ~200ms ease, looping to show both states.
- Floating sticker: small continuous idle motion (±2–4px translate / slight rotate), noise-dot mask animating underneath for a "sparkle/shimmer" feel.

**What to copy:** Idle "alive" mascot/sticker treatment (halftone shimmer + micro rotation) for empty states or hero cards; two-tone button state demonstration loop.

---

## 3. `C0pQnQgiNyz3Glxb` (6 frames, read all)

**Context:** AI "thinking" / loading state overlay on top of a full-bleed classical painting background (Baroque fresco). Three stacked status pills: "Building context..", "Setting things up..", "Creating layout..".

**Chronological motion (t=0→5s):**
- Frame 1: All 3 pills visible, each with a small animated icon on the left (dot-grid / spinner glyph) that differs per frame — grid dots at different fill states (like a loading glyph cycling through frames, e.g. macOS "•• / ⣾" spinner or a 3x3 dot matrix filling).
- Frames 2–6: Background painting stays completely static (no parallax/zoom) — only the small glyph icons and pill backgrounds change. The pill backgrounds subtly shift in opacity (glass/blur intensity varies slightly frame to frame), consistent with a **skeleton shimmer / breathing glassmorphism** effect rather than actual content change.
- The dot-matrix icon (left of each label) visibly **cycles through a 3×3 grid loading animation**: sparse dots (frame1) → filled grid (frame4) → sparse again — a looping "building" glyph, like a fill-in/drain animation on a grid of 9 dots, likely on a ~1–2s loop per icon, each pill's icon phase-offset from the others (staggered).

**UI/design notes:**
- Frosted glass pills: dark semi-transparent (rgba black ~50-60%), backdrop-blur, white text, generous rounded-full shape, subtle inner highlight/border.
- Vertically stacked, centered over image, with consistent ~16-20px gap.
- This is a **generic multi-step loading/progress indicator** pattern — "Building context.. / Setting things up.. / Creating layout.." — cycling text-free (labels stay fixed) while icon shows progress/activity per row.

**Motion recipe:**
- Grid-dot loader: 3×3 (or similar) dot matrix, dots fade/fill in a wave or random order, ~800ms–1.2s loop, easing out, staggered per pill by ~150-300ms so all three don't animate in sync (organic feel).
- Glass pill: backdrop-blur(12–20px), bg black/50%, radius full, subtle shimmer via opacity 0.9↔1 breathing.

**What to copy:** Multi-line "AI is working" status list with independent looping progress glyphs per line, staggered — great for onboarding/AI generation loading states.

---

## 4. `J1tkpQpyuCuEQegI` (5 frames, read all)

**Context:** POS/receipt printer simulation UI — a café order receipt ("Blue Tokai Coffee") going from "Ready" → printing → fully printed with QR code.

**Chronological motion (t=0→4s):**
- Frame 1 (t0): Compact card, header "Receipt #4471 · Ready", centered icon + "Order ready" + CTA "Print receipt" button (black pill), helper text below.
- Frame 2 (t1): Card has **grown taller** and content swapped entirely — header now shows "printing 3/14" (a progress counter), and the body is replaced by a **receipt-paper texture** (jagged/torn top edge, monospace receipt text: store name, address, timestamp). Footer says "Printing...".
- Frame 3 (t2, printing 11/14): Card has grown further, revealing more receipt lines (order items, subtotal, service charge, tax) appearing progressively — this is a **line-by-line reveal simulating dot-matrix printing**, height growing to accommodate.
- Frame 4 (t3): Card fully "printed" — header CTA has changed to "⟲ Tear off" pill (top-right), full receipt now visible including total, "Thank you", QR code, and payment handle. Bottom jagged edge (torn paper) now appears at the *bottom* too, framing the whole receipt like a physical slip.
- Frame 5 (t4): Same fully-printed state held (end of loop / pause before reset).

**UI/design notes:**
- Two nested surfaces: outer light-gray rounded card (device chrome) + inner white "paper" with jagged/zigzag top+bottom edges mimicking torn receipt paper (implemented via a repeating triangle/zigzag mask or SVG clip-path).
- Typography: monospace for receipt content (authentic POS feel), mixed-weight sans for header/labels.
- The **height of the card animates (auto-height growth)** as content is progressively revealed — this is essentially a "growing receipt" reveal, could be done via `max-height`/`height` transition or a clip-path/mask sliding down to reveal pre-laid-out content underneath.
- Progress is communicated via a live counter text "printing N/14" that increments — paired with the visual growth.
- Final state swaps the action button contextually: Print → Tear off (button label + icon change entirely, not just text).

**Motion recipe:**
- Card height/content reveal: incremental reveal keyed to a counter (14 steps), each step ~200-400ms apart (fast, since 14 lines happen within ~2-3 frames/seconds) — feels like a **staggered line-by-line fade/slide-up reveal**, each line: opacity 0→1 + translateY(4px→0), 14 lines staggered ~60-100ms apart total ~1.5s.
- Torn-edge mask: static jagged clip-path/zigzag border applied to top of "paper" element, and mirrored at bottom once complete — could double as a reveal wipe mask.
- Button morph: label+icon fully swap (cross-fade) rather than sliding, tied to state change (ready → printing → done).

**What to copy:** "Physical receipt" torn-paper card motif with progressive line-by-line content reveal + counter; contextual button relabeling on state completion — great for order confirmations, checkout flows, or export/print demos.

---

## 5. `NdzGrWjo-A_vdJYW` (5 frames, read all)

**Context:** Audiobook app (dark theme) on iPhone mockup — bottom-tab navigation with a **popover/context menu that expands upward from the tab bar**, similar to iOS "More" menu or a mini-player expansion.

**Chronological motion (t=0→4s):**
- Frame 1 (t0): Base state — dark home screen, grid of book covers, bottom tab bar visible with Home/Search/Profile icons.
- Frame 2 (t1): A **popover card has appeared above the tab bar**, containing a "Continue Listening" mini-player row (cover art, title, play button, progress bar) + a menu list (Library, Downloads, Bookmarks, Sleep timer). The tab bar underneath has also changed — the "Home" tab is now labeled with text (expanded from icon-only to icon+label), and other icons dim/blur behind the popover.
- Frame 3 (t2): Full **context switch** — different popover now showing a search interface (search bar, category chips Fiction/Self-help/Business/History, "Recent" search results with book thumbnails). Background book grid is dimmed/blurred more heavily, and the tab bar shows "Search" expanded with label instead of "Home".
- Frame 4 (t3): Same search popover, but now even more blurred/dimmed (background nearly illegible) — suggests a **progressive blur increase** while the popover is "settling"/focused, or a transition frame between two popover states (motion blur artifact of a fast tab switch).
- Frame 5 (t4): Back to frame-2-equivalent state (Continue Listening popover), confirming this is a **looping cycle between 2+ popovers** tied to tab selection.

**UI/design notes:**
- Popover: rounded-rect dark/light (adapts — frame2 popover is dark-gray, frame3 is dark-blue-black), appears to grow from the tab bar upward, anchored to the selected tab, with a **connected visual (rounded merge)** between the active tab pill and the popover panel above it — like the tab and popover are one continuous morphing shape (similar to a "speech bubble" grown from the tab).
- Active tab pill: background lightens/pill highlight + label text appears (from icon-only → icon+text) — an **expanding pill tab** akin to the b9wj_bvz_AWoGfMM dock (see #12) but on mobile.
- Heavy background dimming + blur while any popover is open — a modal-style focus effect.
- Note the strong blur/dim gradient as a *transition state*, implying an easing curve on the blur amount (0px → 12–20px) synced with popover translateY/scale-in.

**Motion recipe:**
- Popover open: originates from tab-bar item, scale/translateY from ~0.9/+20px → 1/0, ~250-350ms ease-out, background blur 0→~16px & dim overlay opacity 0→0.5 in parallel.
- Tab label reveal: icon shifts left slightly, text label fades/slides in from behind icon (width auto-animates), synced with pill background highlight fade-in.
- Switching between popovers (tab to tab): likely a fast cross-fade/morph rather than closing then reopening — shape "flows" from one tab's popover to the next.

**What to copy:** Mobile bottom-tab "grown popover" pattern — anchor a context panel to the active tab with a continuous connected shape, animate label reveal + background blur/dim together.

---

## 6. `aKdNBnalLLDZ9I4E` (7 frames, read all)

**Context:** A 2×2 grid of **four independent micro-interaction demos** (quadrant format, like a "component showcase" reel): (top-left) sticky note deletion → trash can; (top-right) "Share post" pill button → loading/sharing → success; (bottom-left) voice-note recorder scrubber with delete/play controls; (bottom-right) sticky note deletion → paper shredder.

**Chronological motion (t=0→6s), per quadrant:**

*Top-left (note → trash):*
- t0: Empty "New note…" card with trash icon (top-right, red).
- t1-t3: Text typed in ("Sample note!" → "sample note"), cursor blinking.
- t4: Trash icon clicked (cursor becomes a "grab" hand over it), note fades/disappears.
- t5: A white cup (physical 3D object, like a paper cup) appears mid-air above a trash can silhouette — the note has been represented as a **crumpled paper ball dropping into a cup/trash**, replacing the flat UI metaphor with a playful 3D physical object.
- t6: Cup shown settled at bottom (object landed), then reset to empty note card (loop restarts, t7 duplicate of t1).

*Top-right (Share pill):*
- t0-t2: "Share post" pill button → click → morphs into a two-part pill "🟢[gradient loading bar] Sharing | Cancel" (the loading state fills a green gradient bar left-to-right inside the button while a separate "Cancel" pill sits beside it).
- t3-t4: Loading bar completes, whole button **morphs into a green pill "✓ Shared"**, with small confetti/particle dots (faint dust flecks around it — decorative sparkle burst).
- t5-t6: Success pill fades out, resets back to "Share post" default pill (loop).

*Bottom-left (voice note):*
- t0-t6: A horizontal pill-shaped voice message scrubber (waveform, timer "0:00" → "0:06", delete/play-pause/close icon buttons) — the **waveform bars fill in with orange/warm color progressively left-to-right** as if actively recording (0:00 → 0:01 → 0:03 → 0:04 → 0:06), i.e., a live recording indicator. At t6, the "X" (close) is clicked with a cursor, and by t7 it resets to a "Play Voice Note" pill button (collapsed/simplified state).

*Bottom-right (note → shredder):*
- t0-t3: Same note card as top-left but independently phased — types "sample note", trash icon hover/click.
- t4: A rose-gold desktop **shredder device** (skeuomorphic 3D render) appears/fades in below the note.
- t5-t6: Paper shreds visibly falling out of the shredder in thin curled strips with tiny embedded text fragments — a **destruction/shred particle animation**, strips waving like confetti streamers, then fading out, resetting to empty note card.

**UI/design notes:**
- Extremely clean, minimal off-white/cream background across all 4 quadrants — a very "Linear/Notion-esque" component demo aesthetic.
- Heavy use of **skeuomorphic 3D rendered objects** (paper cup, shredder) intermixed with flat UI (cards, pills) — a hybrid style where destructive actions get literal physical metaphors instead of just fading out.
- Custom hand-drawn/cursor icons shown explicitly (grab hand, pointer) to narrate the interaction for the viewer.
- Pill buttons throughout: fully rounded, white bg, soft shadow, icon+label, consistent sizing — a component-library-style consistency across all 4 demos.

**Motion recipe:**
- Delete-to-object morph: UI element (note) fades out while simultaneously a themed 3D object animates in (physics-y drop/bounce), ~1-2s total, likely spring easing (bounce on landing).
- Pill loading→success morph: button width animates as internal state changes (label+icon swap, not just color), loading bar fill duration ~1-1.5s, success state auto-reverts after ~1.5-2s.
- Waveform live-record fill: bars illuminate progressively left-to-right synced to a running timer, orange/amber accent on active bars vs. gray on pending.
- Shred/confetti particles: multiple thin strips with slight rotation + gravity fall + fade-out over ~1s, staggered start per strip.

**What to copy:** Playful "physical metaphor" delete confirmations (shred/crumple-into-trash) instead of plain fade; the share-button state-machine morph (idle → loading-bar-inside-pill → success-pill w/ sparkle → revert) is an excellent, highly reusable component pattern for CTAs.

---

## 7. `ST358aeEsQjs4IOs` (9 frames, read all)

**Context:** Two side-by-side finance/trading "daily brief" cards (dark productivity tool, e.g. a trading assistant), each with a large **generative ASCII-art illustration** at the top (feather/quill on the left card "Orchestrate Your Execution"; a leaf/plant on the right card "Validate Your Thesis") that continuously morphs, above a task checklist + schedule dots + action buttons ("Copy Daily Plan", "Share to Team" / "Copy Link", "Share to X").

**Chronological motion (t=0→8s):**
- The ASCII art area is the **only animated region**; the card chrome (title, checklist rows, buttons) is 100% static across all 9 frames.
- Left card (feather): cycles between a **very faint, low-contrast render** (frame1, mostly whitespace, sparse gray/colored characters) and a **fully bloomed, high-contrast render** (frame6, dense black ASCII strokes forming a crisp feather silhouette), then fading back down (frame7-9 return toward sparse). This reads as a **breathing/pulsing opacity+density cycle**, roughly a 6-8 second sine-wave loop: sparse → dense → sparse.
- Right card (leaf/plant): follows the same breathing pattern but **phase-offset** from the left card — when left is sparse, right is already dense (frames 1-2 show right card already fairly bold while left is faint), suggesting the two cards are deliberately **not synchronized** (offset loops for visual interest/liveliness).
- Micro-detail: individual ASCII characters appear to shift/jitter (different characters/glyphs at the same pixel position across frames) even at similar overall density — implying a **per-character noise/glyph-cycling** effect layered on top of the density breathing (like a matrix-rain or ASCII generative-art shader).

**UI/design notes:**
- Dark near-black text on white card, monospace ASCII rendering an image via density-mapped characters (classic "ASCII art from image" technique, likely canvas/WebGL driven).
- Card below: clean SaaS card UI — checkbox rows with checkmark icon, time range text (04:00–09:00), colored dot progress/schedule indicator (mixed gray/green/red/orange dots — a horizontal "week status" strip), pill priority badge (green "PRIORITY"), and bottom toolbar with size toggles (1D/1S/1W... "10 1S 1W") plus dark CTA buttons.
- Whole composition sits on a light gray page background — a marketing screenshot pair, likely meant to show two related product features side by side.

**Motion recipe:**
- ASCII "breathing" generative art: density/opacity of rendered glyphs oscillates on a slow (~6-10s) loop; character glyphs themselves re-randomize each tick (fast, sub-second) for a "living" static-like texture; two instances phase-offset from each other.
- Everything else: zero motion (static card, no hover states shown) — motion is purely decorative/ambient, isolated to the hero illustration.

**What to copy:** Ambient generative ASCII/particle art as a card header — a great low-cost "premium feel" motif for hero cards or empty states; keep the rest of the card completely static so the ambient motion doesn't compete with UI legibility. Consider phase-offsetting duplicate ambient animations across a grid so they don't feel mechanically synced.

---

## 8. `IfKtexbhuTTx_0E7` (14 frames, sampled 8: 1,2,4,6,8,10,12,14)

**Context:** A macOS-style "Quick Note" / scratchpad app window (traffic-light dots, "Todo ⌄" dropdown title, hexagon logo top-right, footer shortcuts "⌘⌥⏎ Add and open" / "Add to note ⌘⏎").

**Chronological motion (t=0→13s):**
- Frames 1-2 (t0-1): Empty note window, cursor idle, chevron next to "Todo" pointing down.
- Frame 4 (t3): Clicking the "Todo ⌄" title triggers a **dropdown menu opening downward** from the title bar — menu items "Random thoughts / Links / Todo ✓ / Ideas" appear in a floating rounded-rect panel with a checkmark next to the current selection ("Todo"). Chevron flips to point up. The note body area is now covered/blurred by the panel's translucency.
- Frames 6-12 (t5-11): The dropdown stays open while the **cursor hovers different rows sequentially** (Todo → Ideas → back to Todo → Ideas again), and each hovered row gets a **light gray highlight pill** that follows the cursor row-by-row (classic macOS menu highlight behavior) — this highlight moves instantly (no visible slide, discrete row snap) between frames.
- Frame 14 (t13): Dropdown has closed, back to empty note state, chevron back to pointing down — full loop reset.

**UI/design notes:**
- Whole window has a **frosted/translucent mint-blue tint** (macOS vibrancy material) — background page bleeds through subtly.
- Dropdown panel: white/light frosted rounded rectangle, drop shadow, positioned directly below the title text (anchored menu, not centered), items in medium-weight sans, checkmark for active selection, generous row padding (~48px row height) for touch-friendly desktop menu.
- This mimics **native macOS menu semantics** exactly — a fully custom-drawn recreation of the OS-level select/dropdown affordance.

**Motion recipe:**
- Dropdown open: panel likely scales/fades in from the anchor point (~150-200ms), chevron rotates 180° in sync.
- Row highlight: discrete background-color snap per row on hover (no interpolation) — true to native menu feel, NOT a smoothly sliding highlight like a segmented control.
- Close: reverse of open, fast fade/scale-down.

**What to copy:** Native-feeling anchored dropdown with instant (non-sliding) row highlight — appropriate for menu/select components where "snappy OS-native" feel is desired over "smooth animated" feel. Chevron flip 180° tied to open state.

---

## 9. `boDfEvhGAyUpMS4w` (14 frames, sampled even: 2,4,6,8,10,12,14 + frame1)

**Context:** A design-studio/agency marketing pair of cards — left card: white bg, orange halftone-dithered generative ink-blot animation above serif headline "Engineering Systems That Think Clearly."; right card: solid blue bg, large sans headline "Turning Complexity into Usable Systems.", with a column of small light-blue dithered/pixelated abstract shapes on the right edge (icons rendered in a matching halftone style, cycling).

**Chronological motion (t=0→13s):**
- Left card: an **orange halftone dot-matrix blob** continuously morphs — starts as two separate small cloud-like dot clusters (frame1), grows into a large connected diagonal ink-stroke/swoosh shape (frames 2, 4), becomes a dense diagonal double-stroke (frame6), then a solid color-fill wedge from one edge (frame8), then thinner diagonal stroke (frame10), then a butterfly/wing-like double-blob shape (frame12), then a diagonal wedge again (frame14) — this is a **fluid noise-driven halftone simulation** (like a Perlin-noise-driven dithered fluid/ink sim, reminiscent of a shader-based generative background), looping smoothly with continuously evolving organic shapes, never repeating the exact same frame in this sample.
- Right card: 5 small vertically-stacked halftone "icon" blobs (in a lighter blue-on-blue dither) that independently morph in density/shape frame to frame — appears to be a **secondary, smaller-scale version of the same noise-dither system**, perhaps representing UI icons dissolving in/out of legibility.
- Both cards' text and layout are 100% static — only the generative dot-pattern areas move.

**UI/design notes:**
- Bold two-tone brand palette: orange-on-white (left) vs. white/light-blue-on-blue (right) — a strong editorial/agency identity system.
- Serif display type (left) contrasted with grotesque sans (right) — a "systems thinking" duality conveyed via typography pairing too.
- Halftone dot rendering: classic dot-screen/ordered-dither look (not simple blur) — dots vary in size/density to represent tone, giving a print/newspaper aesthetic married with digital fluid motion.
- Small logo mark (three horizontal wavy lines, like a "flow" icon) bottom-left of each card, unanimated.

**Motion recipe:**
- Generative halftone-fluid animation: continuous noise-field driven dot density map, evolving shape over ~10-15s+ non-repeating loop (looks like real-time fluid/curl-noise simulation, not a canned loop); dot size/opacity encodes "ink density" instead of simple pixel color.
- No hard cuts — organic continuous morphing, medium speed (shapes fully change character every ~2-4s).

**What to copy:** Halftone/dithered generative-fluid backgrounds as an art-directed hero motif for agency/tech-forward brand cards — pairs well with bold serif+sans type contrast; small-scale repeated version of the same generative system can double as an ambient icon/pattern strip elsewhere on the page for visual cohesion.

---

## 10. `AyjZ4KBSdW4rd_pH` (15 frames, sampled odd: 1,3,5,7,9,11,13,15)

**Context:** A generic content page (skeleton/wireframe loading state: gray bars for title, avatar row, and 6 paragraph lines) with a **floating bottom toolbar** ("✨ Ask AI" pill / arrow icon / crop-frame icon / two-tone squares icon / black circular search button) that morphs into a full-width search bar, plus overlay feedback reactions (👍 thumbs-up, text "OK, but could be better...").

**Chronological motion (t=0→14s):**
- Frame1 (t0): Skeleton page fully loaded (gray placeholder bars), toolbar pill fully visible/sharp at bottom, cursor near it.
- Frame3 (t2): **Heavy blur** across the entire page (skeleton bars and toolbar all significantly blurred/soft), and a **search input pill has appeared centered mid-page** ("🔍 Search...") with sharp focus — meaning focus/attention has shifted to a floating search overlay while background blurs out heavily (Spotlight-style modal takeover), toolbar shrunk to a blurred dark dot at bottom.
- Frame5 (t4): Blur reduces back to none, skeleton content sharp again, toolbar restored, but now a text overlay "OK, but could be better..." fades in centered over the content (feedback annotation), toolbar still present at bottom unchanged.
- Frame7 (t6): Content sharp, feedback text is gone, a faint green horizontal glow/highlight bar appears mid-content (row 3) — perhaps highlighting a specific line as if AI is "selecting" content there.
- Frame9 (t8): Search pill reappears (this time anchored at the bottom, replacing the toolbar entirely, wider, with an "X" clear button on the right) — content sharp behind it.
- Frame11 (t10): Same bottom search bar, cursor hovers the "X" clear icon (now a hand cursor), content sharp.
- Frame13 (t12): Heavy blur again, but now a **green circular thumbs-up icon** is centered (scaled up, ~90px), feedback micro-interaction — like a "liked/approved" confirmation animation, content blurred behind it.
- Frame15 (t14): Back to frame1-equivalent — clean skeleton state, toolbar pill restored, loop resets.

**UI/design notes:**
- This looks like a demo of an **in-page AI assistant toolbar** (like a Grammarly/Notion AI-style floating action bar) that can expand into a search/ask input, and can trigger content-level feedback overlays (approve/disapprove reactions) with heavy background blur to draw focus to the ephemeral feedback element.
- Background dimming via **blur** (not dark overlay) — content remains visible but softened, keeping context while focusing attention on the foreground element — a very "premium/Apple-like" focus technique.
- Toolbar pill: white rounded-full container holding a labeled "Ask AI" segment (with a small emoji/icon) + 3 icon-only buttons + 1 solid black circular button (search) — mixed pill/circle composition, consistent radius family.
- Feedback element: a big colored circular icon (green filled circle w/ white icon) scale-emphasized against blur — a satisfying "toast"/confirmation motif.

**Motion recipe:**
- Focus-blur transition: background blur radius animates 0px → ~10-16px in ~200-300ms ease, paired with a foreground element (search bar / feedback icon) scaling in from ~0.8→1 with slight overshoot.
- Toolbar → search-bar morph: pill width expands/reflows (likely a shared-layout/FLIP animation) from compact icon cluster to full-width input, icons fading out as text input fades in.
- Feedback icon: circular badge pops in with scale+fade (spring, slight overshoot ~1.05 then settle), holds ~1-1.5s, fades out with blur removal simultaneously.

**What to copy:** "Blur-to-focus" pattern for ephemeral feedback/confirmation overlays (blur background instead of dimming — feels lighter/more premium); floating AI-assistant toolbar that morphs into a search input; use of a big, bold circular icon badge for success/approval toasts.

---

## 11. `b9wj_bvz_AWoGfMM` (17 frames, sampled odd: 1,3,5,7,9,11,13,15,17)

**Context:** A floating pill-shaped **dock/navigation bar** (Dashboard / Profile / Resources / Settings) that expands **dropdown submenus upward** when each tab is active — very similar in spirit to `NdzGrWjo-A_vdJYW` but on desktop web and more clearly demonstrating the "connected shape" mechanic.

**Chronological motion (t=0→16s):**
- Frame1 (t0): Collapsed dock — all 4 items shown as icon+label in a single light-gray pill, no submenu, floating centered on white bg.
- Frame3 (t2): "Dashboard" tab is now the **active pill (white bg, distinct from gray dock bg)** with icon+label, and a submenu panel has grown *above* it showing Collection/Token/Rewards/Swap (crypto-wallet nav items) — cursor hovering "Rewards" which has its own subtle row highlight. Other 3 dock tabs have **collapsed to icon-only** (labels hidden) to save space — a clear **expand-active/collapse-inactive tab label** pattern.
- Frame5 (t4): Cursor moved to hover "Token" row — highlight follows to that row instantly.
- Frame7 (t6): Cursor near "Swap" row bottom — dock and submenu unchanged otherwise, submenu appears anchored with rounded bottom corners merging visually into the active dock tab below (shared rounded-rect illusion, corner radii matching up).
- Frame9 (t8): **Switched to "Resources" tab as active** — submenu content fully swapped to Learn/Help center/Blog/Careers (crossfade of submenu content, not a slide), Dashboard/Profile now icon-only, Settings icon-only, cursor hovering Settings icon (transitioning next).
- Frame11 (t10): **Settings** now active, submenu swapped again to Profile/Linked wallets/Email/Customize, cursor mid-row on "Email".
- Frame13 (t12): Cursor still on Settings submenu, hovering near "Customize"/bottom.
- Frame15 (t14): Switched back to **Resources** active (submenu = Learn/Help center/Blog/Careers again), cursor on "Careers" row highlighted.
- Frame17 (t16): Resources still active, submenu content fully visible+static, cursor drifted below the whole component (idle/reset moment) — dock pill appears to have a very faint duplicate/ghost artifact above it in this frame (possible fade-out remnant of a previous submenu, hinting at a crossfade overlap during transition).

**UI/design notes:**
- Extremely minimal, all-white/light-gray, generous whitespace, small sans-serif labels+icons (~14-16px icons).
- The **submenu panel and the active dock tab visually merge** — the submenu's bottom rounded corners sit flush against the active pill's top, giving the illusion of one continuous expanding shape (a rounded speech-bubble / popover-from-tab effect), reinforced by matching border-radius and no visible seam/shadow break between them.
- Tab-to-tab switching swaps submenu content via what looks like a **fast crossfade** (no sliding of rows) while the *position* of the submenu panel stays fixed relative to whichever tab is active (so the panel itself may reposition/resize width to match the new active tab's x-position, growing/shrinking its own width per submenu's content).
- Inactive tabs collapsing to icon-only is a **space-saving accordion behavior** on the row of tabs — likely a `flex` width animation (label max-width 0→auto) combined with opacity fade of the text.

**Motion recipe:**
- Tab activate: inactive→active pill gets bg fill (white on gray) + label reveal (width+opacity), ~200ms ease; other tabs simultaneously collapse label (reverse animation) — a shared-timeline swap.
- Submenu open/reposition: panel translates horizontally to align above new active tab + resizes width to fit new content + crossfades old/new menu items, likely 250-350ms with slight ease-in-out; corner radius consistently rounded (~16-20px) to maintain the "merged shape" illusion throughout.
- Row hover highlight: instant/fast snap highlight per row (similar to IfKtexbhuTTx dropdown) — no sliding pill indicator within the submenu itself (contrast this with the *tab bar itself*, which likely DOES use a sliding/morphing indicator between tabs).

**What to copy:** The "merged shape" dock+submenu pattern (matching border radii + flush connection) is a very strong, reusable primitive for command-palette-adjacent nav — combine with the label-collapse-on-inactive tab behavior for a clean, space-efficient floating nav bar.

---

## 12. `XZahDT-hIKy19B3W` (18 frames, sampled odd: 1,3,5,7,9,11,13,15,17,18)

**Context:** Two related component demos in one loop: (a) a **day-of-week pill selector** with a giant animated word display above it (mon./tue./wed./thu./fri./sat./sun.) in a bold rounded custom font with a pink accent dot; (b) a **number-adjuster pill group** (-100/-25/-10/+10/+25/+100/Random) with a giant animated number display above it.

**Chronological motion (t=0→17s):**

*Day-selector (frames 1,3,5,7,9):*
- Each frame shows a different pill active (dark-filled bg, white text) among 7 white pills (mon–sun), with the **giant word above matching the active pill**, rendered in a chunky rounded custom typeface with a small pink/salmon period-dot accent beside it.
- The giant word text shows visible **motion blur/smear** on some letters when captured mid-transition (e.g., frame5 "wed." has a blurred trailing ghost beneath the crisp text, frame7 "tue." shows a faint double-exposure of the cursor rather than text) — indicating the big word **animates in with a fast directional motion** (likely a quick slide/skew or blur-in effect per letter, snapping to crisp final position), consistent with a playful "impact" text transition (like a blur transition or elastic pop) synced to pill selection.
- Cursor shown mid-click on the *next* pill each frame — confirms this is a scripted auto-demo cycling through all 7 days sequentially with a click-hand cursor.

*Number-adjuster (frames 11,13,15,17,18):*
- Frame11: number shows "-100" (just clicked -100 pill), giant digits rendered in a bold serif/sans numeral font with heavy motion-blur smear (visible ghosting on the "1" digit) — confirming the number **animates in with blur** each time it updates.
- Frame13: "100" (clicked -10 after some other clicks — value changed, still blurred/smeared at digit edges).
- Frame15: number area still transitioning (mostly blurred blob, barely legible) right after clicking "-25" — captured mid-blur, both digits fully obscured by motion blur, i.e. **the blur is strong enough to fully obscure text momentarily** during the count-up/count-down tween.
- Frame17: "0" cleanly displayed after "+100" click, sharp.
- Frame18: "-97290" (after clicking "Random") shown with strong directional smear across all 6 digits — the random button clearly triggers a **fast-rolling number tween** (like an odometer/slot-machine roll) that blurs heavily due to speed, then settles sharp.

**UI/design notes:**
- Playful, bouncy, rounded sans/custom typeface for both the pill labels and the giant display text — a toy-like, tactile aesthetic (similar to Cuberto/Basement-style micro-interaction demos).
- Pills: fully rounded, white default / dark-filled active, consistent sizing, simple binary state (no intermediate hover style visible).
- The giant number/word display sits centered above the pill row, large scale contrast against the small pills — a "hero stat" pattern.
- The **motion-blur-during-transition** technique is the standout signature move here: rather than a simple opacity crossfade, the text genuinely smears directionally (like a fast card-flip or velocity-based blur filter) suggesting an actual `filter: blur()` tied to animation velocity, or pre-rendered blurred frames mimicking a fast flip/scroll-through of values.

**Motion recipe:**
- Big text update on pill click: value changes via a fast "roll-through" tween (especially for numbers — behaves like an odometer/slot machine spinning through intermediate values), rendered with directional motion blur intensity proportional to speed, total duration ~150-400ms, settling sharp at the end.
- Pill active state: instant bg/text color swap (dark fill ↔ white), no animation needed on the pill itself — all the "flair" budget goes into the giant display text.
- For "Random" (larger jump), blur is stronger/longer than for adjacent small increments (+10/-10) — blur intensity scales with value delta / animation distance.

**What to copy:** Motion-blurred "roll" transition for large hero numbers/words on selection change — a distinctive, high-energy alternative to plain crossfade or slide, ideal for playful counters, stat displays, or word-of-the-day style widgets. Pair with simple binary pill selectors (no need for fancy pill animation — let the big text carry all the motion energy).

---

## 13. `GzSRyRqBUAEd1HC_` (20 frames, sampled odd: 1,3,5,7,9,11,13,15,17,19,20)

**Context:** A macOS-style app ("Nova") floating window over a blurred cityscape (Dubai skyline at dusk) wallpaper, demonstrating a **two-level sidebar navigation menu** — explicitly labeled in-app: *"The two-level menu animation is the focus here."*

**Chronological motion (t=0→19s):**
- Frame1 (t0): Collapsed state — just a small white circular button (hamburger-adjacent icon, looks like a "pointing hand"/cursor glyph) floating top-left over a mostly-empty white rounded panel, background city image visible behind/around it (the panel is smaller than full canvas, floating like a widget).
- Frame3 (t2): Panel has **expanded significantly downward and to fill more width**, revealing a full sidebar: app name "Nova" + close (X) top, "New note +" row, primary nav (Overview/Playbooks/Sessions [active, gray pill highlight]/Assets/Shared space/Activity/Connections), a divider, then a workspace-color list (Research [red dot]/Launch [blue dot]/Finance [green dot]/"Add workspace"), then bottom "Preferences" + a token-usage card ("120 tokens left today" with a purple ring icon). A second content pane area to the right is still empty/white (not yet populated).
- Frame5 (t4): "Playbooks" is now the active pill (gray highlight moved from Sessions → Playbooks, discrete snap, no visible slide-trail since only these two frames sampled), and the **right content pane now shows contextual heading+subtext** ("Playbooks / Reusable team workflows live here.") — this confirms a **two-pane master-detail layout**: left = nav list, right = detail panel whose heading crossfades based on selected nav item.
- Frame7 (t6): "Shared space" active, right pane updates to "Shared space / Team handoffs and references appear here." — consistent pattern, content pane heading changes per nav click (likely simple crossfade, given no motion-blur/slide artifacts visible).
- Frame9 (t8): "Connections" active, right pane "Connections / External tools connect into this workspace."
- Frame11 (t10): Still on Connections, but now an **inline "Add workspace" form has expanded** below the workspace dot-list — showing 4 color swatches (red/blue/green/purple) to pick from, a "Workspace name" labeled text input with value "works" being typed, and Cancel/Add buttons — a classic **inline expanding form** (accordion-style reveal pushing content below it down).
- Frame13 (t12): Form has been submitted — new item "● workspace" (red dot, matching the swatch picked) now appears in the workspace list, form has collapsed back, "Add workspace" link restored below the list — confirms optimistic-add pattern where the new list item **slides/fades into the list** and the create-form collapses smoothly.
- Frame15 (t14): "Assets" tab now active, right pane shows an empty-state illustration (small file icon, "No assets yet" heading with "Connections"/other text overlapping — likely a mid-crossfade frame where old heading text hasn't fully faded and new content is fading in simultaneously, causing legibility overlap) + "Upload asset" link, and a faint secondary header "Recent assets ⌄" at the very top of the pane (barely visible, ghosted in).
- Frame17 (t16): "Overview" active — right pane text explicitly reads "Overview / **The two-level menu animation is the focus here.**" confirming the whole demo purpose.
- Frame19 (t18) & Frame20 (t19): Panel has **fully collapsed back down** to the small circular button state (now showing a hamburger/3-line icon instead of the pointer icon seen in frame1) — loop resetting, with the button seemingly hovering in slightly different vertical position between these two nearly-identical closing frames (a tiny settle/bounce at the end of the collapse animation).

**UI/design notes:**
- Classic desktop-app "command palette meets settings panel" combo — white/off-white panel, generous padding, small-caps-free clean sans labels, colored dot bullets for workspace identity (like Notion/Linear workspace switchers), icon+label rows with consistent right-aligned trailing icons.
- Active-row highlight: soft light-gray rounded-rect background behind the label text (pill-ish but rectangular/soft, not fully rounded like the other videos' dock pills) — appears to **snap discretely row to row** rather than sliding smoothly (based on discrete frame sampling, though a smooth FLIP-style slide between adjacent rows is plausible given the "two-level menu animation" being explicitly called out as the star feature).
- Two-pane layout with independent scroll/content areas — left nav ~270px, right content flexible width, separated by a thin vertical divider line.
- The whole floating window itself **expands/collapses like an accordion from a tiny circular FAB into a full app window**, and back — panel corners stay rounded throughout (a persistent ~20-24px radius), suggesting the FAB and the full panel share the same corner-radius token, reinforcing shape continuity (similar "merged shape" philosophy as `b9wj_bvz_AWoGfMM`).
- Background: a blurred/muted photographic wallpaper is visible in the negative space, giving the whole demo a "live desktop widget" context rather than an isolated component shot.

**Motion recipe:**
- Panel expand: FAB (circle, ~40px) → full panel (rounded rect, large) — likely a scale+resize transform anchored at the FAB's position, radius interpolating from full-circle to the panel's rounded-rect value, ~300-450ms ease-out; icon inside cross-fades from pointer→hamburger (or vice versa on close) partway through.
- Nav row select: background highlight snaps (or slides) to new row; right-pane heading + subtext crossfade (~150-200ms) synced to the click, independent of the left nav's own highlight animation timing.
- Inline "Add workspace" form: expands downward (height auto 0→content, ~250ms), pushes trailing content (Preferences card) down accordingly; on submit, collapses back up while new list item fades/slides into the list above it.
- Close: reverse of expand, panel scales/collapses back into the FAB circle with a tiny bounce/settle at the very end (per frames 19-20 near-duplicate positioning).

**What to copy:** The FAB↔full-panel expand/collapse with shared corner-radius continuity is a superb pattern for a launcher/dock icon that "grows into" a full app surface; master-detail nav where the right pane content crossfades per left-nav selection; accordion-style inline "add new item" forms that push content rather than opening a modal.

---

## 14. `kpFQCmUnPyB7_0-a` (22 frames, sampled odd: 1,3,5,7,9,11,13,15,17,19,21,22)

**Context:** Dark-mode marketing pair of feature cards for a real-estate/title transaction product — left card: "Transaction Messaging" (chat UI mockup); right card: "Document Management" (file list UI mockup, macOS traffic-light dots). A scripted cursor loop ticks checkboxes on the right card and the effect ripples focus/blur across both cards.

**Chronological motion (t=0→21s):**
- Frame1 (t0): Both card contents are **heavily dimmed/blurred/illegible** (very low opacity, text unreadable) — an idle/unfocused "resting" state.
- Frame3 (t2): Right card sharpens slightly — "W-2 Form.pdf" row now legible with a green arrow/cursor icon hovering near its checkbox; left card still dim.
- Frame5 (t4): Left card **sharpens fully** — chat bubbles now fully legible ("Hello, I have reviewed the initial docs...", "Sure, let me scan it again in higher resolution."); simultaneously the right card's "Title application.pdf" **checkbox becomes checked (green filled checkbox with checkmark)**, green cursor icon lingering there — both cards briefly in focus together at the "payoff" moment.
- Frame7 (t6): Left card content **fades back toward dim/blur**, right card retains its checked state but text is now dimming too — attention receding.
- Frame9 (t8): Both cards near-fully dimmed again (similar to frame1), right card's checkbox item still shows green micro-cursor lingering (residual highlight), suggesting the "checked" visual state persists even as the card dims — confirming dimming is a pure **opacity/blur overlay**, not a state reset.
- Frame11-13 (t10-12): Right card's second row ("Title application.pdf") cursor re-hovers and re-confirms the check (cursor icon reappears exactly at the checkbox), left card cycles back to sharp/legible (chat visible again) — the loop is clearly **repeating the demonstration** (check row 1, then row 2, alternating) to show the checkbox feature multiple times.
- Frame15 (t14): Dimmed state again (transition trough).
- Frame17 (t16): Right card — now the **second checkbox row ("W-2 Form.pdf") gets the cursor+check treatment** (cursor visible near it, row about to be checked), left card fully sharp again with chat legible.
- Frame19 (t18): Right card's first row checkbox now shown **checked (green)** while cursor has moved to hover row 1 again — text/labels legible on right card only, left card dimmed.
- Frame21 (t20): Fully dimmed trough again (both cards).
- Frame22 (t21): Same dimmed trough, essentially identical to frame21 — confirms a resting "dim" state that the loop returns to and briefly holds before restarting.

**UI/design notes:**
- Both cards sit on pure black background, cards themselves are dark-gray rounded rects with generous padding — this is a **very dark, moody dashboard-style dual-feature showcase**, using illegibility itself (blur+dim) as the "resting/ambient" state, with legibility as the "active/demo" state — an inverse of typical UI (usually static + subtle idle animation; here the *content* itself pulses between hidden and revealed).
- Left card mimics a chat/messaging UI (sender bubbles left-aligned gray, "You"-labeled bubbles differently positioned) — realistic in-product screenshot style.
- Right card mimics a Finder-like file browser (traffic-light window dots, checkbox + file icon + filename + date + "⋮" menu per row, subtle divider line between rows).
- Card captions below each mockup ("Transaction Messaging — Dedicated chat for each job...", "Document Management — All title documents organized...") stay **fully legible/static at all times**, unaffected by the mockup dimming — only the *screenshot mockup area* pulses, not the marketing copy.

**Motion recipe:**
- Ambient dim/blur pulse: mockup content oscillates between ~15-25% opacity+blur (illegible, "resting/breathing") and ~100% opacity+no blur (legible, "spotlight") on a slow loop (~4-6s per half-cycle), independently per card but loosely coordinated so both aren't dim/sharp at exactly the same instant (adds liveliness, avoids feeling robotic/synced).
- Scripted cursor: an autonomous cursor (rendered as a color-accented arrow icon, green here matching brand accent) moves to a target (checkbox), "clicks" (icon changes to a filled/pressed glyph momentarily), triggering the checkbox check animation (border+fill transition to solid green, checkmark icon fades/scales in) — classic "autoplay product demo" scripting.
- Checkbox check: unchecked (outline square) → checked (filled green square + white checkmark), ~150-200ms, likely scale-pop on the checkmark itself for tactile feedback.

**What to copy:** Ambient dim↔sharp "breathing" pulse to draw attention to alternating feature screenshots without requiring user interaction — useful for landing-page feature grids where multiple mockups need to take turns being "the focus" without hard cuts; scripted autoplay cursor + checkbox-check micro-interaction is a solid, reusable demo-loop technique for showing off list/checklist features.

---

## 15. `O3Sjv2U1Dc_N8DSe` (27 frames, sampled every ~3: 1,4,7,10,13,16,19,22,25,27)

**Context:** SaaS landing-page hero for an "AI for Cultural Institutions" product — dark theme, top nav (logo "Lorem", Solutions/Customer service/About us/Careers links, Log in / Try now buttons), large headline + subcopy + two CTAs ("Book a call" white pill, "Try now" dark pill), and below it a **continuously auto-scrolling 3D-tilted shelf of book covers** stretching off both edges of the viewport.

**Chronological motion (t=0→26s):**
- The book "shelf" is arranged in a **perspective-skewed diagonal row** (books appear to recede in scale/rotation from left-large to right-small, like a bookshelf viewed at an angle, or a horizontal carousel with a fisheye/3D-transform applied per item based on position).
- Across all sampled frames (t0, t3, t6, t9, t12, t15, t18, t21, t24, t26), the **entire row of books continuously translates leftward** — comparing frame1 (leftmost visible book: "The Dragon & The Fox") to frame27 (leftmost visible book has shifted to a partially-cropped different cover, with "The Gorgon Medusa"/"The Lantern Dragon" now prominent on the right side, previously off-screen at frame1) — a classic **infinite horizontal marquee/ticker**, continuous constant-speed scroll (no easing, no pause), individual books entering from the right edge and exiting past the left edge seamlessly (looping dataset).
- A faint radial "radar rings" decorative graphic sits centered in the dark background behind the headline — appears completely static (no rotation/pulse detected across samples), purely a static texture.
- A mouse cursor is visible drifting across different horizontal positions frame to frame (t3 near x≈300, t6 near x≈790, t9 near x≈260, t13 near x≈320, t19 near x≈790) — but there's no visible hover-state change on the books themselves (no scale-up/tilt-correction on hover detected in this sample), suggesting either the interaction is very subtle or the cursor movement is just incidental/scripted b-roll rather than a deliberate hover demo.
- Nav bar, headline, subcopy, and both CTA buttons remain **perfectly static** throughout — all motion energy is spent on the book shelf alone.

**UI/design notes:**
- Rich, painterly book-cover illustrations (varied genres: fantasy dragon, portrait/romance, nature/floral, "Eleanor Vance" gothic, minimalist typographic covers, a lighthouse "Light Keeper", a mystical "Gorgon Medusa" gold-on-cream, a winged dragon "Lantern Dragon") — a diverse, richly-art-directed dataset used to make the scroll feel like a real living library rather than placeholder content.
- Each book rendered with a visible **spine-edge (thin white/cream strip)** adjacent to its front cover, reinforcing the 3D "standing book" illusion within a flat 2D layout via consistent perspective/skew transforms per item (likely CSS 3D transforms: `rotateY` + `translateZ` + scale, decreasing as items go further right, i.e., a fixed perspective camera with items fanned along a shallow arc).
- Dark maroon/black vignette border framing the whole hero section (visible as a dark red-brown gradient edge in all frames) — adds a "cinematic" framing device distinct from the rest of the page.
- Top nav uses fully-rounded pill buttons ("Log in" outline, "Try now" filled) — consistent with the broader trend across this whole reference set of favoring pill/capsule shapes for primary actions.

**Motion recipe:**
- Infinite marquee: constant linear-velocity horizontal translate (no ease-in/out, since it must loop seamlessly), duplicated/looped dataset so the scroll never visibly "resets"; likely implemented via a doubled array of book items translated by `-50%` of total width in a `@keyframes` loop, or a JS ticker incrementing `translateX` every frame.
- Per-item 3D perspective: each book has a fixed `rotateY`/skew value based on its slot position (not dynamically recalculated on scroll — the fan/perspective shape is a static per-item transform, only the whole group's `translateX` animates), giving a cheap but convincing "shelf" 3D effect without true WebGL.
- Everything else on the page: zero motion — a strong "one hero motion element, everything else static" discipline, similar in spirit to `ST358aeEsQjs4IOs`'s isolated ASCII motion.

**What to copy:** Infinite-marquee "3D bookshelf/card fan" hero pattern — great for portfolio/catalog/library-style products; achieve the 3D look cheaply via per-item static `rotateY`/scale transforms + one continuous group-level `translateX` loop, rather than true 3D physics; frame the whole hero with a subtle vignette/border to give it a "staged" cinematic quality distinct from the page chrome.

---

# Cross-Video Synthesis: Motion Vocabulary for `pera-design`

Looking across all 15 clips, a small number of **recurring motion primitives** account for nearly everything observed. These should become the backbone of the skill's motion vocabulary.

### 1. Shape-morphing pills & "merged" surfaces
Seen in: `aKdNBnalLLDZ9I4E` (share button), `b9wj_bvz_AWoGfMM` (dock+submenu), `NdzGrWjo-A_vdJYW` (tab+popover), `GzSRyRqBUAEd1HC_` (FAB↔panel), `XZahDT-hIKy19B3W`/`kpFQCmUnPyB7_0-a` (pill groups).
- **Rule:** Anything that expands from a small trigger keeps the **same corner-radius family** as it grows, so the eye reads it as one continuous shape stretching, not two separate elements swapping. Popovers/submenus should visually "grow out of" their anchor (flush corners, no shadow seam).
- **Rule:** Buttons that carry loading/success states **morph their internal content** (icon+label swap, width auto-animates) rather than just changing color. Auto-revert success states after ~1.5-2s.
- Timing: expand/collapse ~250-450ms ease-out; content crossfade inside a morph ~150-200ms, usually overlapping the shape animation rather than sequenced after it.

### 2. Discrete "snap" highlights vs. smooth "slide" indicators
Seen in: `IfKtexbhuTTx_0E7` (dropdown row hover — snaps), `b9wj_bvz_AWoGfMM` (submenu row hover — snaps), `GzSRyRqBUAEd1HC_` (nav active row — likely snaps), `XZahDT-hIKy19B3W`/`aKdNBnalLLDZ9I4E`-style pill groups (binary color swap, no slide).
- **Rule:** For **menu/list row highlights**, prefer an instant/near-instant background snap (native-OS feel) over a smoothly sliding pill — this reads as "correct" for menus, dropdowns, and file lists.
- **Rule:** For **top-level tab bars** (the "spine" of a nav, not its submenu), a sliding/expanding active-indicator with label reveal (icon-only → icon+label) is preferred, as it communicates "this is the primary navigation state" (seen in `b9wj_bvz_AWoGfMM`'s inactive-tab-collapse behavior).

### 3. Focus via blur, not just dimming
Seen in: `AyjZ4KBSdW4rd_pH` (search/feedback overlays blur the page), `kpFQCmUnPyB7_0-a` (ambient dim+blur pulse between two feature cards), `NdzGrWjo-A_vdJYW` (background blurs heavily behind popovers).
- **Rule:** When drawing attention to a foreground element (modal, toast, feedback badge), animate **both opacity dim AND blur radius** on the background (0px → 10-20px), not dim alone — this feels more premium/"Apple-like" than a flat scrim.
- **Rule:** For dual/alternating feature showcases, an ambient breathing dim↔sharp cycle (4-6s half-period, phase-offset between panels) can substitute for hard cuts, keeping multiple demos visible without requiring interaction.

### 4. Progressive / staggered reveal for lists and multi-step content
Seen in: `J1tkpQpyuCuEQegI` (receipt printing line-by-line + live counter), `C0pQnQgiNyz3Glxb` (staggered independent loaders per row).
- **Rule:** When revealing sequential content (receipts, generated text, checklists), pair a **live counter or progress label** with a staggered per-line reveal (~60-150ms stagger) — the counter reassures the user of progress even if the visual reveal is fast.
- **Rule:** For multiple simultaneous "in progress" indicators (loading rows/pills), **phase-offset their internal animations** (~150-300ms apart) so they don't all pulse in lockstep — this alone makes a loading state feel more "alive"/organic vs. mechanical.

### 5. Physical/skeuomorphic metaphors for destructive or "weighty" actions
Seen in: `aKdNBnalLLDZ9I4E` (note → crumpled paper → trash can; note → paper shredder with flying strips).
- **Rule:** For delete/destroy actions on content cards, consider a literal physical metaphor (crumple+drop, shred+scatter) instead of a flat fade-out — adds delight without harming usability, provided the metaphor completes quickly (~1-1.5s) and doesn't block the next action.

### 6. Motion blur as a transition signature for big/hero numerals or words
Seen in: `XZahDT-hIKy19B3W` (day-word and number displays smear on change).
- **Rule:** For large "hero stat" text that updates on a discrete input (counter, word-of-day, big KPI), use a **directional motion-blur "roll-through"** transition rather than a plain crossfade — blur intensity/duration should scale with the size of the value jump (small increments = brief/light blur, "random"/big jumps = longer/heavier blur, odometer-style).

### 7. Ambient generative/particle backgrounds, strictly isolated from UI chrome
Seen in: `ST358aeEsQjs4IOs` (ASCII art breathing), `boDfEvhGAyUpMS4w` (halftone fluid noise), `O3Sjv2U1Dc_N8DSe` (bookshelf marquee), `8674g6CBFaebKcVx` (idle mascot shimmer).
- **Rule:** Ambient/generative motion (particle fields, ASCII art, dithered fluid sims, infinite marquees) should be confined to a clearly bounded decorative region (card header, hero band) while **all surrounding UI chrome (buttons, labels, nav) stays perfectly static**. This "one motion element per view" discipline is what keeps these premium-feeling rather than noisy.
- **Rule:** When the same generative motif repeats at multiple scales/positions on a page (e.g., a big hero animation + a small repeated icon strip using the same rendering style), **phase-offset** each instance so they don't visually synchronize.

### 8. Layout patterns favoring the fully-rounded "pill" as default action shape
Seen in nearly every video: CTA buttons, tab bars, day/number selectors, toolbars, badges.
- **Rule:** Default to `border-radius: full` (pill) for primary buttons, selector chips, and nav bars across the skill — square/soft-rect (16-24px radius) is reserved for containing surfaces (cards, panels, dropdowns), never for interactive action chips. This radius hierarchy (chips = pill, panels = large-radius rect) is the single most consistent visual rule across all 15 references.

### 9. Two-pane / master-detail crossfade
Seen in: `GzSRyRqBUAEd1HC_` (nav list → detail pane heading crossfade).
- **Rule:** When a left-nav selection updates a right-content area, animate only the **heading/summary crossfade** quickly (~150-200ms) rather than re-animating the whole pane — keeps navigation feeling instant while still softening the content swap.

### Suggested timing/easing defaults to encode in the skill
| Motion type | Duration | Easing | Notes |
|---|---|---|---|
| Icon/tooltip crossfade | 120-200ms | ease-out | no movement, just opacity |
| Pill/button state morph (loading→success) | 200-350ms per phase | ease-in-out | width auto-animates with content |
| Popover/submenu expand from anchor | 250-350ms | ease-out (slight overshoot ok) | matching corner radius throughout |
| Panel/FAB expand-collapse (app shell) | 300-450ms | ease-out expand / ease-in collapse | tiny bounce/settle at collapse end |
| Row hover highlight (menus/lists) | ~0-80ms (near-instant) | linear/snap | native menu feel |
| Background blur focus-in | 200-300ms | ease-out | pair with foreground scale-in 0.9→1 |
| Staggered list reveal | 60-150ms stagger, ~200-300ms per item | ease-out, translateY 4-8px | pair with live counter if long list |
| Hero numeral "roll" transition | 150-400ms (scale w/ delta) | directional blur, settle sharp | odometer-style for big jumps |
| Ambient breathing/pulse (dim↔sharp, ASCII density) | 4-8s half-cycle | sine/ease-in-out | phase-offset duplicates |
| Infinite marquee | linear, constant velocity | none (must not ease for seamless loop) | duplicate dataset for seamless wrap |

---

*End of `sources/videos.md` content.*

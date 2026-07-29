# pera-design v2 — what changed and why

Rewritten against Anthropic's *The new rules of context engineering for Claude 5 generation models* (Jul 24, 2026). The design opinions are unchanged — the packaging is.

## Install

```bash
# from the repo root, replacing the old skill folder
rm -rf .cursor/skills/pera-design
cp -R <this-zip>/.cursor/skills/pera-design .cursor/skills/

# refresh the personal install
cp -R .cursor/skills/pera-design ~/.cursor/skills/
```

`references/` and `sources/` are untouched and stay where they are.

## Size

| | v1 | v2 |
|---|---|---|
| SKILL.md (always loaded) | 14.4 KB | 4.1 KB |
| Skill markdown total (excl. `sources/`) | 79.9 KB | 40.3 KB |
| Copy-paste-ready code shipped with the skill | 0 KB | 11.5 KB |

## The five changes

**1. The entry file got 75% smaller.** `SKILL.md` was 150 lines carrying 21 hard rules, a 9-step workflow, a one-shot protocol, a 17-item pre-ship checklist, a 20-item anti-slop list and an evidence table — most of it duplicated in the satellites it links to. v2 keeps 7 non-negotiables, the working method in a paragraph, the ship bar in a paragraph, and a routing table.

**2. Each fact now lives in exactly one place.** `prefers-reduced-motion` appeared in 8 files; "2–4 signature details" in 6; "one Canvas UI per view" in 5; anti-patterns were scattered across 6 lists. Repetition was a workaround for older models that skimmed context, and it actively hurts now — near-duplicate rules that drift apart read as conflicting instructions. All bans are consolidated into `reference/antipatterns.md`; every other file points at it.

**3. Rules became judgement where the rule was sometimes wrong.** v1 said "never code without a master spec" — true for a landing page, absurd for one button, so the model had to silently decide when to disobey. v2 scopes it: spec anything larger than a component. Same for "cards are rare" (which contradicted the Amicro card section) and "desktop-first is allowed when honest". The hard rules that survived are the ones that are always true.

**4. Code replaced prose where prose was pretending to be code.** `TOKENS.md` described tokens; `code/tokens.css` *is* the token file, with all three directions, the theme-flip block, the fingerprint layer and the reduced-motion switch — paste it in and it works. `SIGNATURE.md` embedded fragments; `code/signature.css` and `code/signature.js` are complete and importable, with a clean contract: JS only ever writes `--p` and toggles one root class, so the no-JS state is correct by construction. Per the article, a working file is a higher-fidelity reference than a description of one.

**5. Progressive disclosure by task, not by topic.** The routing table says *when* to read each file, not just what it contains — so a "fix this dropdown" task loads `craft.md` and nothing else, while a full landing page pulls the spec template and signature catalog.

## File map

```
SKILL.md                    entry — non-negotiables, method, ship bar, routing
code/tokens.css             was TOKENS.md — now real CSS
code/signature.css          was embedded in SIGNATURE.md — now complete
code/signature.js           new — scrub, theme flip, word split, spine fallback
reference/directions.md     was PRINCIPLES.md + VOCABULARY.md
reference/craft.md          was CRAFT.md, deduplicated
reference/signature.md      was SIGNATURE.md, code extracted
reference/motion.md         was MOTION.md, kill-switch moved to tokens.css
reference/patterns.md       was PATTERNS.md, bans removed
reference/antipatterns.md   new — every ban, once
reference/spec-template.md  was AUTOPROMPT.md, halved
reference/canvasui.md       was CANVASUI.md, install table collapsed
reference/amicro.md         was AMICRO.md, tables collapsed
sources/                    unchanged
```

## v2.1 — what a real build taught the skill

Added after shipping an animation-heavy illustrated page end to end. Everything below is evidence from that build, not theory: each item is a failure that survived code review and died to a browser probe, or a capability the skill had no words for.

**`reference/imagination.md` — new, and the reason for a new rule in `SKILL.md`.** The skill had a full catalog of signature *mechanisms* and nothing about where an idea comes from, so a page could be assembled correctly and still be anonymous — one of the two failure modes named in the entry file. The answer that came out of the build: imagination is not invention, it is specificity harvested from the subject. The file is a six-row inventory (places, rituals, voice, material, time, tension) filled from real source material, a map from each row to a page decision — places name the sections, time chooses and places the flagship — and the swap test: at least one moment per page must be impossible on any other page. It is written to be executable by a small model, ending in an eight-step short version, and it draws the line between imagination and hallucination explicitly, because that line is where weaker models fall off.

**`reference/verify.md` and `code/verify.js` — new.** The gap flagged below as "nothing in the skill verifies itself" is now a file plus working code — `pera.report()` returns first-viewport fit, overflow, decoded images, uncropped-inside-a-pin and a control audit in one call. It covers pinning the viewport before screenshotting, the four geometry probes (first-viewport fit, horizontal overflow, uncropped-inside-a-pin, images actually decoded), sampling scrubbed and triggered states in both directions, using the accessibility tree as the fastest craft audit, and reporting what went unverified. Also the two tooling traps that cost the most time: smooth-scroll libraries clamping programmatic scroll, and screenshots caught mid-repaint showing loaded images as blank.

**`reference/assets.md` — new.** The skill said "content carries color" but never said where content comes from. This is the browser-driven route to real artwork and real copy: harvesting `background-image` and lazy attributes when the art isn't in `<img>` tags, clicking through interactive galleries to pull the whole data set, slicing a spritesheet by alpha-column gaps and cropping losslessly with `sips`, staging transparent cutouts on a ground line, and the rights line a tribute page owes its source.

**`craft.md` gained "Viewport and chrome".** The first viewport is arithmetic before it is taste: constant-height sticky chrome, `calc(100svh - var(--chrome-h))`, hero as `1fr auto` rows rather than absolute overlays, figures on a shared baseline.

**`spec-template.md` gained section 0** — the subject inventory now precedes the stack, so section names are derived before anything is built, and the acceptance checklist names the one detail that fails the swap test.

**`antipatterns.md` gained "Anonymity", "Layout traps" and "Handoff".** Nine layout failures that read as correct in the source — full-height sections overflowing by exactly the header height, chrome that resizes when it compacts, composition elements parked as siblings of the grid, dead half-screens, cards clipped inside a pin, triggers bound to scroll bands catching their neighbours, `loading="lazy"` in transformed tracks — plus the handoff bans: reporting a check as passed when only the code was read, and implying coverage you don't have.

**`SKILL.md` ship bar** now says the quiet part: reading your own code is not verification, and source real material before inventing it.

## v2.2 — Emil Kowalski skills vendored

Vendored the full [emilkowalski/skills](https://github.com/emilkowalski/skills) tree (MIT) under `reference/emil/`: `emil-design-eng`, `review-animations`, `improve-animations`, `find-animation-opportunities`, `animation-vocabulary`, `apple-design`, `pick-ui-library`, plus their satellite files (`STANDARDS.md`, `AUDIT.md`, `PLAN-TEMPLATE.md`).

Pera stays the composition authority; Emil is progressive-disclosure for motion craft. `SKILL.md` gained routing rows; `motion.md` points at the index; `emil-design-eng` drops the upstream greeting gate so it can run as a satellite. Upstream pin and license live in `reference/emil/SOURCE.md` and `LICENSE`.

## Still worth doing

- `sources/*.md` is 136 KB of raw evidence and nothing routes into it precisely. Consider one `sources/README.md` index mapping claim → file → section, so a rule can be traced without loading 60 KB.
- The Uber navbar is cited as the craft bar but it is one control. Two or three more finished components (a dropdown, an input, a modal) would make the bar a reference set instead of a single sample — code beats description here too.
- `code/verify.js` covers geometry, images and controls. Contrast ratios, focus-ring visibility on both grounds, and the reduced-motion end state are still eyeball checks — they could be probes too.

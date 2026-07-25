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

## Still worth doing

- `sources/*.md` is 136 KB of raw evidence and nothing routes into it precisely. Consider one `sources/README.md` index mapping claim → file → section, so a rule can be traced without loading 60 KB.
- The Uber navbar is cited as the craft bar but it is one control. Two or three more finished components (a dropdown, an input, a modal) would make the bar a reference set instead of a single sample — code beats description here too.
- Nothing in the skill verifies itself. A `verify.md` with the browser QA loop (screenshot at two widths, tab through, scroll both directions) would let the checklist become a callable step rather than prose in the entry file.

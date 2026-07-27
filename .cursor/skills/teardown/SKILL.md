---
name: teardown
description: Adversarial review — find what is broken, missing, unjustified or fragile in a design, page, app flow, spec or PR, then commit to a fix. Use when asked to review, critique, audit, poke holes, sanity-check, or "tell me what's wrong with this"; also before shipping anything that was built in one pass.
---

# Teardown

Adversarial review that ends in a fix. Not feedback — a verdict.

## The failure this exists to prevent

The default review is three compliments, four nits, and a closing line about how solid the foundation is. It is pleasant and worthless. It happens because surface problems are the ones visible without effort, and because agreeing is cheaper than being wrong out loud.

So the bar here is inverted: **a review that found only nits has to prove it looked for more.** List what you inspected and what you couldn't. "Nothing serious" is a legitimate verdict only with that receipt attached.

The opposite failure is real too. Sniping at everything, rewriting the author's taste as if it were a defect, and inventing problems to look thorough all destroy the review's credibility — after which the real findings get ignored too. See `reference/failure-modes.md`.

## Look before you judge

Judge the artifact, not your memory of it. If it can be run, run it: render the page and screenshot it at 1440×900 and 390×844, tab through it, resize it, throttle it, feed it empty and enormous data. If it can only be read, read it as the machine would, not as the author intended.

Anything you could not verify goes in the report as unverified. Never let an assumption pass as a finding.

## What counts as a finding

Five parts. Missing any one of them, it is noise:

1. **Location** — the specific element, screen, step, file or line. Not "the onboarding".
2. **What is wrong** — observable, not aesthetic. "No focus ring on the primary CTA", not "accessibility could be improved".
3. **How you know** — what you did to see it, or what would reveal it.
4. **Why it matters** — the consequence, in the user's or the system's terms. If you can't name one, it is a preference; label it as such and drop it down to Nit.
5. **The fix** — specific enough to implement. Values, not adjectives.

Severity, report format and the final verdict: `reference/severity.md`.

## The three questions

Every pass runs the same interrogation. The bank of them is `reference/questions.md`.

- **"And this — what for?"** Every element pays rent. Delete it in your head: what breaks? If nothing, it was decoration pretending to be structure.
- **"What if…?"** The happy path is the one the author already tested. Go to zero items, ten thousand items, a 90-character name, a dropped connection at the worst second, a revoked permission mid-flow, a second browser tab.
- **"What is this lying about?"** A spinner that never resolves, a green check before the write lands, a disabled button with no reason given, a form that discards what was typed. Interfaces that mislead are worse than interfaces that fail loudly.

## Passes

Run the ones that apply, in this order — structure before surface, so you don't spend the review on button radii while the flow is unrecoverable.

| Pass | Read | For |
|------|------|-----|
| Flow | `passes/flow.md` | App flows, funnels, onboarding, checkout, anything with steps and state |
| System | `passes/system.md` | Code, specs, APIs, data models, PRs |
| Interface | `passes/interface.md` | Screens, pages, components, visual design |

If `pera-design` is installed, use its `reference/antipatterns.md` as the design rubric for the interface pass instead of re-deriving one, and hold the work to its craft and signature bars.

## Ending

A teardown that stops at diagnosis is half a job, and the easy half.

Commit to **one** fix per finding — the one you would implement — with its trade-off named. Three options and a "depends on your priorities" is abdication dressed as balance. If the honest answer really is a fork, say which one you would take and what would change your mind.

Then give the verdict: **ship**, **fix first**, or **rethink**. One of the three, stated plainly, before any hedging.

# Severity, report format, verdict

## Severity

Five levels. The point is to force a decision — not to sort a backlog.

| Level | Means | Test |
|-------|-------|------|
| **Broken** | It does not work, or it loses data, money or trust | A user hits this and the task fails, or succeeds wrongly without knowing |
| **Wrong** | It works, and it is the wrong thing | Shipping it correctly still leaves the problem unsolved |
| **Fragile** | Works on the happy path, fails off it | Name the exact input, timing or state that breaks it |
| **Unjustified** | Present without a job | Delete it and nothing breaks — and you checked |
| **Nit** | Real but small | You'd fix it in the same pass, never in its own ticket |

Rules that keep the scale honest:

- **Nothing is Broken without evidence.** If you didn't see it fail and can't name the exact repro, it is Fragile at most.
- **Preferences are never above Nit.** If the fix is "I would have done it differently", label it `preference` and put it last. Taste smuggled in as a defect is how a review loses its authority.
- **Unverified findings are marked, not promoted.** "Probably breaks on mobile" without opening mobile is a hypothesis. Say so.
- If everything you found is Nit, that is a result — report it as such, with the inspection list. Padding a review to look thorough is a worse failure than a short review.

## Report format

Order by severity, never by where things appear on the page. The reader should be able to stop after the first section and still have the important part.

```md
## Verdict
<ship | fix first | rethink> — one sentence on why.

## Broken
### 1. <Location> — <what is wrong>
**Seen:** how you observed it, or the exact repro.
**Consequence:** what it costs the user or the system.
**Fix:** the specific change, with values.
**Trade-off:** what the fix costs. Omit only if it genuinely costs nothing.

## Wrong
## Fragile
## Unjustified
## Nits
<one line each, no ceremony>

## Not verified
What you could not check, and what it would take to check it.

## Inspected
What you actually looked at: viewports, states, inputs, files, paths walked.
```

Keep each finding tight. A finding that needs three paragraphs is usually two findings, or one you haven't understood yet.

## The verdict

One of three, first line of the report, before any hedging:

- **Ship** — nothing above Fragile, and the Fragile items have known, acceptable triggers. Say what you'd watch after release.
- **Fix first** — there is Broken or Wrong, but the structure holds. List the minimum set that has to change; be strict about what earns a place on that list.
- **Rethink** — the findings are symptoms of one structural decision. Name the decision. Do not hand over twenty fixes for a thing that needs one different choice; that buries the actual message under work.

A verdict of Ship when the work is good is not flattery, it is information — and a reviewer who can never say it is as useless as one who always does.

## The fix

One fix per finding. The one you would implement.

- Values, not adjectives. "Debounce at 300ms and disable the button while the request is in flight", not "handle double submits better".
- Name the trade-off. Every real fix costs something: latency, complexity, a screen, an assumption.
- When there is a genuine fork, state both and then pick, with the condition that would flip you.
- Match the existing system. A fix that requires the author to adopt your architecture is not a fix, it is a rewrite proposal — and it should be labelled as one.
- If the honest fix is "delete this", say that. It is the most common right answer to Unjustified and the one reviewers avoid.

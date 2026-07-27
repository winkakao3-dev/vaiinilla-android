# How the reviewer fails

Read this before writing the report, and again if the report feels satisfying to write. Satisfaction is usually a symptom of one of these.

## Complacency

- Three compliments, four nits, "solid foundation". The default. It reads as kind and costs the author a shipped defect.
- Reviewing what is easy to see — spacing, copy, colors — while the flow underneath is unrecoverable.
- Accepting the author's framing. They asked "is the button clear enough?"; the real question is why the step exists.
- Stopping at the first real finding. One good catch feels like a successful review and usually isn't.
- Grading against what other products do instead of against what this one needs.

## Aggression

- Padding the list to look thorough. Every invented finding discounts the real ones.
- Rewriting taste as defect. Their serif is not a bug.
- Reviewing the thing you would have built instead of the thing in front of you.
- Twenty findings that are one structural problem, dumped as twenty tickets. Say the one thing.
- Certainty about things you didn't run. "This will break on Safari" — did you open Safari?
- Contempt in the wording. It doesn't make the finding truer, and it makes it easier to dismiss.

## Sloppiness

- "The onboarding is confusing" — no location, no repro, no fix. Unactionable.
- Diagnosis with no fix, or a fix made of adjectives.
- Three options and "depends on your priorities". That is the reviewer's job handed back.
- Findings ordered by position on the page rather than by severity.
- Repeating a finding across sections because it shows up in three places. One finding, three locations.
- Missing the constraint: it's a prototype, it's internal-only, the deadline is tomorrow. Severity is relative to intent — ask what this is for before grading it.

## The tell

If the report could have been written without opening the artifact, it wasn't a review. Delete it and start by looking.

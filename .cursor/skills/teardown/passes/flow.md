# Pass — flow

For anything with steps and state: onboarding, checkout, signup, invites, settings, a wizard, a funnel. Run this **before** the interface pass. A beautiful screen inside a broken flow is a wasted review.

## Walk it as a person, not as the author

Do the flow end to end and write down every step. Then do it again wrong: back out at step 3, refresh at step 2, arrive at step 4 by deep link, use it with an account that already did it once. Most defects live in the second walk.

## Map before judging

For each step, name four things:

1. **The job** — what the user is accomplishing here, in their words, not the system's.
2. **The state** — what exists after this step that didn't before. Where it lives. Who else can see it.
3. **The exits** — forward, back, cancel, close the tab, and what each one does to the state.
4. **The failure** — what happens when this step's request fails, and what the user can do about it.

A step where you can't fill in all four is a finding on its own.

## The interrogation

Run `reference/questions.md`. These angles bite hardest on flows:

**Steps that shouldn't exist.** Which step asks for something the system already knows, or could ask for later, or could infer? Which one is a confirmation of an action that was already reversible? The shortest path to value is the benchmark — count how many of these steps are on it.

**Order.** Is anything asked before the user has a reason to care? Payment before value, permissions before context, profile fields before the first success. Front-loaded friction is the most common abandonment cause and it never looks like a bug.

**Recovery.** At every step: they made a mistake — what's the move? An undo, a back that preserves input, an edit later, or nothing. "Nothing" is a finding. Especially after irreversible actions where the confirmation was a one-line dialog.

**Interruption.** Real users leave. Close the tab at each step and come back: is progress kept, is it obvious where they were, does the system re-ask what it already has? Sessions expire, phones die, tabs multiply.

**Dead ends.** Any state a user can reach with no forward path and no explanation: permission denied, empty result, unsupported file, expired link, an account that exists but can't sign in. Every dead end needs an exit that isn't the back button.

**Concurrency and repetition.** Two tabs. Double submit. A retry after a timeout that actually succeeded. A webhook delivered twice. Does the system duplicate, or hold?

**Truth.** Where does the flow tell the user something it doesn't know yet? "Confirmed" before the write lands, "we'll email you" with no queue behind it, a success screen after a request that silently failed.

**The second time.** The flow was designed for a first-time user. Run it as someone who has done it twenty times — the tutorial that can't be skipped, the confirmation that has become noise, the default that's wrong for the returning case.

**Who else is in this.** Multi-user flows: what does the other person see, and when? Invites to someone who already has an account, shared links after access is revoked, a teammate editing the same object.

## Findings that keep hiding here

- The empty state of a flow, not a list — what a brand-new account sees before anything exists.
- The error copy, which is almost always written last and says "Something went wrong".
- Where the money or the data actually commits, versus where the UI implies it did.
- The step that exists because of the team's internal structure, not the user's task.
- Anything the user is forced to remember between steps — a code, an ID, a filename — that the system had and dropped.

## Then

Findings go through `reference/severity.md`. Flow defects skew Broken and Wrong; be strict about the difference between "a step is annoying" (Nit) and "a step loses work" (Broken).

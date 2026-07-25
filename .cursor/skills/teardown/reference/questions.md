# The question bank

Not a checklist to recite — a set of angles that reliably surface things the author stopped seeing. Pick the ones that bite. A question that produces nothing on this artifact was still worth asking once.

## "And this — what for?"

The justification pass. Aim it at anything that survived only because nobody questioned it.

- Delete it in your head. What breaks? If nothing breaks, why is it there?
- What job does it do that its neighbour doesn't already do?
- Who asked for it — a user, a stakeholder, or a template?
- Could two reasonable people read this element as meaning different things? Then it means nothing.
- Is this solving the problem, or is it the visible residue of the team's internal structure?
- Would a first-time user notice it was gone? Would anyone?
- Is this a real affordance, or a decoration shaped like one — a card that isn't clickable, a chevron that opens nothing, a search that only filters what is already visible?
- Three things here compete for the same attention. Which one is supposed to win? If the answer isn't obvious in one second, none of them do.

## "What if…?" — cardinality

The author tested with three items. Nobody has three items.

- Zero. Is the empty state designed, or is it a blank area with a sad sentence?
- Exactly one. Does the grid still make sense with a single tile? Does "1 items" appear?
- Two, where the design assumed an odd number, or a centered layout.
- Ten thousand. Is there pagination, virtualization, a search that actually narrows? Or does the page just die?
- The one that never loads — an image 404, a video that won't decode, an avatar that doesn't exist.
- Negative, zero-value, decimal, currency with no cents, a quantity of 0 in a cart.

## "What if…?" — data

- A 90-character name. A single-letter name. Four surnames. No last name at all.
- Emoji, accents, RTL text, CJK, HTML tags typed into a text field.
- A value that is technically valid and absurd: a birthdate in 1890, a price of €0.00, a file of 4 GB.
- Whitespace-only input. Copy-pasted text carrying invisible formatting.
- Null where the design assumed a value — no avatar, no description, no due date. Does the layout collapse or hold?

## "What if…?" — time and failure

- The request takes 8 seconds. Is there feedback, or does the interface look frozen?
- The connection drops at the worst possible moment — after payment, before confirmation. Where does the money live?
- The server returns 500. Does the user see the error, or a spinner forever?
- The user double-clicks submit. Two orders? Two emails? Two charges?
- Two tabs open, both editing. Who wins, and does anyone get told?
- Session expires mid-form. Is the typed content preserved, or punished?
- Permission is revoked while the user is inside the flow.
- The clock is wrong, the timezone isn't the server's, the date crosses a DST boundary.
- It runs twice — a retry, a refresh, a webhook redelivery. Is it idempotent, or does it duplicate?

## "What if…?" — the human

- First use versus the third year: does the interface that teaches also become the one that slows you down?
- They made a mistake. Can they undo it? Is the undo discoverable within the seconds they'd look for it?
- They left and came back tomorrow. Is state preserved, and is it obvious where they were?
- They hit the browser back button. Does the app agree with the browser about where they are?
- They deep-link into step 4 without doing 1–3.
- They share the URL. Does the recipient see what the sender saw, or a login wall with no return path?
- They're on a phone, one-handed, in sunlight, on the subway with 2 bars.
- They only have a keyboard. They only have a screen reader. They can't tell red from green.
- They're annoyed. Where does this design punish an impatient person — an animation that blocks, a confirmation that doesn't need to exist?

## "What is this lying about?"

The highest-value pass, and the one almost nobody runs.

- A success state that fires before the write is confirmed.
- A progress bar not connected to progress.
- A spinner with no timeout and no failure branch.
- A disabled control that never says why, so the user has no move left.
- "Saved" that means "queued". "Deleted" that means "hidden".
- A count that's stale, a badge that never clears, a timestamp in an ambiguous zone.
- Copy in the imperative that the interface can't deliver: "instantly", "secure", "unlimited".
- An empty state that says there's nothing, when really the filter is wrong or the request failed.
- A form that validates on submit what it could have validated on blur — punishing the user for information the system had all along.

## Structural questions

For when nothing individual is wrong and it still doesn't work.

- What is this screen's one job? Can you say it in a sentence? Does the layout agree?
- Where does a new user's eye land first, and is that the thing that matters most?
- What is the shortest path to value, and how many steps of this flow are on it?
- Which step has the highest chance of abandonment, and what is there to catch it?
- What does this force the user to remember, that the system could have remembered instead?
- Which decision is being asked of the user that the system could make, or defer?
- If this succeeds, what breaks next — at 10× the traffic, the data, the team?

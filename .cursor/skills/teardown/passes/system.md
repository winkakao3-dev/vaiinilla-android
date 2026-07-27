# Pass — system

For code, specs, APIs, data models, PRs, architecture. Judge whether it holds, not whether it matches your style.

## Read it as the machine

Follow the actual path of execution and data, not the file order and not the author's narration. The comment and the code disagree more often than either is wrong alone — when they do, that's a finding.

Where you can run it, run it: the test suite, the failing input, the query with 100k rows, the endpoint with a malformed body. An assertion you can execute beats a paragraph of reasoning.

## Where the defects are

**Names that lie.** A function called `validateUser` that also writes to the database. A boolean called `enabled` that means "not disabled unless overridden". A name that lies is a bug that hasn't been triggered yet, and it will be triggered by whoever reads it next.

**The unhandled branch.** Every `if` with no `else`, every `try` whose `catch` swallows, every switch missing a case, every promise with no rejection path. What happens on the branch nobody wrote?

**State that can be two things.** Data that exists in two places and can drift: cache and source, client and server, a denormalized column and its origin. Ask what reconciles them and when. "It shouldn't happen" means it will.

**Boundaries.** Where the system trusts input it shouldn't: user input, another service's response, a config file, a webhook, a date parsed from a string. Trust at a boundary is where security and corruption both enter.

**Failure semantics.** When this fails halfway, what is the state? Partial writes, non-idempotent retries, a queue with no dead letter, a transaction that isn't one. Distributed steps that assume both sides succeeded.

**Concurrency.** Two requests, same row. Read-modify-write with no lock or version. A counter incremented in application code. An assumption that this runs once.

**Scale cliffs.** The query with no index and no limit. The loop that fetches inside itself. The array held in memory that grows with the user's data. It works at 100 and dies at 100k — name the number.

**Time.** Timezone-naive storage, DST arithmetic, expiry compared against a client clock, "now" captured at the wrong moment.

**Coupling.** What has to change together but lives apart? A magic string duplicated in three files, a shape known to both ends without a shared type, an enum extended in one place only.

**The test suite.** Not whether coverage is high — whether the tests would fail if the code were wrong. Tests asserting mocks, tests asserting implementation, no test for the branch that just got added.

**Reversibility.** Can this be rolled back? Is the migration one-way? Does deploying it require the client to update at the same second?

## For specs, not code

- Which requirement is written as an adjective and will be implemented as a guess?
- Which case is unspecified — empty, error, concurrent, unauthorized — and will therefore be decided in a hurry at 4pm?
- Which two lines contradict each other? Duplicated rules that drifted apart are a spec's most common defect.
- What is the acceptance criterion, and is it observable? "Feels fast" is not.
- What does this spec forbid? A spec with no DO-NOTs hasn't been thought through.

## Not findings

Style, formatting, and structural preference are not defects unless they contradict the surrounding code — in which case the finding is the inconsistency, not the choice. Match the codebase's idiom, don't impose yours. If you would have architected it differently, that is a `preference` label and it goes last, if it goes at all.

## Then

Grade through `reference/severity.md`. Nothing is Broken without a repro you actually ran or a code path you can trace line by line.

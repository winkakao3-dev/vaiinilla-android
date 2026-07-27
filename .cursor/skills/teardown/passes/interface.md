# Pass — interface

For screens, pages, components, visual design. Run **after** flow and system — by then you know which screens matter.

If `pera-design` is installed, its `reference/antipatterns.md` is the design rubric and its `reference/craft.md` is the finish bar. Don't re-derive them here; this file is about what to look at, not what good looks like.

## Look at it

Render and screenshot at 1440×900 and 390×844. Then, in this order, because each one finds a different class of defect:

1. **Squint.** Blur it until only the big shapes remain. Where does the eye land? Is that the thing that matters? If three areas fight, hierarchy has failed and nothing below this line matters yet.
2. **Tab through it.** Every interactive element, in order. Anything unreachable, invisibly focused, or trapped is a finding. This one catch usually outnumbers everything the reviewer found by looking.
3. **Resize.** 1440 → 390 slowly. Watch where it breaks: text at 12 characters wide, a table that scrolls into nothing, a fixed element that eats the viewport, a hover-only affordance on touch.
4. **Feed it reality.** The 90-character name, the empty list, 200 rows, the missing image, the null date. Layouts hold on the author's data and collapse on the user's.
5. **Slow it down and turn it off.** Throttle the network, disable JS, set `prefers-reduced-motion`. Is the end state complete and legible, or half-painted?

## What to interrogate

**Hierarchy.** One job per screen — can you say it in a sentence, and does the layout agree? What is loudest, and does it deserve to be? Two competing primary actions is a decision the design refused to make.

**The rent question.** Run "and this — what for?" over every element. Badges, chips, dividers, icons next to labels that already say the word, cards around content that isn't interactive, a stat strip nobody reads. Decoration shaped like structure.

**States.** Every control: idle, hover, press, focus-visible, disabled, loading, error. Every list: empty, one, many, too many, loading, failed. The missing state is almost always empty or error, because they get built last and reviewed never.

**Feedback.** After every click, what tells the user it worked? Instantly, or after 8 seconds? A disabled button that never explains itself leaves the user with no move.

**Copy.** Read it as someone who doesn't work here. Jargon, labels that name the database column, errors that say "Something went wrong", buttons that say "Submit" when they mean "Pay €40". Placeholder text still living in production. Truncation that cuts mid-word.

**Legibility.** Contrast on both grounds, body text under 14px, light weights on photography, text over video with no scrim, line lengths past ~75 characters, color as the only carrier of meaning.

**Touch.** Targets under 44px, elements under the thumb-blocked zone, hover-dependent information with no tap equivalent, a fixed CTA covering the last row of a list.

**Consistency.** Two radii, three shadows, four grays, two date formats, the same action named differently on two screens. Inconsistency is a finding even when both variants are fine.

**Anonymity.** Would anyone screenshot this? Is there one thing here that no template would produce? Polished and forgettable is a real defect, and it is the one nobody reports.

## Findings that keep hiding here

- The focus ring, removed with `outline: none` and never replaced.
- The empty state, which is a centered gray sentence.
- The error state, which doesn't exist.
- The loading state on a slow connection, tested only on localhost.
- The second-to-last row of a long list, under a fixed footer.
- The page at 320px, or at 200% browser zoom.
- The print view, when the page is a document people print.

## Then

Grade through `reference/severity.md`. Be disciplined: unreachable-by-keyboard is Broken, an unstyled empty state is Wrong, a slightly heavy shadow is a Nit, and "I'd have used a serif" is a preference and goes last or nowhere.

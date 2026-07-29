# Imagination — specificity, harvested

The rest of this skill is about form: which signature, which easing, which state. None of it says where an *idea* comes from. This file does, because a page assembled correctly from the catalog can still be anonymous — and anonymous is one of the two ways Pera fails.

**Imagination is not invention. It is specificity harvested from the subject.** The memorable moments in a good page are almost never things the designer made up; they are things that were already true about the subject and that everyone else left on the floor. So the move is not "be creative" — it is *go look, then use what you find*.

This also means imagination and hallucination point in opposite directions. Inventing a fact to make a page interesting is the failure this file exists to prevent. If the inventory below comes back thin, that is a research problem — go get more material (`assets.md`) — never a licence to make things up.

## 1. Inventory the subject

Before any layout, before any effect, fill this table from the actual source material. Six rows, short answers, no adjectives. The worked example is the Chiikawa build.

| Slot | Ask | Example answer |
|------|-----|----------------|
| **Places** | Where does this happen? | a meadow, a cave, the ramen shop 「郎」 |
| **Rituals** | What do they do, over and over? | pull weeds, go out to fight, eat ramen, dance in pyjamas |
| **Voice** | What exact words or sounds does it use? | ウラ！ ヤハ！ なんとかして！ |
| **Material** | What is it physically made of? | ink outline, flat pastel fill, paper |
| **Time** | Is there a day/night, a season, a cycle? | work by day, combat at nightfall |
| **Tension** | What contradiction sits at its centre? | tiny and tender, but the work is hard and a little sad |

For a product or a company the slots are the same, only the answers change: places become surfaces and environments, rituals become the jobs users repeat, voice becomes the words the team actually says in support threads and changelogs, material becomes the substrate the work lives on, time becomes the cycle the business runs on — a close, a sprint, a season — and tension becomes the thing the product is quietly fighting.

## 2. Map each row to a page decision

The inventory is not mood-boarding. Each row has a job.

**Places → the section structure.** Name sections after the world, never after the template. `草むしり / 討伐 / ラーメン「郎」 / パジャマパーティー` instead of Hero / Features / Gallery / CTA. This single substitution does more for character than any effect, and it costs nothing.

**Rituals → the scenes and their order.** What the subject repeatedly *does* becomes what the page walks you through. A page organised around verbs feels alive; one organised around categories feels like a filing cabinet.

**Voice → real typographic material.** Actual quoted words, set at display scale, are more distinctive than any font pairing. The marquee of shouted cries was not invented — it was transcribed. Voice also belongs in the microcopy, the button labels and the alt text.

**Material → the tokens.** What the subject is made of decides the ground, the texture, the border treatment and the radius. Ink-outlined artwork earns a paper ground, a grain pass and drawn borders; machined product UI earns matte neutrals and precise hairlines. Pick the material from the subject, then let `code/tokens.css` express it.

**Time → the flagship signature.** This is the highest-leverage row. The day→night flip in that build was not chosen because theme flips are impressive; it was chosen because the subject *has a nightfall*, and it fires on the one section where night actually falls. A flagship derived from the subject's own cycle feels inevitable. The same flip dropped on a page with no night in it feels like a demo.

**Tension → the tone of copy and motion.** Tenderness plus hard work becomes figures that breathe softly while the type stays plain and unsentimental. The contradiction is what keeps a charming subject from turning saccharine.

## 3. Repurpose before you add

Before inventing a new decorative element, check whether something already in the asset set can play the part. Small character icons built for a hamburger menu became the cursor trail and the section markers; a promotional spritesheet became twelve staged actors. Reusing found material reads as authored, because everything on the page then comes from one place. Adding a new generic ornament reads as filler.

## 4. The swap test

Take any section name, signature or decoration and ask: **could I paste this onto a page about something else, and would it still fit?**

If yes, it is decoration. Decoration is allowed — most of a page is decoration — but **at least one moment per page must fail the swap test outright.** It has to be impossible anywhere else. That is the thing people screenshot.

Run it on the finished page: point at your flagship and name the fact about the subject it came from. If you can't, you picked from the catalog rather than from the world, and it will read that way.

## 5. Imagination is not extra effects

The budget in `signature.md` does not move: 2–4 details, one flagship. Imagination shows up in *which* ones you choose and *where* you fire them, never in how many. Five subject-derived effects fighting each other is the same failure as five arbitrary ones.

## The short version

1. Fill the six-row inventory from real source material. Thin inventory → go get more, don't invent.
2. Name the sections from **Places** and order them by **Rituals**.
3. Set one real quote from **Voice** at display scale.
4. Derive ground and texture from **Material**.
5. Fire the flagship signature on the moment **Time** gives you.
6. Let **Tension** set the tone of copy and motion.
7. Repurpose the assets you already have before adding new ornament.
8. Run the swap test. If nothing fails it, the page is anonymous — go back to row one.

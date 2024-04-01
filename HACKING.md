# Design Notes

I take any program I write as an excuse to experiment various design patterns and constraints.

I keep a strict separation of "technical code", located in packages called `tech`, and "business code". The technical code can be as verbose and complicated as needed, so that the business code can be as concise and readable as needed.

Some questions I'm exploring here:

* How practical can it be to write the front-end in Java, to generate Javascript, HTML and CSS? (Spoiler alert: not very) In particular, access to objects sent by the back end is enabled by a `@JS` annotation which constructs an object allowing to access fields from generated Javascript code.
* Don't-Repeat-Yourself: CSS classes are Java identifiers, so we can refer to them from the HTML in a type-safe way.
* To what extent can we ban the use of `null` in business code (both the keyword and the value)?
* Server-side-rendering: the `ItemComponent` class represents a shopping item, and a single specification can produce both HTML code sent at load-time, and Javascript code constructing an identical item (when a new item should be shown).

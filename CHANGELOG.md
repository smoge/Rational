# Changelog

## Unreleased

1. Whole `Rational` values now hash like Integer and Float.
   Approximate Float equality and fraction hashes are unchanged.

## v2.0.0

1. `floor`, `ceil`, `frac`, `trunc`, `round`, `roundUp`, `mod`, `wrap`, and
   `fold` now return `Rational` values.
2. Zero denominators, division by zero, reciprocal of zero, and terms that are
   not numbers now throw `Error`.
3. Use `.asInteger` before passing exact results to primitive APIs that need an
   integer.
4. Removed the old `Event` and `Pseq` core overwrites.

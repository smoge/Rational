# Rational

Rational Numbers extension to SuperCollider.

## Install

```supercollider
Quarks.install("https://github.com/smoge/Rational")
```

## Quick Start

```supercollider
Rational(6, 4)           // -> 3 %/ 2
Rational("8/4")          // -> 2 %/ 1
Rational(0.33333)        // -> 1 %/ 3
5 %/ 7 == Rational(5, 7) // -> true
```

Basic arithmetic stays in rational form:

```supercollider
a = Rational(3, 2);
b = 4 %/ 5;
a + b;       // -> 23 %/ 10
a - b;       // -> 7 %/ 10
a * b;       // -> 6 %/ 5
a / b;       // -> 15 %/ 8
a.pow(2);    // -> 9 %/ 4
```

Rationals compare and sort alongside other numbers:

```supercollider
(7 %/ 3) > (2 %/ 1) // -> true
[1, 1.5, 3 %/ 5, 8 %/ 5, 1.342].sort;
```

Convert when needed:

```supercollider
(5 %/ 7).asFloat   // -> 0.71428571428571
pi.asRational(999) // -> 355 %/ 113
```

Operations with exact rational answers return `Rational` values:

```supercollider
(7 %/ 2).floor       // -> 3 %/ 1
(1 %/ 3).frac        // -> 1 %/ 3
(1 %/ 3).mod(1 %/ 7) // -> 1 %/ 21
```

Other `Number` methods use `asFloat` when no rational answer exists.
`respondsTo` reports those forwarded methods:

```supercollider
(1 %/ 3).sqrt              // -> 0.57735026918963
(1 %/ 2).respondsTo(\sqrt) // -> true
```

Zero denominators, division by zero, reciprocal of zero, and terms that are not
numbers throw `Error`.

## Precision Notes

`Rational` stores numerator and denominator as 64 bit `Float`s. This avoids
SuperCollider `Integer` wrapping during rational arithmetic.

SuperCollider still wraps large integer literals before `Rational` sees them. If
you need a term outside the 32 bit `Integer` range, write it with `.0`.

Arithmetic cancels common factors before multiplying where possible. This keeps
many intermediate values smaller and preserves more precision, but it does not
make the numeric range unlimited.

## Tests

```supercollider
TestRational.run
```

The test suite covers construction, arithmetic, comparison, sorting, Float
storage, large value precision, zero denominators, `inf`, `NaN`, string parsing,
and deterministic randomized algebra.

## More Examples

See the SCDoc help in your IDE. Source: `HelpSource/Rational.schelp`.

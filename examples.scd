/*
Rational Numbers

Rational lets you write rhythms and intervals the way musicians
actually think about them: as fractions.

Instead of representing values like 1/3 or 3/8 as floating-point numbers,
Rational keeps them as numerator/denominator pairs and only converts to
floats when absolutely necessary (for scheduling or DSP).

This is especially useful for musical work:

- rhythmic notation that stays exact
- polyrhythms without drift
- just-intonation ratios
- symbolic time calculations

Installation:
Quarks.install("https://github.com/smoge/Rational")

Test:
TestRational.run
*/


/*
FUNDAMENTALS

All rationals are automatically reduced to lowest terms.
*/

// Constructor
a = Rational(3, 4);        // -> 3%/4

// Infix operator (recommended)
b = 3 %/ 4;                // -> 3%/4

// Automatic reduction
c = Rational(6, 8);        // -> 3%/4

// From strings
d = "3/4".asRational;      // -> 3%/4
"  3   %/   4  ".asRational;

// From floats (approximation)
0.75.asRational;           // -> 3%/4
0.333.asRational(100);     // -> 1%/3

// Equality uses reduced form
a == c;                    // true


/*
Arithmetic

All arithmetic is exact.
*/

(1 %/ 2) + (1 %/ 3);       // -> 5%/6
(3 %/ 4) - (1 %/ 2);       // -> 1%/4
(2 %/ 3) * (3 %/ 5);       // -> 2%/5
(1 %/ 2) / (2 %/ 3);       // -> 3%/4

// Mixed arithmetic works transparently
(1 %/ 2) + 1;              // -> 3%/2
(3 %/ 4) * 2;              // -> 3%/2
3 + (1 %/ 4);              // -> 13%/4


/*
Comparison / Ord
*/

(1 %/ 2) < (2 %/ 3);       // true
(3 %/ 4) > (1 %/ 2);       // true
(2 %/ 4) == (1 %/ 2);      // true
(1 %/ 3) != (1 %/ 2);      // true

[3%/4, 1%/2, 2%/3, 1%/4].sort;


/*
Conversions
*/

(3 %/ 4).asFloat;          // 0.75
(7 %/ 4).asInteger;        // 1

pi.asRational(10);         // 22%/7
pi.asRational(1000);       // 355%/113

(355 %/ 113).simplify(10); // 22%/7


/*
Unary operations
*/

(3 %/ 4).neg;
(3 %/ 4).reciprocal;
(-3 %/ 4).abs;
(2 %/ 3).squared;
(2 %/ 3).cubed;


/*
Exponentiation
*/

(2 %/ 3).pow(3);           // 8%/27
(2 %/ 3).pow(-2);          // 9%/4
(3 %/ 4).pow(0);           // 1%/1


/*
Accessors and predicates
*/

r = 3 %/ 4;
r.numerator;
r.denominator;

r.sign;
(-3 %/ 4).sign;

r.isRational;
r.isPositive;

(4 %/ 3).isNumeratorPowerOfTwo;
(3 %/ 8).isDenominatorPowerOfTwo;


/*
Edge cases
*/

Rational(1, 0);             // nil
Rational(inf, 1);           // inf
Rational(1, inf);           // 0%/1
(1 %/ 2) / (0 %/ 1);        // nil


/*
Collections
*/

(1..8).collect { |n| 1 %/ n };
[1%/2, 1%/3, 1%/6].sum;

[1%/2, 2%/3, 3%/4, 4%/5].plot;


/*
Patterns
*/

(
Pbind(
    \degree, Pseq([0, 2, 4, 5], inf),
    \dur,    Pseq([1%/4, 1%/8, 1%/8, 1%/2], inf)
).play;
)


(
var beats = 4;

Ppar([
    Pbind(\degree, 0, \dur, beats %/ 3),
    Pbind(\degree, 4, \dur, beats %/ 5)
]).play;
)


/*
Pitch
*/

(
var fifth  = 3 %/ 2;
var fourth = 4 %/ 3;

fifth * fourth;        // octave
fifth.reciprocal;
)


/*
Rhythm
*/

(
var q = 1 %/ 4;
var e = 1 %/ 8;

(q + e) == (3 %/ 8);
)


(
var pattern = [2%/8, 2%/8, 3%/8];
pattern.sum;

Pbind(
    \degree, Pseq([0, 2, 4], inf),
    \dur,    Pseq(pattern, inf)
).play;
)

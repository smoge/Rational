/*
Rational Numbers for SuperCollider

Installation:
Quarks.install("https://github.com/smoge/Rational")

Test:
TestRational.run
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

[1%/2, 2%/3, 3%/4, 4%/5].plot;

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

fifth * fourth;        // -> octave
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


///////////////////////////////////////////////////

(
s.waitForBoot {

	var pitchRatios = (
		unison: 1%/1, major2: 9%/8, minor3: 6%/5, major3: 5%/4,
		fourth: 4%/3, fifth: 3%/2, major6: 5%/3,
		minor7: 9%/5, octave: 2%/1
	);

	var ratios = pitchRatios.values;
	var root = 207.5;
	var ratioToFreq = { |r| root * r.asFloat };

	SynthDef(\ping, { |out=0, freq=440, amp=0.1, pan=0|
		var env, sig, mod, body;

		env = EnvGen.kr(
			Env.perc(0.004, 0.35, curve: -5),
			doneAction: 2
		);

		mod = SinOsc.kr(5, Rand(0, 2pi)).range(0.997, 1.003);
		sig = SinOsc.ar(freq * mod);
		body = SinOsc.ar(freq * 2.01, 0, 0.2);
		sig = sig + body;
		sig = LPF.ar(sig, freq * 3.5);
		sig = sig * env * amp;

		sig = Splay.ar(sig, 0.15);
		Out.ar(out, Balance2.ar(sig[0], sig[1], pan));
	}).add;

	SynthDef(\bell, { |out=0, freq=440, amp=0.1, pan=0, decay=3|
		var env, exc, sig, modes;

		env = EnvGen.kr(
			Env.perc(0.01, decay, curve: -6),
			doneAction: 2
		);

		exc = PinkNoise.ar(0.12)
		* EnvGen.kr(Env.perc(0.002, 0.04));

		modes = [1, 2.02, 3.01, 4.12, 5.3];

		sig = Mix.fill(modes.size, { |i|
			Ringz.ar(
				exc,
				freq * modes[i],
				decay * (0.9 - (i * 0.12))
			)
		});

		sig = LPF.ar(sig, 8000);
		sig = sig * env * amp;

		Out.ar(out, Pan2.ar(sig, pan));
	}).add;

	s.sync;

	~voiceOne = Pbind(
		\instrument, \ping,
		\dur, Prand((1..13).collect { |n| n %/ 51 }, inf),
		\freq, Pseq([
			ratioToFreq.(pitchRatios.fifth),
			Pfunc { ratioToFreq.(ratios.choose) },
			ratioToFreq.(pitchRatios.octave)
		], inf) * Prand([0.5, 1, 2], inf),
		\amp, Pwhite(0.01, 0.1),
		\pan, Pwhite(0.4, 0.7)
	);

	~voiceTwo = Pbind(
		\instrument, \bell,
		\dur, Pfunc { rrand(5, 11) %/ 37 },
		\freq, Pfunc { ratioToFreq.(ratios.choose) },
		\amp, Pwhite(0.01, 0.07),
		\pan, Pwhite(0.1, 0.9)
	);

	~thang = Ppar([~voiceOne, ~voiceTwo]).play;
};
)

Rational : Number {
	var <numerator, <denominator;


	// Note [Precision]
	// ~~~~~~~~~~~~~~~~
	//
	// Numerator and denominator are always Float, never Integer.
	//
	// An sclang Integer is 32 bits and wraps past 2147483647 with no warning. A
	// Float is an IEEE double and holds every integer exactly up to 2^53.
	//
	// Nothing type-checks that. fromReducedTerms coerces with .asFloat, every
	// other path leans on reduce dividing both terms by gcd, since sclang's
	// Integer / Integer returns a Float even when gcd is 1. Let reduce skip that
	// division (fast path for gcd 1) and Integer terms slip through.
	// test_Components_StoredAsFloat guards this.
	//
	// When writing large values: sclang wraps integer literals while parsing,
	// before Rational ever sees them.
	//
	//   Rational(3000000000, 1)      // reads as Rational(-1294967296, 1)
	//   Rational(3000000000.0, 1.0)  // the ".0" makes it a Float
	//
	// See Note [Cross-reduction] for the arithmetic precision rule.
	// See Note [Rational bounds] in Tests/TestRational.sc for test limits.


	// Note [Cross-reduction]
	// ~~~~~~~~~~~~~~~~~~~~~~
	//
	// Rational arithmetic cancels common factors before it multiplies.
	//
	// For * and / factors are cancelled across opposite terms:
	//
	//   (a/b) * (c/d)  cancels a with d, and c with b
	//   (a/b) / (c/d)  cancels a with c, and b with d
	//
	// For + and -, common denominator factors are cancelled before the two
	// cross-products are built.
	//
	// For * and / this also keeps the result canonical: if a/b and c/d are both in
	// lowest terms, cancelling gcd(a,d) and gcd(c,b) leaves the product in lowest
	// terms. That's why * and / may call fromReducedTerms and skip reduce, and it
	// is what == (which compares terms directly) and test_NormalizedForm depend
	// on.
	//
	// This keeps intermediate values smaller when operands share factors, and it
	// avoids some loss of precision. Of course, it doesn't make the range
	// unlimited. With nothing to cancel and all terms bounded by M, a single * or
	// / builds intermediates near M^2, and a single + or - near 2 * M^2, since it
	// sums two cross-products. Chaining has no ceiling: k operands chained with +
	// or - reach k * M^k, with * or / they reach M^k. See Note [Rational bounds]
	// in Tests/TestRational.sc.


	*new { arg numerator=1.0, denominator=1.0;
		if (numerator.isKindOf(String)) { ^numerator.asRational };
		this.prCheckTerm(numerator, "numerator");
		this.prCheckTerm(denominator, "denominator");
		if (numerator.isNaN || denominator.isNaN) { ^0/0 };
		if (denominator == 0) { this.prRefuseZeroDenominator };
		if (numerator == inf) { ^inf };
		if (numerator == -inf) { ^-inf };
		if (denominator == inf) { ^Rational(0, 1) };
		if (denominator == -inf) { ^Rational(0, 1) };
		if (numerator.isKindOf(Rational) or: denominator.isKindOf(Rational)) {
			numerator = numerator.asRational;
			denominator = denominator.asRational;
			^(numerator / denominator)
		};
		if (numerator.frac != 0 or: denominator.frac != 0) {
			^(numerator/denominator).asRational
		};
		^super.newCopyArgs(numerator, denominator).reduce
	}

	// Fast constructor for normalized terms. Use this only when the caller already
	// knows the terms are reduced.
	*fromReducedTerms { arg numerator=1.0, denominator=1.0;
		if (denominator == 0) { this.prRefuseZeroDenominator };
		if (denominator < 0) {
			numerator = numerator.neg;
			denominator = denominator.neg;
		};
		^super.newCopyArgs(numerator.asFloat, denominator.asFloat);
	}

	// Fast constructor for internal arithmetic. It reduces and normalizes sign,
	// while skipping Rational.new's parsing, infinity, NaN, and fractional checks.
	*fromTerms { arg numerator=1.0, denominator=1.0;
		if (denominator == 0) { this.prRefuseZeroDenominator };
		^super.newCopyArgs(numerator, denominator).reduce
	}

	*newFrom { arg that; ^that.asRational }

	// Note [Refusing rather than answering nil]
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	//
	// A zero denominator, a division by zero and the reciprocal of zero used to
	// warn and answer nil. nil is a value, so it travels: it reaches a caller
	// that never asked, and the warning is long gone by the time anything goes
	// wrong. A downstream quark spelled `MusicPitch(\\c, 1%/0)` as a C natural
	// for exactly this reason - nil arrived where an alteration was expected and
	// was read as "none given".
	//
	// These throw now. It is a breaking change against a published quark, and
	// the tests that asserted nil were updated with it.

	*prRefuseZeroDenominator {
		Error("Rational: a denominator of zero has no value. Nothing in the "
			"rationals divides by zero, so this is refused where it was "
			"written rather than answered with nil.").throw
	}

	// A term must be a number. Without this the first thing a non-number meets
	// is `isNaN` below, and the error names that rather than the argument.
	*prCheckTerm { arg value, what;
		if (value.isNumber) { ^value };
		Error("Rational: % is not a number, so it cannot be a %.".format(
			value.asCompileString, what)).throw
	}

	*gcd { arg a, b;
		a = a.abs;
		b = b.abs;
		while { b != 0 } { b = a mod: (a = b) };
		^a
	}

	reduce {
		var d;
		if (numerator.frac == 0 and: denominator.frac == 0) {
			d = this.class.gcd(numerator, denominator);
			numerator = numerator / d;
			denominator = denominator / d;
			if (denominator < 0) {
				numerator = numerator.neg;
				denominator = denominator.neg;
			}
		};
		^this
	}

	factor {
		var d = this.class.gcd(numerator, denominator).abs;
		if (denominator < 0) { d = d.neg };
		if (numerator < 0) { d = d.neg };
		^d.asFloat
	}

	sign {
		if (numerator == 0) { ^0 };
		if (numerator > 0) { ^1 };
		if (numerator < 0) { ^(-1) };
	}

	numerator_ { arg newNumerator=1.0;
		numerator = newNumerator;
		if (numerator.isNaN || denominator.isNaN) { ^0/0 };
		if (numerator.frac != 0) { ^(numerator/denominator).asRational };
		if (numerator == inf) { ^inf };
		if (numerator == -inf) { ^-inf };
		^this.reduce
	}

	denominator_ { arg newDenominator=1.0;
		// Checked before the assignment, so a refusal leaves the receiver intact.
		if (newDenominator == 0) { this.class.prRefuseZeroDenominator };
		denominator = newDenominator;
		if (denominator.isNaN) { ^0/0 };
		if (denominator.frac != 0) { ^(numerator/denominator).asRational };
		if (denominator == inf) { ^this.class.new(0, 1) };
		if (denominator == -inf) { ^0 };
		^this.reduce
	}

	isRational { ^true }

	// Note [Exact where it can be]
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	//
	// doesNotUnderstand answers any selector this class lacks from this.asFloat.
	// For most of the Number vocabulary that is right: a rational has no
	// rational square root, so sqrt, sin, log and midicps give Floats and should.
	//
	// It was also answering the operations that DO have exact rational results.
	// floor, ceil, round, roundUp, trunc, frac, mod, wrap and fold are closed
	// over the rationals, and each returned a Float:
	//
	//   (7%/2).floor  ->  3.0                where 3%/1 is exact
	//   (1%/3).frac   ->  0.33333333333333   where 1%/3 is exact
	//
	// Wrong rather than lossy, and silent. They are defined below, so the
	// fallback no longer sees them; test_ExactOperationsAgreeWithFloat pins each
	// against the Float it stands in for, since matching sclang matters more
	// here than matching any textbook. sclang quantizes downward rather than
	// toward zero - (-3.5).trunc is -4.0 - and its frac is never negative.
	//
	// respondsTo is overridden to agree with the fallback. Left alone, a
	// Rational answers sqrt while reporting that it cannot, which defeats every
	// caller that asks before sending.
	doesNotUnderstand { |selector ...args|
		var float = this.asFloat;
		if (float.respondsTo(selector)) {
			^float.performList(selector, args)
		} {
			^super.doesNotUnderstand(selector, *args)
		}
	}

	respondsTo { arg selector;
		^super.respondsTo(selector) or: { this.asFloat.respondsTo(selector) }
	}

	isZero { ^numerator == 0 }

	// Object:do runs its function once; SimpleNumber:do counts. floor answers a
	// Rational now, so `x.floor.do { }` would silently run once where it ran
	// three times before - a quieter wrong than the Float it replaced. Both
	// follow sclang's counts exactly, quirks included: 3.5.do yields three
	// values and 3.5.reverseDo yields four. do yields the counter as an Integer,
	// which is what a loop index is for; reverseDo counts down from this - 1, so
	// its values are rational whenever the receiver is.
	do { arg function;
		var i = 0, limit = this.asInteger;
		while { i < limit } { function.value(i, i); i = i + 1 }
	}

	reverseDo { arg function;
		var i = 0, j = this - 1;
		while { i < this } { function.value(j, i); j = j - 1; i = i + 1 }
	}

	// reduce leaves the denominator positive, so the quotient needs no sign
	// correction and floor is floor of the terms.
	floor { ^this.class.fromReducedTerms((numerator / denominator).floor, 1.0) }
	ceil  { ^this.class.fromReducedTerms((numerator / denominator).ceil, 1.0) }

	// The part above the floor, so never negative: (-7%/2).frac is 1%/2.
	frac { ^this - this.floor }

	trunc   { arg quantum = 1; ^(this / quantum).floor * quantum }
	roundUp { arg quantum = 1; ^(this / quantum).ceil * quantum }
	round   { arg quantum = 1;
		^((this / quantum) + this.class.fromReducedTerms(1.0, 2.0)).floor * quantum
	}

	mod { arg aNumber; ^this - (aNumber * (this / aNumber).floor) }

	wrap { arg lo, hi;
		var range;
		lo = lo.asRational; hi = hi.asRational;
		range = hi - lo;
		if (range.isZero) { ^lo };
		^lo + (this - lo).mod(range)
	}

	fold { arg lo, hi;
		var range, range2, x, c;
		lo = lo.asRational; hi = hi.asRational;
		range = hi - lo;
		if (range.isZero) { ^lo };
		range2 = range + range;
		x = this - lo;
		c = x - (range2 * (x / range2).floor);
		if (c >= range) { c = range2 - c };
		^c + lo
	}

	isNaN { ^numerator.isNaN or: { denominator.isNaN }}

	isNegative { ^numerator.isNegative }
	isPositive { ^numerator.isPositive }
	isNumeratorPowerOfTwo { ^numerator.asInteger.isPowerOfTwo }
	isDenominatorPowerOfTwo { ^denominator.asInteger.isPowerOfTwo }

	asRational { ^this }
	asFloat { ^(numerator / denominator).asFloat }
	asInteger { ^(numerator / denominator).asInteger }
	asInt { ^this.asInteger }

	%/ { arg aNumber; ^this.class.new(this, aNumber) }


	+ { arg aNumber, adverb;
		var g, n, d;
		aNumber = aNumber.asRational;
		if (this.denominator == 1 and: { aNumber.denominator == 1 }) {
			^this.class.fromReducedTerms(this.numerator + aNumber.numerator, 1.0)
		};
		if (this.denominator == aNumber.denominator) {
			^this.class.fromTerms(this.numerator + aNumber.numerator, this.denominator)
		};
		g = this.class.gcd(this.denominator, aNumber.denominator);
		n = (this.numerator * (aNumber.denominator / g)) +
		(aNumber.numerator * (this.denominator / g));
		d = (this.denominator / g) * aNumber.denominator;
		^this.class.fromTerms(n, d)
	}

	- { arg aNumber, adverb;
		var g, n, d;
		aNumber = aNumber.asRational;
		if (this.denominator == 1 and: { aNumber.denominator == 1 }) {
			^this.class.fromReducedTerms(this.numerator - aNumber.numerator, 1.0)
		};
		if (this.denominator == aNumber.denominator) {
			^this.class.fromTerms(this.numerator - aNumber.numerator, this.denominator)
		};
		g = this.class.gcd(this.denominator, aNumber.denominator);
		n = (this.numerator * (aNumber.denominator / g)) -
		(aNumber.numerator * (this.denominator / g));
		d = (this.denominator / g) * aNumber.denominator;
		^this.class.fromTerms(n, d)
	}

	* { arg aNumber, adverb;
		var g1, g2, n, d;
		aNumber = aNumber.asRational;
		g1 = this.class.gcd(this.numerator.abs, aNumber.denominator.abs);
		g2 = this.class.gcd(aNumber.numerator.abs, this.denominator.abs);
		n = (this.numerator / g1) * (aNumber.numerator / g2);
		d = (this.denominator / g2) * (aNumber.denominator / g1);
		^this.class.fromReducedTerms(n, d)
	}

	/ { arg aNumber, adverb;
		var g1, g2, n, d;
		aNumber = aNumber.asRational;
		if (aNumber.numerator == 0) { Error("Rational: division by zero.").throw };
		g1 = this.class.gcd(this.numerator.abs, aNumber.numerator.abs);
		g2 = this.class.gcd(this.denominator.abs, aNumber.denominator.abs);
		n = (this.numerator / g1) * (aNumber.denominator / g2);
		d = (this.denominator / g2) * (aNumber.numerator / g1);
		^this.class.fromReducedTerms(n, d)
	}

	== { arg aNumber, adverb;
		aNumber = aNumber.asRational;
		^(this.numerator == aNumber.numerator) and: { this.denominator == aNumber.denominator }
	}

	!= { arg aNumber, adverb;
		aNumber = aNumber.asRational;
		^(this.numerator != aNumber.numerator) or: { this.denominator != aNumber.denominator }
	}

	compareValue { arg aNumber;
		var g, lhs, rhs;
		aNumber = aNumber.asRational;
		if (this.denominator == aNumber.denominator) {
			^(this.numerator - aNumber.numerator).sign
		};
		g = this.class.gcd(this.denominator, aNumber.denominator);
		lhs = this.numerator * (aNumber.denominator / g);
		rhs = aNumber.numerator * (this.denominator / g);
		^(lhs - rhs).sign
	}

	< { arg aNumber; ^this.compareValue(aNumber) < 0 }
	> { arg aNumber; ^this.compareValue(aNumber) > 0 }
	<= { arg aNumber; ^this.compareValue(aNumber) <= 0 }
	>= { arg aNumber; ^this.compareValue(aNumber) >= 0 }

	reciprocal {
		if (numerator == 0) { Error("Rational: zero has no reciprocal.").throw };
		^this.class.fromReducedTerms(denominator, numerator)
	}

	neg { ^this.class.fromReducedTerms(numerator.neg, denominator) }
	abs { ^this.class.fromReducedTerms(numerator.abs, denominator) }
	squared { ^this.pow(2) }
	cubed { ^this.pow(3) }

	pow { arg n;
		var result, base, exponent;

		if (n.isKindOf(Rational)) { n = n.asFloat };
		if (n.frac != 0) { ^this.asFloat.pow(n) };

		exponent = n.asInteger;

		^case
		{ exponent == 0 } { this.class.fromReducedTerms(1.0, 1.0) }
		{ exponent > 0 } {
			result = this.class.fromReducedTerms(1.0, 1.0);
			base = this;
			while { exponent > 0 } {
				if (exponent.odd) { result = result * base };
				base = base * base;
				exponent = exponent >> 1;
			};
			result
		}
		{ exponent < 0 } {
			if (numerator == 0) { "Zero to negative power undefined".error; nil }
			{ this.reciprocal.pow(exponent.abs) }
		}
	}

	simplify { arg maxDenominator=20, fasterBetter=false;
		var frac = (numerator / denominator).asFraction(maxDenominator, fasterBetter);
		^Rational(frac[0], frac[1])
	}

	performBinaryOpOnSimpleNumber { arg aSelector, aNumber, adverb;
		^aNumber.asRational.perform(aSelector, this, adverb)
	}

	hash { ^this.instVarHash }

	printOn { arg stream;
		stream << numerator.asString.replace(".0", "") << "%/" << denominator.asString.replace(".0", "")
	}

	storeOn { arg stream;
		stream << numerator.asString.replace(".0", "") << "%/" << denominator.asString.replace(".0", "")
	}
}

+ SimpleNumber {
	asRational { arg maxDenominator=100, fasterBetter=false;
		var fraction;
		if (this.abs == inf) { ^this };
		fraction = this.asFraction(maxDenominator, fasterBetter);
		^Rational(fraction[0], fraction[1])
	}

	%/ { arg aNumber; ^Rational(this, aNumber) }

	performBinaryOpOnRational { arg aSelector, rational, adverb;
		^rational.perform(aSelector, this.asRational, adverb)
	}
}

+ Integer {
	asRational { ^Rational.fromReducedTerms(this, 1) }
}

+ Number {
	numerator { ^this }
	denominator { ^1 }
	rational { arg denominator=1; ^Rational(this, denominator) }
}

+ SequenceableCollection {
	asRational { arg maxDenominator=100;
		^this.collect { |item| item.asRational(maxDenominator) }
	}
}

+ String {
	asRational {
		var parts = this.replace("%", "").split($/).collect(_.stripWhiteSpace);
		^Rational(parts[0].asFloat, parts[1].asFloat)
	}
}

+ AbstractFunction {
	performBinaryOpOnRational { arg aSelector, aRational, adverb;
		^this.reverseComposeBinaryOp(aSelector, aRational, adverb)
	}
}

+ Object {
	isRational { ^false }
	performBinaryOpOnRational { arg aSelector, thing, adverb;
		^this.performBinaryOpOnSomething(aSelector, thing, adverb)
	}
}

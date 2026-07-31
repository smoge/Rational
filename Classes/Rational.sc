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
		if (numerator.isNaN || denominator.isNaN) { ^0/0 };
		if (denominator == 0) { "Rational has zero denominator".warn; ^nil };
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
		if (denominator == 0) { "Rational has zero denominator".error; ^nil };
		if (denominator < 0) {
			numerator = numerator.neg;
			denominator = denominator.neg;
		};
		^super.newCopyArgs(numerator.asFloat, denominator.asFloat);
	}

	// Fast constructor for internal arithmetic. It reduces and normalizes sign,
	// while skipping Rational.new's parsing, infinity, NaN, and fractional checks.
	*fromTerms { arg numerator=1.0, denominator=1.0;
		if (denominator == 0) { "Rational has zero denominator".error; ^nil };
		^super.newCopyArgs(numerator, denominator).reduce
	}

	*newFrom { arg that; ^that.asRational }

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
		denominator = newDenominator;
		if (denominator.isNaN) { ^0/0 };
		if (denominator == 0) { "Rational has zero denominator".error; ^nil };
		if (denominator.frac != 0) { ^(numerator/denominator).asRational };
		if (denominator == inf) { ^this.class.new(0, 1) };
		if (denominator == -inf) { ^0 };
		^this.reduce
	}

	isRational { ^true }

	doesNotUnderstand { |selector ...args|
		var float = this.asFloat;
		if (float.respondsTo(selector)) {
			^float.performList(selector, args)
		} {
			^super.doesNotUnderstand(selector, *args)
		}
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
		if (aNumber.numerator == 0) { "Division by zero".error; ^nil };
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
		if (numerator == 0) { "Reciprocal of zero".error; ^nil };
		^this.class.fromReducedTerms(denominator, numerator)
	}

	neg { ^this.class.fromReducedTerms(numerator.neg, denominator) }
	abs { ^this.class.fromReducedTerms(numerator.abs, denominator) }
	squared { ^this.pow(2) }
	cubed { ^this.pow(3) }

	pow { arg n;
		var result, base;
		^case
		{ n == 0 } { this.class.fromReducedTerms(1.0, 1.0) }
		{ n > 0 } {
			result = this.class.fromReducedTerms(1.0, 1.0);
			base = this;
			n = n.asInteger;
			while { n > 0 } {
				if (n.odd) { result = result * base };
				base = base * base;
				n = n >> 1;
			};
			result
		}
		{ n < 0 } {
			if (numerator == 0) { "Zero to negative power undefined".error; nil }
			{ this.reciprocal.pow(n.abs) }
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

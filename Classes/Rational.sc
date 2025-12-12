Rational : Number {
	var <numerator, <denominator;

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

	*fromReducedTerms { arg numerator=1.0, denominator=1.0;
		if (denominator == 0) { "Rational has zero denominator".error; ^nil };
		if (denominator < 0) {
			numerator = numerator.neg;
			denominator = denominator.neg;
		};
		^super.newCopyArgs(numerator, denominator);
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
			numerator = numerator div: d;
			denominator = denominator div: d;
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
		g = this.class.gcd(this.denominator, aNumber.denominator);
		n = (this.numerator * (aNumber.denominator div: g)) +
		(aNumber.numerator * (this.denominator div: g));
		d = (this.denominator div: g) * aNumber.denominator;
		^this.class.new(n, d)
	}

	- { arg aNumber, adverb;
		var g, n, d;
		aNumber = aNumber.asRational;
		g = this.class.gcd(this.denominator, aNumber.denominator);
		n = (this.numerator * (aNumber.denominator div: g)) -
		(aNumber.numerator * (this.denominator div: g));
		d = (this.denominator div: g) * aNumber.denominator;
		^this.class.new(n, d)
	}

	* { arg aNumber, adverb;
		var g1, g2, n, d;
		aNumber = aNumber.asRational;
		g1 = this.class.gcd(this.numerator.abs, aNumber.denominator.abs);
		g2 = this.class.gcd(aNumber.numerator.abs, this.denominator.abs);
		n = (this.numerator div: g1) * (aNumber.numerator div: g2);
		d = (this.denominator div: g2) * (aNumber.denominator div: g1);
		^this.class.new(n, d)
	}

	/ { arg aNumber, adverb;
		var g1, g2, n, d;
		aNumber = aNumber.asRational;
		if (aNumber.numerator == 0) { "Division by zero".error; ^nil };
		g1 = this.class.gcd(this.numerator.abs, aNumber.numerator.abs);
		g2 = this.class.gcd(this.denominator.abs, aNumber.denominator.abs);
		n = (this.numerator div: g1) * (aNumber.denominator div: g2);
		d = (this.denominator div: g2) * (aNumber.numerator div: g1);
		^this.class.new(n, d)
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
		g = this.class.gcd(this.denominator, aNumber.denominator);
		lhs = this.numerator * (aNumber.denominator div: g);
		rhs = aNumber.numerator * (this.denominator div: g);
		^(lhs - rhs).sign
	}

	< { arg aNumber; ^this.compareValue(aNumber) < 0 }
	> { arg aNumber; ^this.compareValue(aNumber) > 0 }
	<= { arg aNumber; ^this.compareValue(aNumber) <= 0 }
	>= { arg aNumber; ^this.compareValue(aNumber) >= 0 }

	reciprocal {
		if (numerator == 0) { "Reciprocal of zero".error; ^nil };
		^this.class.new(denominator, numerator)
	}

	neg { ^this.class.new(numerator.neg, denominator) }
	abs { ^this.class.new(numerator.abs, denominator) }
	squared { ^this.pow(2) }
	cubed { ^this.pow(3) }

	pow { arg n;
		var result, base;
		^case
		{ n == 0 } { this.class.new(1, 1) }
		{ n > 0 } {
			result = this.class.new(1, 1);
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
	asRational { ^Rational(this, 1) }
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
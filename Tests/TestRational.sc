/*
TestRational.run
*/

TestRational : UnitTest {

	// Note [Rational bounds]
	// ~~~~~~~~~~~~~~~~~~~~~~
	//
	// Components are Float, integers are exact up to 2^53 = 9007199254740992.
	// See Note [Precision] in Classes/Rational.sc
	//
	// The arithmetic under test builds intermediates far larger than the
	// inputs: every + - * / cross-multiplies numerators and denominators, and
	// the sort and comparison tests cross-multiply a second time inside
	// compareValue. assertEquals does not add to this, since == compares terms
	// directly (see Note [Cross-reduction]). Once an intermediate passes 2^53
	// sclang rounds it, and the test then fails for a reason that has nothing
	// to do with Rational. Each bound below is set by the largest intermediate
	// the tests using it can produce.
	//
	// The bounds are conservative: they assume no common factors cancel.
	// That keeps the tests valid even when cross-reduction cannot help.
	//
	//   maxIntVal        94906265  one product, M^2 <= 2^53
	//                              compare, sort, multiply, divide
	//
	//   maxAddSubIntVal  67108864  sum of two products, 2 * M^2 <= 2^53
	//                              a single + or -  (exactly 2^26)
	//
	//   maxAssocIntVal     144263  two adds over three operands, 3 * M^3 <= 2^53
	//                              (the k * M^k of Note [Cross-reduction]; k
	//                              counts operands, not operations). Also used
	//                              for (a*b)*c, which needs only M^3 <= 2^53 and
	//                              would allow 208063.
	//
	// Each bound is the largest integer satisfying its inequality; the next one
	// up overflows 2^53.
	//
	// compare sits under maxIntVal even though lhs - rhs can reach 2 * M^2 and
	// round. Only the sign of the difference is used, and IEEE rounding never
	// flips a sign or turns a non-zero difference into zero.
	//
	// Keep these as Integer literals. A Float bound makes maxIntVal.rand return
	// fractional values, which sends Rational down its asRational path instead
	// of the exact integer one. Values above 2^31 - 1 have to be written with a
	// ".0" instead, as explained in Note [Precision].


	var <>minIntVal = -94906265, <>maxIntVal = 94906265;
	var <>minAddSubIntVal = -67108864, <>maxAddSubIntVal = 67108864;
	var <>minAssocIntVal = -144263, <>maxAssocIntVal = 144263;
	var <>minFloatVal = -9007199254740992.0, <>maxFloatVal = 9007199254740992.0;
	var <>numTests = 50;
	var <>seed = 147;
	var <>isVerbose = true;

	setUp { thisThread.randSeed = seed }

	test_Setters {
		var rat = 1 %/ 2;
		rat.numerator_(3);
		rat.denominator_(3);
		this.assert(rat == Rational(1, 1), "Setter mutation test passed.");
	}

	test_Setters_Return {
		var rat, result;
		rat = 1 %/ 2;
		result = rat.numerator_(4);
		this.assert(
			result.isKindOf(Rational) and: { result == Rational(2, 1) },
			"numerator_ returns reduced Rational"
		);
		rat = 1 %/ 2;
		result = rat.denominator_(4);
		this.assert(
			result.isKindOf(Rational) and: { result == Rational(1, 4) },
			"denominator_ returns reduced Rational"
		);
	}

	test_Setters_KeepFloatStorage {
		var rat, result;

		rat = 1 %/ 2;
		result = rat.numerator_(3000000000.0);
		this.assert(
			result.numerator.isKindOf(Float) and: { result.denominator.isKindOf(Float) },
			"numerator_ keeps Float storage"
		);
		this.assertEquals(result.numerator, 1500000000.0, "numerator_ preserves large value", isVerbose);
		this.assertEquals(result.denominator, 1.0, "numerator_ preserves reduced denominator", isVerbose);

		rat = 1 %/ 2;
		result = rat.denominator_(3000000000.0);
		this.assert(
			result.numerator.isKindOf(Float) and: { result.denominator.isKindOf(Float) },
			"denominator_ keeps Float storage"
		);
		this.assertEquals(result.numerator, 1.0, "denominator_ preserves reduced numerator", isVerbose);
		this.assertEquals(result.denominator, 3000000000.0, "denominator_ preserves large value", isVerbose);
	}

	test_Reduce_Returns_This {
		var rat = Rational(4, 8);
		this.assert(
			rat.isKindOf(Rational) and: { rat == Rational(1, 2) },
			"reduce returns this after reduction"
		);
	}

	test_ZeroDenominator {
		var r1, r2, divResult, recipResult;

		[1, -1, 0, 100, -100, 0.5, pi].do { |num|
			var rat = Rational(num, 0);
			this.assert(rat.isNil, format("Rational(%, 0) returns nil", num));
		};

		r1 = Rational(1, 2);
		r2 = Rational(0, 1);
		divResult = r1 / r2;
		this.assert(divResult.isNil, "Division by zero rational returns nil");

		recipResult = Rational(0, 1).reciprocal;
		this.assert(recipResult.isNil, "Reciprocal of zero returns nil");
	}

	test_fromReducedTerms_Validation {
		var rat;

		rat = Rational.fromReducedTerms(1, 0);
		this.assert(rat.isNil, "fromReducedTerms(1,0) returns nil");

		rat = Rational.fromReducedTerms(1, -2);
		this.assert(
			rat.denominator > 0 and: { rat == Rational(1, -2) },
			"fromReducedTerms normalizes negative denominator"
		);

		rat = Rational.fromTerms(2, 4);
		this.assert(
			rat == Rational(1, 2) and: { rat.numerator.isKindOf(Float) and: { rat.denominator.isKindOf(Float) } },
			"fromTerms reduces and stores Float components"
		);

		rat = Rational.fromTerms(1, 0);
		this.assert(rat.isNil, "fromTerms(1,0) returns nil");
	}

	test_NegativeDenominator_Normalization {
		this.assertEquals(Rational(1, -2), Rational(-1, 2), "Rational(1,-2) normalizes sign", isVerbose);
		this.assertEquals("1/-2".asRational, Rational(-1, 2), "String input normalizes negative denominator", isVerbose);
		this.assertEquals(
			Rational(1, 2) * Rational(1, -1),
			Rational(-1, 2),
			"Operation-created negative denominator normalizes sign",
			isVerbose
		);
	}

	test_Comparison_SignNormalized {
		var a = Rational.fromReducedTerms(1, 2);
		var b = Rational.fromReducedTerms(-1, 2);
		this.assert(b < a, "Comparison works correctly with normalized signs");
	}

	test_inf_nan {
		this.assert(Rational(1, 1) + inf == inf, "Rational(1,1) + inf == inf");
		this.assert(Rational(inf, 1) == inf, "Rational(inf,1) == inf");
		this.assert(Rational(-inf, 1) == -inf, "Rational(-inf,1) == -inf");
		this.assert(Rational(inf, rrand(-1000, 1000)) == inf, "Rational(inf, n) == inf");
		this.assert(Rational(-inf, rrand(-1000, 1000)) == -inf, "Rational(-inf, n) == -inf");
		this.assert(Rational(0, inf) == Rational(0, 1), "Rational(0,inf) == Rational(0,1)");
		this.assert(Rational(rrand(-1000, 1000), inf) == Rational(0, 1), "Rational(n,inf) == Rational(0,1)");
		this.assert(Rational(1, 0/0).isNaN, "Rational(1,0/0).isNaN");
		this.assert(Rational(0/0, 1).isNaN, "Rational(0/0,1).isNaN");
		this.assert(Rational(1, 1).numerator_(inf) === inf, "numerator_(inf) === inf");
		this.assert(Rational(1, 1).denominator_(inf) == Rational(0, 1), "denominator_(inf) == Rational(0,1)");
		this.assert(Rational(1, 1).numerator_(0/0).isNaN, "numerator_(0/0).isNaN");
		this.assert(Rational(1, 1).denominator_(0/0).isNaN, "denominator_(0/0).isNaN");
	}

	test_BigNumbers {
		this.assert((2147483646 %/ 1) + 1 == (2147483647 %/ 1), "Big int addition near max");
		this.assert((2147483647.0 %/ 1) + 1 == (2147483648.0 %/ 1), "Big float addition past max");
		this.assert(Rational(2147483647.0, 1) * 2 == (4294967294.0 %/ 1), "Big float multiplication");
		this.assert((-2147483646 %/ 1) - 1 == (-2147483647 %/ 1), "Big negative subtraction");
	}

	// See Note [Rational bounds]. This test protects the storage choice: every
	// public way of making or changing a Rational should leave both parts as
	// Float.
	test_Components_StoredAsFloat {
		var cases = [
			Rational(1, 3),
			Rational(1.0, 3.0),
			Rational(4, 8),
			Rational(-2, 6),
			Rational(5, 1),
			"3/4".asRational,
			0.75.asRational,
			Rational.fromReducedTerms(1, 2),
			Rational(1, 3) + Rational(1, 5),
			Rational(1, 3) - Rational(1, 5),
			Rational(1, 3) * Rational(3, 5),
			Rational(1, 3) / Rational(3, 5),
			Rational(2, 3).pow(4),
			Rational(1, 2).reciprocal,
			Rational(1, 2).neg,
			Rational(-1, 2).abs
		];

		cases.do { |r|
			this.assert(
				r.numerator.isKindOf(Float),
				format("% numerator is Float (was %)", r, r.numerator.class),
				isVerbose
			);
			this.assert(
				r.denominator.isKindOf(Float),
				format("% denominator is Float (was %)", r, r.denominator.class),
				isVerbose
			);
		};

		numTests.do {
			var r = Rational(rrand(minIntVal, maxIntVal), 1 + maxIntVal.rand * [-1, 1].choose);
			this.assert(
				r.numerator.isKindOf(Float) and: { r.denominator.isKindOf(Float) },
				format("% components are Float", r),
				isVerbose
			);
		};
	}

	// See Note [Rational bounds]. This checks the same rule by value, not just by
	// class.
	//   1. Big values supplied directly. They need ".0" in source, because
	//      sclang wraps integer literals at parse time.
	//   2. Normal integer inputs whose intermediate products exceed 2^31. This
	//      is the path users hit in practice, and it is what broke sort order
	//      and associativity in real use.
	test_Precision_Beyond32Bit {
		var big = 3000000000.0;   // 2^31 < big < 2^32
		var huge = 1.0e15;        // far beyond 32-bit range
		var checkRat = { |rat, num, denom, msg|
			this.assertEquals(rat.numerator, num, msg ++ " numerator", isVerbose);
			this.assertEquals(rat.denominator, denom, msg ++ " denominator", isVerbose);
		};

		// Big Float values supplied directly.
		checkRat.(Rational(big, 1.0), big, 1.0, "Rational(3e9, 1)");
		checkRat.(Rational(huge, 7.0), huge, 7.0, "Rational(1e15, 7)");
		checkRat.(Rational(1.0, big) + Rational(1.0, 7.0),
			3000000007.0, 21000000000.0, "1/3e9 + 1/7");
		checkRat.(Rational(1.0, big) - Rational(1.0, 7.0),
			-2999999993.0, 21000000000.0, "1/3e9 - 1/7");
		checkRat.(Rational(1.0, big) * Rational(7.0, 1.0),
			7.0, big, "1/3e9 * 7");
		checkRat.(Rational(1.0, big) / Rational(1.0, 7.0),
			7.0, big, "1/3e9 / (1/7)");
		this.assert(
			Rational(1.0, big) < Rational(1.0, 7.0),
			"1/3e9 < 1/7 with denominators past 2^31"
		);

		// normal Integer inputs, but intermediates past 2^31.
		checkRat.(Rational(50000, 1) * 50000,
			2500000000.0, 1.0, "50000/1 * 50000");
		checkRat.(Rational(114622, 114621) + Rational(114621, 114622),
			26276176525.0, 13138088262.0, "114622/114621 + 114621/114622");
		checkRat.(Rational(1, 114621) - Rational(1, 114622),
			1.0, 13138088262.0, "1/114621 - 1/114622");
		this.assert(
			Rational(114622, 114621) > Rational(114621, 114622),
			"comparison survives cross-multiplication past 2^31"
		);

		// The Float ceiling from Note [Rational bounds].
		checkRat.(Rational(maxFloatVal, 1.0), maxFloatVal, 1.0, "Rational(2^53, 1)");
		checkRat.(Rational(maxFloatVal - 1, 3.0), maxFloatVal - 1, 3.0, "Rational(2^53-1, 3)");
		checkRat.(Rational(minFloatVal, 1.0), minFloatVal, 1.0, "Rational(-2^53, 1)");
		this.assertEquals(
			(Rational(1.0, maxFloatVal) * Rational(maxFloatVal, 1.0)).numerator, 1.0,
			"1/2^53 * 2^53/1 == 1", isVerbose
		);
	}

	// See Note [Rational bounds]. These examples would build huge intermediates
	// _without_ cross-reduction, but the reduced result is still representable.
	test_CrossReduction_EdgePrecision {
		var checkRat = { |rat, num, denom, msg|
			this.assertEquals(rat.numerator, num, msg ++ " numerator", isVerbose);
			this.assertEquals(rat.denominator, denom, msg ++ " denominator", isVerbose);
		};

		checkRat.(
			Rational(maxFloatVal, 3.0) * Rational(3.0, maxFloatVal),
			1.0, 1.0,
			"(2^53/3) * (3/2^53)"
		);

		checkRat.(
			Rational(maxFloatVal, 3.0) / Rational(maxFloatVal, 7.0),
			7.0, 3.0,
			"(2^53/3) / (2^53/7)"
		);

		checkRat.(
			Rational(1.0, maxFloatVal) + Rational(1.0, maxFloatVal),
			1.0, maxFloatVal / 2,
			"1/2^53 + 1/2^53"
		);

		checkRat.(
			Rational(1.0, maxFloatVal) - Rational(1.0, maxFloatVal),
			0.0, 1.0,
			"1/2^53 - 1/2^53"
		);

		checkRat.(
			Rational(minFloatVal, 3.0) * Rational(3.0, maxFloatVal),
			-1.0, 1.0,
			"(-2^53/3) * (3/2^53)"
		);

		checkRat.(
			Rational(maxFloatVal, -3.0) / Rational(maxFloatVal, 7.0),
			-7.0, 3.0,
			"(2^53/-3) / (2^53/7)"
		);
	}

	// These pin the random add/sub bound to an exact no-cancellation example.
	test_AddSub_EdgePrecision {
		var m = maxAddSubIntVal;
		var checkRat = { |rat, num, denom, msg|
			this.assertEquals(rat.numerator, num, msg ++ " numerator", isVerbose);
			this.assertEquals(rat.denominator, denom, msg ++ " denominator", isVerbose);
		};

		checkRat.(
			Rational(m, m - 1) + Rational(m - 1, m),
			9007199120523265.0, 4503599560261632.0,
			"add/sub bound addition"
		);

		checkRat.(
			Rational(m, m - 1) - Rational(m - 1, m),
			134217727.0, 4503599560261632.0,
			"add/sub bound subtraction"
		);
	}

	// Comparison needs one exact product on each side, so maxIntVal can be larger
	// than the add and sub bounds.
	test_Compare_EdgePrecision {
		this.assert(
			Rational(maxIntVal, maxIntVal - 1) > Rational(maxIntVal - 1, maxIntVal),
			"Comparison works at the one-product precision bound"
		);
	}

	// These cases use optimized paths in +, -, and compareValue. The result should
	// be the same as the general path, including reduction.
	test_AddSubCompare_FastPathShapes {
		var r;

		this.assertEquals(
			Rational(1, 7) + Rational(2, 7),
			Rational(3, 7),
			"Add with same denominator",
			isVerbose
		);
		this.assertEquals(
			Rational(1, 7) + Rational(6, 7),
			Rational(1, 1),
			"Add with same denominator still reduces",
			isVerbose
		);
		this.assertEquals(
			Rational(2, 7) - Rational(1, 7),
			Rational(1, 7),
			"Subtract with same denominator",
			isVerbose
		);

		r = Rational(355, 1) + Rational(22, 1);
		this.assertEquals(r.numerator, 377.0, "Denominator-1 add numerator", isVerbose);
		this.assertEquals(r.denominator, 1.0, "Denominator-1 add denominator", isVerbose);

		r = Rational(355, 1) - Rational(22, 1);
		this.assertEquals(r.numerator, 333.0, "Denominator-1 subtract numerator", isVerbose);
		this.assertEquals(r.denominator, 1.0, "Denominator-1 subtract denominator", isVerbose);

		this.assert(Rational(1, 7) < Rational(2, 7), "Compare with same denominator");
		this.assert(Rational(355, 1) > Rational(22, 1), "Compare with denominator 1");
	}

	test_asRational_Precision {
		var irrationals = [pi, 2.sqrt, 3.sqrt, exp(1)];
		var maxDenominators = [10, 100, 1000, 10000];

		irrationals.do { |val|
			var prevError = inf;
			maxDenominators.do { |maxDenom|
				var rat = val.asRational(maxDenom);
				var error = (val - rat.asFloat).abs;

				this.assert(
					rat.denominator <= maxDenom,
					format("%.asRational(%) denominator % <= %", val, maxDenom, rat.denominator, maxDenom)
				);
				this.assert(
					error <= prevError,
					format("%.asRational(%) error % <= previous %", val, maxDenom, error, prevError)
				);
				prevError = error;
			};
		};

		this.assertEquals(pi.asRational(10), Rational(22, 7), "pi.asRational(10) == 22/7");
		this.assertEquals(pi.asRational(1000), Rational(355, 113), "pi.asRational(1000) == 355/113");
	}

	test_Reciprocals {
		numTests.do {
			var x = rrand(minIntVal, maxIntVal);
			var y = 1 + maxIntVal.rand * [-1, 1].choose;
			var z = Rational(x, y);
			this.assertEquals(z, z.reciprocal.reciprocal, format("Reciprocal of % is involutory", z), isVerbose);
		};

		numTests.do {
			var x = rrand(1, 999);
			var y = rrand(1, 999);
			var z = Rational(x, y);
			this.assertEquals(z * z.reciprocal, Rational(1, 1), format("% * reciprocal = 1", z), isVerbose);
		};
	}

	test_newFromString {
		numTests.do {
			var x = rrand(minIntVal, maxIntVal);
			var y = 1 + maxIntVal.rand * [-1, 1].choose;
			var rat1 = Rational(x, y);
			var rat2 = Rational.newFrom(x.asString ++ "/" ++ y.asString);
			this.assertEquals(rat1, rat2, format("String parsing for %", rat1), isVerbose);
		};
	}

	test_strangeStringInput {
		var strangeStrings = [
			"3 %/ 2  ",
			"3  /  2 ",
			"3    /   2 ",
			"   3    / 2    ",
			"    3     / 2      ",
			"    3   %    /   2   ",
			"    3    /    2 ",
			"        3       %      /         2 "
		];

		strangeStrings.do { |str, i|
			this.assertEquals(
				Rational(str), Rational(3, 2),
				format("Strange string #%: '%'", i, str),
				isVerbose
			);
		};
	}

	test_StringInput_Floats {
		this.assertEquals("1 / 0.1".asRational, Rational(10, 1), "String '1 / 0.1' parses correctly");
	}

	test_commutativeAdd {
		numTests.do {
			var x1 = rrand(minAddSubIntVal, maxAddSubIntVal);
			var y1 = 1 + maxAddSubIntVal.rand * [-1, 1].choose;
			var x2 = rrand(minAddSubIntVal, maxAddSubIntVal);
			var y2 = 1 + maxAddSubIntVal.rand;
			var z1 = Rational(x1, y1);
			var z2 = Rational(x2, y2);
			this.assertEquals(z1 + z2, z2 + z1, format("% + % commutes", z1, z2), isVerbose);
		};
	}

	test_commutativeMul {
		numTests.do {
			var x1 = rrand(minIntVal, maxIntVal);
			var y1 = 1 + maxIntVal.rand * [-1, 1].choose;
			var x2 = rrand(minIntVal, maxIntVal);
			var y2 = 1 + maxIntVal.rand * [-1, 1].choose;
			var z1 = Rational(x1, y1);
			var z2 = Rational(x2, y2);
			this.assertEquals(z1 * z2, z2 * z1, format("% * % commutes", z1, z2), isVerbose);
		};
	}

	test_Additive_Inverse {
		numTests.do {
			var x = rrand(minIntVal, maxIntVal);
			var y = 1 + maxIntVal.rand * [1, -1].choose;
			var rat = Rational(x, y);

			this.assertEquals(Rational(x * -1, y), Rational(x, y * -1), format("Additive inverse 1: %", rat), isVerbose);
			this.assertEquals((-1) * rat, Rational(x * -1, y), format("Additive inverse 2: %", rat), isVerbose);
			this.assertEquals((-1) * rat, Rational(x, y * -1), format("Additive inverse 3: %", rat), isVerbose);
		};
	}

	test_Multiplicative_Inverse {
		numTests.do {
			var x = 1 + maxIntVal.rand * [-1, 1].choose;
			var y = 1 + maxIntVal.rand * [-1, 1].choose;
			var rat1 = Rational(x, y);
			var rat2 = Rational(y, x);
			this.assertEquals(rat1.pow(-1), rat2, format("%.pow(-1) == %", rat1, rat2), isVerbose);
		};
	}

	test_Div_Eq_ReciprocalMul {
		numTests.do {
			var x1 = rrand(minIntVal, maxIntVal);
			var y1 = 1 + maxIntVal.rand * [-1, 1].choose;
			var x2 = 1 + maxIntVal.rand * [-1, 1].choose;
			var y2 = 1 + maxIntVal.rand * [-1, 1].choose;
			var z1 = Rational(x1, y1);
			var z2 = Rational(x2, y2);
			this.assertEquals(z1 / z2, z1 * z2.reciprocal, format("% / % == % * reciprocal", z1, z2, z1), isVerbose);
		};
	}

	test_Neg_Subtraction {
		numTests.do {
			var x1 = rrand(minAddSubIntVal, maxAddSubIntVal);
			var y1 = 1 + maxAddSubIntVal.rand * [-1, 1].choose;
			var x2 = rrand(minAddSubIntVal, maxAddSubIntVal);
			var y2 = 1 + maxAddSubIntVal.rand * [-1, 1].choose;
			var z1 = Rational(x1, y1);
			var z2 = Rational(x2, y2);
			this.assertEquals(z1 - z2, z2.neg - z1.neg, format("% - % == %.neg - %.neg", z1, z2, z2, z1), isVerbose);
		};
	}

	test_reciprocal_Div {
		numTests.do {
			var x1 = 1 + maxIntVal.rand * [-1, 1].choose;
			var y1 = 1 + maxIntVal.rand * [-1, 1].choose;
			var x2 = 1 + maxIntVal.rand * [-1, 1].choose;
			var y2 = 1 + maxIntVal.rand * [-1, 1].choose;
			var z1 = Rational(x1, y1);
			var z2 = Rational(x2, y2);
			this.assertEquals(z1 / z2, (z2 / z1).reciprocal, format("% / % == (% / %).reciprocal", z1, z2, z2, z1), isVerbose);
		};
	}

	test_Sort_and_Scramble {
		var listSize = 100;
		numTests.do {
			var ratList = Array.fill(listSize, {
				Rational(rrand(minIntVal, maxIntVal), 1 + maxIntVal.rand * [-1, 1].choose)
			});
			this.assertEquals(ratList.sort, ratList.scramble.sort, "Sort is stable after scramble", isVerbose);
		};
	}

	test_Sort_and_asFloat {
		var listSize = 100;
		numTests.do {
			var ratList = Array.fill(listSize, {
				Rational(rrand(minIntVal, maxIntVal), 1 + maxIntVal.rand * [-1, 1].choose)
			});
			this.assertEquals(
				ratList.scramble.sort.asFloat,
				ratList.scramble.asFloat.sort,
				"Sorted rationals match sorted floats",
				isVerbose
			);
		};
	}

	test_Mul_Inverse_DifferentExponents_NonZeroIntInput {
		numTests.do {
			var maxVal = 100;
			var x1 = 1 + maxVal.rand * [-1, 1].choose;
			var y1 = 1 + maxVal.rand * [-1, 1].choose;
			var rat = Rational(x1, y1);
			var maxExponent = 6;

			maxExponent.do { |i|
				this.assertEquals(
					rat.pow(i * -1),
					rat.pow(i).reciprocal,
					format("%.pow(%) == %.pow(%).reciprocal", rat, i.neg, rat, i),
					isVerbose
				);
			};
		};
	}

	test_Float_Rat_Float {
		numTests.do {
			var maxVal = 1000.01;
			var x = 1 + maxVal.rand * [1, -1].choose;
			var y = 1 + maxVal.rand * [1, -1].choose;
			var rat = Rational(x, y);
			var float = x / y;

			this.assertFloatEquals(
				rat.asFloat,
				float.asRational.asFloat,
				format("Float conversion roundtrip for %", rat),
				0.000001,
				isVerbose
			);
		};
	}

	test_Rat_Float_Rat {
		numTests.do {
			var maxVal = 100;
			var x = 1 + maxVal.rand * [1, -1].choose;
			var y = 1 + maxVal.rand * [1, -1].choose;
			var rat = Rational(x, y);
			var float = x / y;

			this.assertEquals(
				rat,
				float.asRational.asFloat.asRational,
				format("Rational conversion roundtrip for %", rat),
				isVerbose
			);
		};
	}

	test_Exponentiation {
		numTests.do {
			var maxVal = 9;
			var x = 1 + maxVal.rand;
			var y = 1 + maxVal.rand;
			var r = Rational(x, y);
			var e = rrand(2, 5).asInteger;
			var a = rrand(1, 3).asInteger;
			var b = rrand(1, 3).asInteger;

			this.assertEquals(
				r.pow(e) * r.pow(e.neg),
				Rational(1, 1),
				format("%.pow(%) * %.pow(%) == 1", r, e, r, e.neg),
				isVerbose
			);

			this.assertEquals(
				r.pow(e),
				e.collect { r }.reduce('*'),
				format("%.pow(%) == repeated multiplication", r, e),
				isVerbose
			);

			this.assertEquals(
				r.pow(a).pow(b),
				r.pow(a * b),
				format("%.pow(%).pow(%) == %.pow(%)", r, a, b, r, a * b),
				isVerbose
			);
		};
	}

	test_Exponentiation_NonInteger {
		this.assertEquals(
			(2 %/ 3).pow(2.0),
			4 %/ 9,
			"Integral Float exponent stays Rational",
			isVerbose
		);

		this.assertFloatEquals(
			(4 %/ 9).pow(0.5),
			(4 %/ 9).asFloat.pow(0.5),
			"Fractional exponent delegates to Float",
			0.000001,
			isVerbose
		);

		this.assertFloatEquals(
			(2 %/ 3).pow(2.5),
			(2 %/ 3).asFloat.pow(2.5),
			"Non-integer exponent is not truncated",
			0.000001,
			isVerbose
		);
	}

	// See Note [Rational bounds]. Associativity is depth 3, so it uses
	// maxAssocIntVal instead of maxIntVal.
	test_AssociativeAdd {
		numTests.do {
			var x1 = rrand(minAssocIntVal, maxAssocIntVal);
			var y1 = 1 + maxAssocIntVal.rand * [-1, 1].choose;
			var x2 = rrand(minAssocIntVal, maxAssocIntVal);
			var y2 = 1 + maxAssocIntVal.rand * [-1, 1].choose;
			var x3 = rrand(minAssocIntVal, maxAssocIntVal);
			var y3 = 1 + maxAssocIntVal.rand * [-1, 1].choose;
			var z1 = Rational(x1, y1);
			var z2 = Rational(x2, y2);
			var z3 = Rational(x3, y3);

			this.assertEquals(
				(z1 + z2) + z3,
				z1 + (z2 + z3),
				format("(% + %) + % == % + (% + %)", z1, z2, z3, z1, z2, z3),
				isVerbose
			);
		};
	}

	// See Note [Rational bounds]. Associativity is depth 3, so it uses
	// maxAssocIntVal instead of maxIntVal.
	test_AssociativeMul {
		numTests.do {
			var x1 = rrand(minAssocIntVal, maxAssocIntVal);
			var y1 = 1 + maxAssocIntVal.rand * [-1, 1].choose;
			var x2 = rrand(minAssocIntVal, maxAssocIntVal);
			var y2 = 1 + maxAssocIntVal.rand * [-1, 1].choose;
			var x3 = rrand(minAssocIntVal, maxAssocIntVal);
			var y3 = 1 + maxAssocIntVal.rand * [-1, 1].choose;
			var z1 = Rational(x1, y1);
			var z2 = Rational(x2, y2);
			var z3 = Rational(x3, y3);

			this.assertEquals(
				(z1 * z2) * z3,
				z1 * (z2 * z3),
				format("(% * %) * % == % * (% * %)", z1, z2, z3, z1, z2, z3),
				isVerbose
			);
		};
	}

	test_Distributive {
		numTests.do {
			var minVal = 1;
			var maxVal = 20;
			var x1 = rrand(minVal, maxVal);
			var y1 = 1 + maxVal.rand;
			var x2 = rrand(minVal, maxVal);
			var y2 = 1 + maxVal.rand;
			var x3 = rrand(minVal, maxVal);
			var y3 = 1 + maxVal.rand;
			var a = Rational(x1, y1);
			var b = Rational(x2, y2);
			var c = Rational(x3, y3);

			this.assertEquals(
				(a + b) * c,
				(a * c) + (b * c),
				format("(% + %) * % == (% * %) + (% * %)", a, b, c, a, c, b, c),
				isVerbose
			);
		};
	}

	test_commutativeAdd_Array {
		numTests.do {
			var n = 20;
			var minVal = -10;
			var maxVal = 10;
			var xs = Array.fill(n, { rrand(minVal, maxVal) });
			var ys = Array.fill(n, { 1 + maxVal.rand * [-1, 1].choose });
			var rats = n.collect { |i| Rational(xs[i], ys[i]) };
			var original = rats.sum;
			var scrambled = rats.scramble.sum;

			this.assertEquals(original, scrambled, "Array sum is commutative", isVerbose);
		};
	}

	test_NormalizedForm {
		numTests.do {
			var x = rrand(minIntVal, maxIntVal);
			var y = 1 + maxIntVal.rand * [-1, 1].choose;
			var r = Rational(x, y);
			this.assert(
				Rational.fromReducedTerms(r.numerator, r.denominator) == r,
				"Rational is always in reduced canonical form"
			);
		};
	}


}

/*
TestRational.run
*/

TestRational : UnitTest {

    var <>minIntVal = -114622, <>maxIntVal = 114622;
    var <>minFloatVal = -100738241.0, <>maxFloatVal = 100738241.0;
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
            var x1 = rrand(minIntVal, maxIntVal);
            var y1 = 1 + maxIntVal.rand * [-1, 1].choose;
            var x2 = rrand(minIntVal, maxIntVal);
            var y2 = 1 + maxIntVal.rand;
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
            var x1 = rrand(minIntVal, maxIntVal);
            var y1 = 1 + maxIntVal.rand * [-1, 1].choose;
            var x2 = rrand(minIntVal, maxIntVal);
            var y2 = 1 + maxIntVal.rand * [-1, 1].choose;
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

    test_AssociativeAdd {
        numTests.do {
            var x1 = rrand(minIntVal, maxIntVal);
            var y1 = 1 + maxIntVal.rand * [-1, 1].choose;
            var x2 = rrand(minIntVal, maxIntVal);
            var y2 = 1 + maxIntVal.rand * [-1, 1].choose;
            var x3 = rrand(minIntVal, maxIntVal);
            var y3 = 1 + maxIntVal.rand * [-1, 1].choose;
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

    test_AssociativeMul {
        numTests.do {
            var x1 = rrand(minIntVal, maxIntVal);
            var y1 = 1 + maxIntVal.rand * [-1, 1].choose;
            var x2 = rrand(minIntVal, maxIntVal);
            var y2 = 1 + maxIntVal.rand * [-1, 1].choose;
            var x3 = rrand(minIntVal, maxIntVal);
            var y3 = 1 + maxIntVal.rand * [-1, 1].choose;
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
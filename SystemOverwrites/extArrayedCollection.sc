+ ArrayedCollection {
	plot { |name, bounds, discrete=false, numChannels, minval, maxval, separately = true|
		var array, plotter;
		array = this.as(Array);

		if(array.maxDepth > 3) {
			"Cannot currently plot an array with more than 3 dimensions".warn;
			^nil
		};
		plotter = Plotter(name, bounds);
		if(discrete) { plotter.plotMode = \points };

		numChannels !? { array = array.unlace(numChannels) };
		array = array.collect {|elem|
			if (elem.isKindOf(Env)) {
				elem.asMultichannelSignal.flop
			} {
				if (elem.isKindOf(Rational))
				{ elem.asFloat }
				{ elem }
			};

		};
		plotter.setValue(
			array,
			findSpecs: true,
			separately: separately,
			refresh: true,
			minval: minval,
			maxval: maxval
		);

		^plotter
	}
}


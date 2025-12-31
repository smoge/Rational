+ ArrayedCollection {
    plot { |name, bounds, discrete = false, numChannels, minval, maxval, separately = true, parent|
        var array, plotter;
        array = this.as(Array);
        if (array.maxDepth > 3) {
            "Cannot currently plot an array with more than 3 dimensions".warn;
            ^nil
        };
        plotter = Plotter(name, bounds, parent);
        if (discrete) { plotter.plotMode = \points };
        numChannels !? { array = array.unlace(numChannels) };
        array = array.collect { |elem|
            case
            { elem.isKindOf(Env) } { elem.asMultichannelSignal.flop }
            { elem.isKindOf(Rational) } { elem.asFloat }
            { elem }
        };
        plotter.setValue(array, findSpecs: true, separately: separately, refresh: true, minval: minval, maxval: maxval);
        ^plotter
    }
}

+ ListPattern {
    copy { ^super.copy.list_(list.copy) }
    storeArgs { ^[list, repeats] }
}

+ Event {
    delta {
        var dominated, delta;
        ^(this[\delta] ?? {
            dominated = this[\dur] ?? { 1.0 };
            dominated = if (dominated.isKindOf(Rational)) { dominated.asFloat } { dominated };
            dominated * this[\stretch] ?? { 1.0 } - (this[\lag] ?? { 0.0 }) - (this[\timingOffset] ?? { 0.0 })
        })
    }
}

+ Pseq {
    embedInStream { |inval|
        var item, offsetValue;
        offsetValue = offset.value(inval);
        if (inval.eventAt('reverse') == true) {
            repeats.value(inval).do { |j|
                list.size.reverseDo { |i|
                    item = list.wrapAt(i + offsetValue);
                    inval = if (item.isKindOf(Rational)) {
                        item.asFloat.embedInStream(inval)
                    } {
                        item.embedInStream(inval)
                    };
                };
            };
        } {
            repeats.value(inval).do { |j|
                list.size.do { |i|
                    item = list.wrapAt(i + offsetValue);
                    inval = if (item.isKindOf(Rational)) {
                        item.asFloat.embedInStream(inval)
                    } {
                        item.embedInStream(inval)
                    };
                };
            };
        };
        ^inval
    }

    storeArgs { ^[list, repeats, offset] }
}

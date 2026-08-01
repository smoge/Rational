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

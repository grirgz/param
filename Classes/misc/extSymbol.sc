
// FIXME: this is bad, take habit of using +++ everywhere then without the quark everything is broken
// also can be already used by another quark
+ Symbol {
	+++ { arg right;
		^ (this ++ right).asSymbol
	}
}

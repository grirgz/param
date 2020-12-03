

	(
	~normalizedPath = { arg path;
		// limitation: this algo doesnt always add a final / even when sure it's a folder
		//var path = this.fullPath;
		var ret;
		var sep = $/, empty = "", dot = ".", dotdot = "..";
		var initial_slashes = 0;
		var initial_dot = 0;
		var comps, new_comps;
		ret = if ( path == empty ){ empty };
		ret ?? {

			initial_dot = ( path.split(sep).select(_ != empty).first == dot );

			initial_slashes = path.beginsWith(sep.asString);
			if (
				initial_slashes 
				and: { path.beginsWith("//") and: { path.beginsWith("///").not } }
			) { 
				initial_slashes = 1; // do not allow starting by double slash
			};
			initial_slashes = initial_slashes.asInteger;
			comps = path.split(sep);
			//[path, comps].asCompileString.debug("com");
			//initial_slashes.debug("initial_slashes");
			new_comps = List.new;
			comps.do { arg comp, idx;
				block { arg break;
					if ( [empty, dot].includesEqual(comp)) {
						// allow dot only on first comp;
						break.value;
					};
					//[ comp != dotdot,  ]
					if (
						comp != dotdot
						//[dotdot, dot].includes(comp)
						or: { ( initial_slashes == 0 and: { new_comps.size == 0 } ) 
						or: { new_comps.size > 0 and: { new_comps.last == dotdot }}}
					) {
						//[new_comps, comp].debug("add");
						new_comps.add(comp);
					} {
						if ( new_comps.size > 0 ) {
							//[new_comps, comp].debug("remove");
							new_comps.pop
						}
					};
				}
			};
			//[comps, new_comps].asCompileString.debug("com, nc");
			comps = new_comps;
			if(comps.last == dotdot) {
				comps[comps.size-1] = comps.last ++ sep
			};
			path = comps.join(sep);
			if ( initial_slashes > 0 ) {
				path = sep.dup(initial_slashes).join ++ path
			};
			if(initial_dot) {
				path = "." +/+ path
			};
			ret = if(path.size > 0) {
				path
			} {
				dot ++ "/";
			};
		};
		ret;
	}
	)


(
	~dotest = {
		var data =   [
			[ "", "" ],
			[ "/..", "/" ],
			[ "/../", "/" ],
			[ ".", "./" ],
			[ "./", "./" ],
			[ "./blo.wav", "./blo.wav" ],
			[ "././blo.wav", "./blo.wav" ],
			[ "./../blo.wav", "./../blo.wav" ], // should be "../blo.wav" but "./../blo.wav" is acceptable
			[ "/blo.wav", "/blo.wav" ],
			[ "..", "../" ],
			[ "../", "../" ],
			[ "../abc/def", "../abc/def" ],
			[ "../abc/def/..", "../abc" ], // should have final slash but acceptable
			[ "../abc/././././def/..", "../abc" ], // should have final slash but acceptable
			[ "////../abc/def", "/abc/def" ],
			[ "/../def", "/def" ],
			[ "../def", "../def" ],
			[ "/abc////../def", "/def" ],
			[ "abc/../def/ghi", "def/ghi" ],
			[ "/abc/def/../ghi", "/abc/ghi" ],
			[ "/abc/..abc////../def", "/abc/def" ],
			[ "/abc/..abc/../def", "/abc/def" ],
			[ "abc/../def", "def" ],
			[ "abc/../../def", "../def" ],
			[ "././", "./" ],
			[ "abc/..", "./" ],
			[ "abc/../", "./" ],
			[ "abc/../..", "../" ],
			[ "abc/../../", "../" ],
			[ "a/..", "./" ],
			[ "a/../", "./" ],
			[ "a/../..", "../" ],
			[ "a/../../", "../" ],
			[ "../../../a", "../../../a" ],
			[ "../a../../a", "../a" ],
			[ "cccc/abc////..////.//../", "./" ],
			[ "aaaa/cccc/abc////..////.//../", "aaaa" ], // should have final slash but acceptable
			[ "..//////.///..////..////.//////abc////.////..////def//abc/..", "../../../def" ], // should have final slash but acceptable
			[ "////////////..//////.///..////..////.//////abc////.////..////def//abc/..", "/def" ], // should have final slash but acceptable
		];
		data.do({ arg arr;
			var test = ~normalizedPath.(arr[0]) == arr[1];
			var passed = if(test) { "OK" } { "FAIL" };
			debug("%: In: %. Out: %. Should be: %".format(passed, arr[0].asCompileString, ~normalizedPath.(arr[0]).asCompileString, arr[1].asCompileString));
			test;
		})
	};
	~dotest.value;
)

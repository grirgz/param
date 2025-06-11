
// not the best place for theses useful additions
// FIXME: should work with all editors
+ String {
	pbcopy {
		"xsel -ib <<EOD\n%\nEOD".format(this).unixCmd
	}

	vimpbpaste {
		"vim --servername scvim --remote-send '<Esc>\"+p<Enter>'".unixCmd;
	}

	vimpaste {
		"vim --servername scvim --remote-send '<Esc>:a!<Enter>%\n<C-c>'".format(this).unixCmd;
	}

	
	editorInsert { arg inblock=true;
		//this.pbcopy;
		var str = this;
		var filename = "/tmp/scclipboard";
		if(inblock) {
			str = "(\n%\n);\n".format(str)
		};

		case(
			{ Platform.ideName == "scvim" }, {
				//"cat > % <<EOD\n%\nEOD".format(filename, str).unixCmd; // bug with xsel + vim + big buffer
				File.use(filename, "w") { arg file;
					file.write(str)
				};
				"vim --servername scvim --remote-send '<Esc>:read %<Enter>'".format(filename).unixCmd;
			},
			{ Platform.ideName == "scnvim" }, {
				// TODO: to be tested
				File.use(filename, "w") { arg file;
					file.write(str)
				};
				"nvim --servername scnvim --remote-send '<Esc>:read %<Enter>'".format(filename).unixCmd;
			},
			{ Platform.ideName == "scqt" }, {
				var doc = Document.current;
				var endlinepos;
				var content = doc.string[doc.selectionStart..];
				endlinepos = doc.selectionStart + content.find("\n");
				doc.string_("\n"++ str, endlinepos);
			}, {
				Log(\Param).error("ERROR: Don't know how to editorInsert with %", Platform.ideName);
			}
		)
	}
}

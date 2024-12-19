
// not the best place for theses useful additions
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

		//"cat > % <<EOD\n%\nEOD".format(filename, str).unixCmd; // bug with xsel + vim + big buffer
		File.use(filename, "w") { arg file;
			file.write(str)
		};
		"vim --servername scvim --remote-send '<Esc>:read %<Enter>'".format(filename).unixCmd;
	}
}

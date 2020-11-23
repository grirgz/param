
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
		if(inblock) {
			str = "(\n%\n);\n".format(str)
		};

		"cat > /tmp/scclipboard <<EOD\n%\nEOD".format(str).unixCmd; // bug with xsel + vim + big buffer
		"vim --servername scvim --remote-send '<Esc>:read /tmp/scclipboard<Enter>'".unixCmd;
	}
}

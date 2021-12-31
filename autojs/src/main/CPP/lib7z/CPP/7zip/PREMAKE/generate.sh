#!/bin/sh

set -x

doit()
{
	rm -fr P7ZIP.$1
	mkdir P7ZIP.$1
	cd P7ZIP.$1
	cp ../premake4.lua premake4.lua
	premake4 $1
	cd ..
}

doit codeblocks
doit codelite
doit gmake


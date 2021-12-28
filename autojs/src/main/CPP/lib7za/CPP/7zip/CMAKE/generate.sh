
doit()
{
	cd ..
	rm -fr "P7ZIP.$1"
	mkdir  "P7ZIP.$1"
	cd     "P7ZIP.$1"

	cmake -G "$2" -DCMAKE_BUILD_TYPE=Debug  ../CMAKE/
	#cmake -G "$2" -DCMAKE_BUILD_TYPE=Release  ../CMAKE/
}

CURDIR=$PWD

cd $CURDIR
doit "Unix" "Unix Makefiles"

cd $CURDIR
doit "codeblocks" "CodeBlocks - Unix Makefiles"

#cd $CURDIR
#doit "KDevelop3" "KDevelop3"

cd $CURDIR
doit "EclipseCDT4" "Eclipse CDT4 - Unix Makefiles"

cd $CURDIR
doit "ninja" "Ninja"


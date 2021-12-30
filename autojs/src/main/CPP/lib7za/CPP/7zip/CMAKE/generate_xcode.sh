
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
CMAKE_OSX_ARCHITECTURES=i386
export CMAKE_OSX_ARCHITECTURES
doit "Xcode" "Xcode"


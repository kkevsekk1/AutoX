
# TARGET_FLAGS=-D_7ZIP_ST

include ../../../../makefile.crc32
include ../../../../makefile.machine

TARGET_FLAGS=$(CC_EXE)
LOCAL_LINK=$(LINK_EXE)
LIBS=$(LOCAL_LIBS)

include ../../../../makefile.glb

test: $(PROG)
	echo TOOLS=$(TOOLS)
	cp LzmaAlone.cpp testfile
	$(TOOLS) ./$(PROG) e testfile testfile.lzma
	$(TOOLS) ./$(PROG) d testfile.lzma testfile.lzma.d 
	diff -s testfile testfile.lzma.d
	$(TOOLS) ./$(PROG) e testfile testfile.lzma
	$(TOOLS) ./$(PROG) d testfile.lzma testfile.lzma.d 
	diff -s testfile testfile.lzma.d
	$(TOOLS) ./$(PROG) e -si -so < testfile > testfile.lzma.2
	$(TOOLS) ./$(PROG) d -si -so < testfile.lzma.2 > testfile.lzma.2.d 
	diff -s testfile testfile.lzma.2.d
	@echo =========
	@echo All Done 
	@echo =========


clean2: clean
	rm -f testfile*


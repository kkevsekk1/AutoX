

all:
	$(MAKE) -C 7za          all
	$(MAKE) -C 7z           all
	$(MAKE) -C 7zr          all
	$(MAKE) -C Format7zFree all
	$(MAKE) -C test_lib     all
	$(MAKE) -C MemLat       all
	$(MAKE) -C PipeLen      all

clean:
	$(MAKE) -C 7za          clean
	$(MAKE) -C 7z           clean
	$(MAKE) -C 7zr          clean
	$(MAKE) -C Format7zFree clean
	$(MAKE) -C test_lib     clean
	$(MAKE) -C MemLat       clean
	$(MAKE) -C PipeLen      clean

install: all
	$(MAKE) -C 7za          install
	$(MAKE) -C 7z           install
	$(MAKE) -C 7zr          install
	$(MAKE) -C Format7zFree install
	$(MAKE) -C test_lib     install
	$(MAKE) -C MemLat       install
	$(MAKE) -C PipeLen      install



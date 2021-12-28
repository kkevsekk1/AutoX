cmake_minimum_required(VERSION 2.6)

project(p7zip)

SET(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${PROJECT_BINARY_DIR}/bin
    CACHE STRING "Where binaries and .dll files go" FORCE)
    

IF(HAVE_WCHAR_H)
  OPTION(HAVE_UNICODE_WCHAR "Enable Unicode Support" YES)
  MARK_AS_ADVANCED(HAVE_UNICODE_WCHAR)
ELSE(HAVE_WCHAR_H)
  SET(HAVE_UNICODE_WCHAR 0)
ENDIF(HAVE_WCHAR_H)

SET(HAVE_PTHREADS 0)
FIND_PACKAGE(Threads)
IF(WIN32 OR CMAKE_USE_PTHREADS_INIT)
  SET(HAVE_PTHREADS 1)
ENDIF(WIN32 OR CMAKE_USE_PTHREADS_INIT)

add_definitions(-DENV_HAVE_GCCVISIBILITYPATCH)

SET(CMAKE_CXX_FLAGS "-fvisibility=hidden -fvisibility-inlines-hidden")

add_subdirectory(7za)

add_subdirectory(7z_)

add_subdirectory(7zr)

add_subdirectory(Format7zFree  bin)

find_package(wxWidgets COMPONENTS core base adv)

if(wxWidgets_FOUND)

	add_subdirectory(7zG)

	add_subdirectory(7zFM)

else(wxWidgets_FOUND)
	message("Warning : wxWidgets not found")
endif(wxWidgets_FOUND)




PROJECT(lzhamdecomp)
cmake_minimum_required(VERSION 2.8)
option(BUILD_X64 "build 64-bit" TRUE)

message("Initial BUILD_X64=${BUILD_X64}")
message("Initial CMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}")

if( NOT CMAKE_BUILD_TYPE )
  set( CMAKE_BUILD_TYPE Release )
endif( NOT CMAKE_BUILD_TYPE )

message( ${PROJECT_NAME} " build type: " ${CMAKE_BUILD_TYPE} )

if (BUILD_X64)
	message("Building 64-bit")
else()
	message("Building 32-bit")
endif(BUILD_X64)

set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -Wall -Wextra")
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -Wall -Wextra")

set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Wall -Wextra -O3 -fomit-frame-pointer -fexpensive-optimizations")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Wall -Wextra")

set(SRC_LIST 
    ../include/lzham_dynamic_lib.h
    ../include/lzham_static_lib.h
    ../include/lzham.h
    ../include/lzham_exports.inc
    ../include/zlib.h
    lzham_assert.cpp       
    lzham_assert.h         
    lzham_checksum.cpp     
    lzham_checksum.h       
    lzham_config.h         
    lzham_core.h           
    lzham_decomp.h
    lzham_helpers.h        
    lzham_huffman_codes.cpp
    lzham_huffman_codes.h  
    lzham_lzdecompbase.cpp 
    lzham_lzdecompbase.h   
    lzham_lzdecomp.cpp     
    lzham_math.h           
    lzham_mem.cpp          
    lzham_mem.h            
    lzham_platform.cpp     
    lzham_platform.h       
    lzham_prefix_coding.cpp
    lzham_prefix_coding.h  
    lzham_symbol_codec.cpp 
    lzham_symbol_codec.h   
    lzham_timer.cpp        
    lzham_timer.h          
    lzham_traits.h         
    lzham_types.h          
    lzham_utils.h          
    lzham_vector.cpp       
    lzham_vector.h)

# -fno-strict-aliasing is *required* to compile LZHAM
set(GCC_COMPILE_FLAGS "-fno-strict-aliasing -D_LARGEFILE64_SOURCE=1 -D_FILE_OFFSET_BITS=64")

if (NOT BUILD_X64)
	set(GCC_COMPILE_FLAGS "${GCC_COMPILE_FLAGS} -m32")
endif()

set(CMAKE_EXE_LINKER_FLAGS  "${CMAKE_EXE_LINKER_FLAGS} ${GCC_LINK_FLAGS}")

set(CMAKE_C_FLAGS  "${CMAKE_C_FLAGS} ${GCC_COMPILE_FLAGS}")
set(CMAKE_C_FLAGS_RELEASE  "${CMAKE_C_FLAGS_RELEASE} ${GCC_COMPILE_FLAGS} -DNDEBUG")
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} ${GCC_COMPILE_FLAGS} -D_DEBUG")

set(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} ${GCC_COMPILE_FLAGS}")
set(CMAKE_CXX_FLAGS_RELEASE  "${CMAKE_CXX_FLAGS_RELEASE} ${GCC_COMPILE_FLAGS} -DNDEBUG")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} ${GCC_COMPILE_FLAGS} -D_DEBUG")

include_directories(
	${PROJECT_SOURCE_DIR}/../lzhamdecomp 
	${PROJECT_SOURCE_DIR}/../include)

#set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/bin_linux)

add_library(${PROJECT_NAME} ${SRC_LIST})
install(TARGETS ${PROJECT_NAME} LIBRARY DESTINATION lib${LIB_SUFFIX} ARCHIVE DESTINATION lib${LIB_SUFFIX} RUNTIME DESTINATION bin)

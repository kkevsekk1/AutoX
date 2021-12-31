cmake_minimum_required(VERSION 3.4.1)


add_library(zips SHARED src/main/cpp/zips.cpp )

#设置头文件查找目录
include_directories(src/main/cpp/lib7za/C src/main/cpp/lib7za/CPP)

#设置库查找目录
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/src/main/jniLibs/${CMAKE_ANDROID_ARCH_ABI}")

target_link_libraries(zips 7za log )
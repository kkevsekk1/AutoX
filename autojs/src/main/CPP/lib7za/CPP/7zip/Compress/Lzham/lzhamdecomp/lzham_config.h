// File: lzham_config.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

#if defined(_DEBUG) || defined(DEBUG)
   #define LZHAM_BUILD_DEBUG
   
   #ifndef DEBUG
      #define DEBUG
   #endif
#else
   #define LZHAM_BUILD_RELEASE
   
   #ifndef NDEBUG
      #define NDEBUG
   #endif
   
   #ifdef DEBUG
      #error DEBUG cannot be defined in LZHAM_BUILD_RELEASE
   #endif
#endif

// HACK HACK
#define LZHAM_BUFFERED_PRINTF 0
#define LZHAM_PERF_SECTIONS 0
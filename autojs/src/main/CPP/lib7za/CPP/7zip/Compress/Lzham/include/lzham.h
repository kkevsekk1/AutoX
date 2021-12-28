// File: lzham.h - Copyright (c) 2009-2012 Richard Geldreich, Jr. <richgel99@gmail.com>
// LZHAM uses the MIT License. See Copyright Notice and license at the end of this file.
//
// This is the main header file, includable from C or C++ files, which defines all the publically available API's, structs, and types used by the LZHAM codec.
//
// Notes:
// 
// As of LZHAM alpha8, there are now two sets of API's:
// - The first (oldest) API directly exposes all of the codec's functionality. See lzham_compress_init(), lzham_decompress_init(), etc. This API has the lowest overhead
//   and is the most tested.
// - The new API implements the most useful/popular subset of the zlib API, but doesn't expose all of the codec's functionality yet. See the lzham_z* functions. 
//   This functionality is provided because most users of compression libraries are already very familiar with the nuts and bolts of the zlib API.
//   For the most common zlib usage cases LZHAM is an almost drop-in replacement for zlib. To make switching from zlib even easier, you can define the LZHAM_DEFINE_ZLIB_API macro, 
//   which causes this header to #define most zlib symbols to their LZHAM equivalents.
//   Note that LZHAM does not actually implement the deflate/inflate algorithm, so it cannot decompress streams created by standard zlib yet (and of course, zlib cannot decompress
//   streams created by LZHAM). Internally, this API is mostly implemented via the older low-level LZHAM API.

#ifndef __LZHAM_H__
#define __LZHAM_H__

#ifdef _MSC_VER
#pragma once
#endif

#include <stdlib.h>

// Upper byte = major version
// Lower byte = minor version
#define LZHAM_DLL_VERSION 0x1011

#if defined(_WIN64) || defined(__MINGW64__) || defined(_LP64) || defined(__LP64__)
	#define LZHAM_64BIT 1
#endif

#if defined(_MSC_VER)
   #define LZHAM_CDECL __cdecl
#else
   #define LZHAM_CDECL
#endif

#ifdef LZHAM_EXPORTS
   #define LZHAM_DLL_EXPORT __declspec(dllexport)
#else
   #define LZHAM_DLL_EXPORT
#endif

#ifdef __cplusplus
extern "C" {
#endif

   typedef unsigned char   lzham_uint8;
   typedef signed int      lzham_int32;
   typedef unsigned int    lzham_uint32;
   typedef unsigned int    lzham_bool;

   // Returns DLL version (LZHAM_DLL_VERSION).
   LZHAM_DLL_EXPORT lzham_uint32 LZHAM_CDECL lzham_get_version(void);

   // User provided memory allocation

   // Custom allocation function must return pointers with LZHAM_MIN_ALLOC_ALIGNMENT (or better).
   #define LZHAM_MIN_ALLOC_ALIGNMENT sizeof(size_t) * 2

   typedef void*  (LZHAM_CDECL *lzham_realloc_func)(void* p, size_t size, size_t* pActual_size, lzham_bool movable, void* pUser_data);
   typedef size_t (LZHAM_CDECL *lzham_msize_func)(void* p, void* pUser_data);

   // Call this function to force LZHAM to use custom memory malloc(), realloc(), free() and msize functions.
   LZHAM_DLL_EXPORT void LZHAM_CDECL lzham_set_memory_callbacks(lzham_realloc_func pRealloc, lzham_msize_func pMSize, void* pUser_data);

   // lzham_flush_t must map directly to the zlib-style API flush types (LZHAM_Z_NO_FLUSH, etc.)
   typedef enum
   {
      LZHAM_NO_FLUSH = 0,
      LZHAM_SYNC_FLUSH = 2,
      LZHAM_FULL_FLUSH = 3,
      LZHAM_FINISH = 4,
      LZHAM_TABLE_FLUSH = 10
   } lzham_flush_t;

   // Compression (keep in sync with g_num_lzx_position_slots[])
   #define LZHAM_MIN_DICT_SIZE_LOG2 15
   #define LZHAM_MAX_DICT_SIZE_LOG2_X86 26
   #define LZHAM_MAX_DICT_SIZE_LOG2_X64 29

   #define LZHAM_MAX_HELPER_THREADS 64

   typedef enum
   {
      LZHAM_COMP_STATUS_NOT_FINISHED = 0,
      LZHAM_COMP_STATUS_NEEDS_MORE_INPUT,
      LZHAM_COMP_STATUS_HAS_MORE_OUTPUT,

      // All the following enums must indicate failure/success.

      LZHAM_COMP_STATUS_FIRST_SUCCESS_OR_FAILURE_CODE,
      LZHAM_COMP_STATUS_SUCCESS = LZHAM_COMP_STATUS_FIRST_SUCCESS_OR_FAILURE_CODE,

      LZHAM_COMP_STATUS_FIRST_FAILURE_CODE,
      LZHAM_COMP_STATUS_FAILED = LZHAM_COMP_STATUS_FIRST_FAILURE_CODE,
      LZHAM_COMP_STATUS_FAILED_INITIALIZING,
      LZHAM_COMP_STATUS_INVALID_PARAMETER,
      LZHAM_COMP_STATUS_OUTPUT_BUF_TOO_SMALL,

      LZHAM_COMP_STATUS_FORCE_DWORD = 0xFFFFFFFF
   } lzham_compress_status_t;

   typedef enum 
   {
      LZHAM_COMP_LEVEL_FASTEST = 0,
      LZHAM_COMP_LEVEL_FASTER,
      LZHAM_COMP_LEVEL_DEFAULT,
      LZHAM_COMP_LEVEL_BETTER,
      LZHAM_COMP_LEVEL_UBER,

      LZHAM_TOTAL_COMP_LEVELS,

      LZHAM_COMP_LEVEL_FORCE_DWORD = 0xFFFFFFFF
   } lzham_compress_level;

   // Streaming compression
   typedef void *lzham_compress_state_ptr;

   typedef enum
   {
      LZHAM_COMP_FLAG_EXTREME_PARSING = 2,         // Improves ratio by allowing the compressor's parse graph to grow "higher" (up to 4 parent nodes per output node), but is much slower.
      LZHAM_COMP_FLAG_DETERMINISTIC_PARSING = 4,   // Guarantees that the compressed output will always be the same given the same input and parameters (no variation between runs due to kernel threading scheduling).

      // If enabled, the compressor is free to use any optimizations which could lower the decompression rate (such
      // as adaptively resetting the Huffman table update rate to maximum frequency, which is costly for the decompressor).
      LZHAM_COMP_FLAG_TRADEOFF_DECOMPRESSION_RATE_FOR_COMP_RATIO = 16,
      
      LZHAM_COMP_FLAG_WRITE_ZLIB_STREAM = 32,
      
      LZHAM_COMP_FLAG_FORCE_SINGLE_THREADED_PARSING = 64,

      LZHAM_COMP_FLAG_USE_LOW_MEMORY_MATCH_FINDER = 128,

   } lzham_compress_flags;
		
	typedef enum 
	{
		LZHAM_INSANELY_SLOW_TABLE_UPDATE_RATE = 1, // 1=insanely slow decompression, here for reference, use 2!
		LZHAM_SLOWEST_TABLE_UPDATE_RATE = 2,
		LZHAM_DEFAULT_TABLE_UPDATE_RATE = 8,
		LZHAM_FASTEST_TABLE_UPDATE_RATE = 20
	} lzham_table_update_rate;

   #define LZHAM_EXTREME_PARSING_MAX_BEST_ARRIVALS_MIN (2)
   
   // LZHAM_EXTREME_PARSING_MAX_BEST_ARRIVALS_MAX can be increased by the user (if you recompile lzham), I've tried up to 64.
   #define LZHAM_EXTREME_PARSING_MAX_BEST_ARRIVALS_MAX (8)

   #define LZHAM_MIN_FAST_BYTES (8)
   #define LZHAM_MAX_FAST_BYTES (258)

	// Compression parameters struct.
	// IMPORTANT: The values of m_dict_size_log2, m_table_update_rate, m_table_max_update_interval, and m_table_update_interval_slow_rate MUST
	// match during compression and decompression. The codec does not verify these values for you, if you don't use the same settings during
	// decompression it will fail (usually with a LZHAM_DECOMP_STATUS_FAILED_BAD_CODE error).
	// The seed buffer's contents and size must match the seed buffer used during decompression.
   typedef struct
   {
      lzham_uint32 m_struct_size;            // set to sizeof(lzham_compress_params)
      lzham_uint32 m_dict_size_log2;         // set to the log2(dictionary_size), must range between [LZHAM_MIN_DICT_SIZE_LOG2, LZHAM_MAX_DICT_SIZE_LOG2_X86] for x86 LZHAM_MAX_DICT_SIZE_LOG2_X64 for x64
      lzham_compress_level m_level;          // set to LZHAM_COMP_LEVEL_FASTEST, etc.
		lzham_uint32 m_table_update_rate;		// Controls tradeoff between ratio and decompression throughput. 0=default, or [1,LZHAM_MAX_TABLE_UPDATE_RATE], higher=faster but lower ratio.
      lzham_int32 m_max_helper_threads;      // max # of additional "helper" threads to create, must range between [-1,LZHAM_MAX_HELPER_THREADS], where -1=max practical
      lzham_uint32 m_compress_flags;         // optional compression flags (see lzham_compress_flags enum)
      lzham_uint32 m_num_seed_bytes;         // for delta compression (optional) - number of seed bytes pointed to by m_pSeed_bytes
      const void *m_pSeed_bytes;             // for delta compression (optional) - pointer to seed bytes buffer, must be at least m_num_seed_bytes long
						      
      // Advanced settings - set to 0 if you don't care.
		// m_table_max_update_interval/m_table_update_interval_slow_rate override m_table_update_rate and allow finer control over the table update settings.
		// If either are non-zero they will override whatever m_table_update_rate is set to. Just leave them 0 unless you are specifically customizing them for your data.
						
		// def=0, typical range 12-128 (LZHAM_DEFAULT_TABLE_UPDATE_RATE=64), controls the max interval between table updates, higher=longer max interval (faster decode/lower ratio). Was 16 in prev. releases.
		lzham_uint32 m_table_max_update_interval;
		// def=0, 32 or higher (LZHAM_DEFAULT_TABLE_UPDATE_RATE=64), scaled by 32, controls the slowing of the update update freq, higher=more rapid slowing (faster decode/lower ratio). Was 40 in prev. releases.
		lzham_uint32 m_table_update_interval_slow_rate;

      // If non-zero, must range between LZHAM_EXTREME_PARSING_MAX_BEST_ARRIVALS_MIN and LZHAM_EXTREME_PARSING_MAX_BEST_ARRIVALS_MAX.
      // Field added in version 0x1011
      lzham_uint32 m_extreme_parsing_max_best_arrivals;

      // If non-zero, must range between LZHAM_MIN_FAST_BYTES-LZHAM_MAX_FAST_BYTES.
      // If this is 0, the compressor will either use a fast_bytes setting controlled by m_level, or a for extreme parsing a fixed setting of LZHAM_EXTREME_PARSING_FAST_BYTES (96).
      // Field added in version 0x1011
      lzham_uint32 m_fast_bytes;

   } lzham_compress_params;
   
   // Initializes a compressor. Returns a pointer to the compressor's internal state, or NULL on failure.
   // pParams cannot be NULL. Be sure to initialize the pParams->m_struct_size member to sizeof(lzham_compress_params) (along with the other members to reasonable values) before calling this function.
   // TODO: With large dictionaries this function could take a while (due to memory allocation). I need to add a reinit() API for compression (decompression already has one).
   LZHAM_DLL_EXPORT lzham_compress_state_ptr LZHAM_CDECL lzham_compress_init(const lzham_compress_params *pParams);

   LZHAM_DLL_EXPORT lzham_compress_state_ptr LZHAM_CDECL lzham_compress_reinit(lzham_compress_state_ptr pState);

   // Deinitializes a compressor, releasing all allocated memory.
   // returns adler32 of source data (valid only on success).
   LZHAM_DLL_EXPORT lzham_uint32 LZHAM_CDECL lzham_compress_deinit(lzham_compress_state_ptr pState);

   // Compresses an arbitrarily sized block of data, writing as much available compressed data as possible to the output buffer. 
   // This method may be called as many times as needed, but for best perf. try not to call it with tiny buffers.
   // pState - Pointer to internal compression state, created by lzham_compress_init.
   // pIn_buf, pIn_buf_size - Pointer to input data buffer, and pointer to a size_t containing the number of bytes available in this buffer. 
   //                         On return, *pIn_buf_size will be set to the number of bytes read from the buffer.
   // pOut_buf, pOut_buf_size - Pointer to the output data buffer, and a pointer to a size_t containing the max number of bytes that can be written to this buffer.
   //                         On return, *pOut_buf_size will be set to the number of bytes written to this buffer.
   // no_more_input_bytes_flag - Set to true to indicate that no more input bytes are available to compress (EOF). Once you call this function with this param set to true, it must stay set to true in all future calls.
   //
   // Normal return status codes:
   //    LZHAM_COMP_STATUS_NOT_FINISHED - Compression can continue, but the compressor needs more input, or it needs more room in the output buffer.
   //    LZHAM_COMP_STATUS_NEEDS_MORE_INPUT - Compression can contintue, but the compressor has no more output, and has no input but we're not at EOF. Supply more input to continue.
   // Success/failure return status codes:
   //    LZHAM_COMP_STATUS_SUCCESS - Compression has completed successfully.
   //    LZHAM_COMP_STATUS_FAILED, LZHAM_COMP_STATUS_FAILED_INITIALIZING, LZHAM_COMP_STATUS_INVALID_PARAMETER - Something went wrong.
   LZHAM_DLL_EXPORT lzham_compress_status_t LZHAM_CDECL lzham_compress(
      lzham_compress_state_ptr pState,
      const lzham_uint8 *pIn_buf, size_t *pIn_buf_size,
      lzham_uint8 *pOut_buf, size_t *pOut_buf_size,
      lzham_bool no_more_input_bytes_flag);

   LZHAM_DLL_EXPORT lzham_compress_status_t LZHAM_CDECL lzham_compress2(
      lzham_compress_state_ptr pState,
      const lzham_uint8 *pIn_buf, size_t *pIn_buf_size,
      lzham_uint8 *pOut_buf, size_t *pOut_buf_size,
      lzham_flush_t flush_type);

   // Single function call compression interface.
   // Same return codes as lzham_compress, except this function can also return LZHAM_COMP_STATUS_OUTPUT_BUF_TOO_SMALL.
   LZHAM_DLL_EXPORT lzham_compress_status_t LZHAM_CDECL lzham_compress_memory(
      const lzham_compress_params *pParams,
      lzham_uint8* pDst_buf,
      size_t *pDst_len,
      const lzham_uint8* pSrc_buf,
      size_t src_len,
      lzham_uint32 *pAdler32);

   // Decompression
   typedef enum
   {
      // LZHAM_DECOMP_STATUS_NOT_FINISHED indicates that the decompressor is flushing its internal buffer to the caller's output buffer. 
      // There may be more bytes available to decompress on the next call, but there is no guarantee.
      LZHAM_DECOMP_STATUS_NOT_FINISHED = 0,

      // LZHAM_DECOMP_STATUS_HAS_MORE_OUTPUT indicates that the decompressor is trying to flush its internal buffer to the caller's output buffer, 
      // but the caller hasn't provided any space to copy this data to the caller's output buffer. Call the lzham_decompress() again with a non-empty sized output buffer.
      LZHAM_DECOMP_STATUS_HAS_MORE_OUTPUT,

      // LZHAM_DECOMP_STATUS_NEEDS_MORE_INPUT indicates that the decompressor has consumed all input bytes, has not encountered an "end of stream" code, 
      // and the caller hasn't set no_more_input_bytes_flag to true, so it's expecting more input to proceed.
      LZHAM_DECOMP_STATUS_NEEDS_MORE_INPUT,

      // All the following enums always (and MUST) indicate failure/success.
      LZHAM_DECOMP_STATUS_FIRST_SUCCESS_OR_FAILURE_CODE,

      // LZHAM_DECOMP_STATUS_SUCCESS indicates decompression has successfully completed.
      LZHAM_DECOMP_STATUS_SUCCESS = LZHAM_DECOMP_STATUS_FIRST_SUCCESS_OR_FAILURE_CODE,

      // The remaining status codes indicate a failure of some sort. Most failures are unrecoverable. TODO: Document which codes are recoverable.
      LZHAM_DECOMP_STATUS_FIRST_FAILURE_CODE,

      LZHAM_DECOMP_STATUS_FAILED_INITIALIZING = LZHAM_DECOMP_STATUS_FIRST_FAILURE_CODE,
      LZHAM_DECOMP_STATUS_FAILED_DEST_BUF_TOO_SMALL,
      LZHAM_DECOMP_STATUS_FAILED_EXPECTED_MORE_RAW_BYTES,
      LZHAM_DECOMP_STATUS_FAILED_BAD_CODE,
      LZHAM_DECOMP_STATUS_FAILED_ADLER32,
      LZHAM_DECOMP_STATUS_FAILED_BAD_RAW_BLOCK,
      LZHAM_DECOMP_STATUS_FAILED_BAD_COMP_BLOCK_SYNC_CHECK,
      LZHAM_DECOMP_STATUS_FAILED_BAD_ZLIB_HEADER,
      LZHAM_DECOMP_STATUS_FAILED_NEED_SEED_BYTES,
      LZHAM_DECOMP_STATUS_FAILED_BAD_SEED_BYTES,
      LZHAM_DECOMP_STATUS_FAILED_BAD_SYNC_BLOCK,
      LZHAM_DECOMP_STATUS_INVALID_PARAMETER,
   } lzham_decompress_status_t;
   
   typedef void *lzham_decompress_state_ptr;

   typedef enum
   {
      LZHAM_DECOMP_FLAG_OUTPUT_UNBUFFERED = 1,
      LZHAM_DECOMP_FLAG_COMPUTE_ADLER32 = 2,
      LZHAM_DECOMP_FLAG_READ_ZLIB_STREAM = 4,
   } lzham_decompress_flags;

   // Decompression parameters structure.
   // Notes: 
   // m_dict_size_log2 MUST match the value used during compression!
   // If m_num_seed_bytes != 0, LZHAM_DECOMP_FLAG_OUTPUT_UNBUFFERED must not be set (i.e. static "seed" dictionaries are not compatible with unbuffered decompression).
   // The seed buffer's contents and size must match the seed buffer used during compression.
   // m_table_update_rate (or m_table_max_update_interval/m_table_update_interval_slow_rate) MUST match the values used for compression!
   typedef struct
   {
      lzham_uint32 m_struct_size;            // set to sizeof(lzham_decompress_params)
      lzham_uint32 m_dict_size_log2;         // set to the log2(dictionary_size), must range between [LZHAM_MIN_DICT_SIZE_LOG2, LZHAM_MAX_DICT_SIZE_LOG2_X86] for x86 LZHAM_MAX_DICT_SIZE_LOG2_X64 for x64
		lzham_uint32 m_table_update_rate;		// Controls tradeoff between ratio and decompression throughput. 0=default, or [1,LZHAM_MAX_TABLE_UPDATE_RATE], higher=faster but lower ratio.
      lzham_uint32 m_decompress_flags;       // optional decompression flags (see lzham_decompress_flags enum)
      lzham_uint32 m_num_seed_bytes;         // for delta compression (optional) - number of seed bytes pointed to by m_pSeed_bytes
      const void *m_pSeed_bytes;             // for delta compression (optional) - pointer to seed bytes buffer, must be at least m_num_seed_bytes long

      // Advanced settings - set to 0 if you don't care.
		// m_table_max_update_interval/m_table_update_interval_slow_rate override m_table_update_rate and allow finer control over the table update settings.
		// If either are non-zero they will override whatever m_table_update_rate is set to. Just leave them 0 unless you are specifically customizing them for your data.

      // def=0, typical range 12-128 (LZHAM_DEFAULT_TABLE_UPDATE_RATE=64), controls the max interval between table updates, higher=longer max interval (faster decode/lower ratio). Was 16 in prev. releases.
      lzham_uint32 m_table_max_update_interval;
      // def=0, 32 or higher (LZHAM_DEFAULT_TABLE_UPDATE_RATE=64), scaled by 32, controls the slowing of the update update freq, higher=more rapid slowing (faster decode/lower ratio). Was 40 in prev. releases.
      lzham_uint32 m_table_update_interval_slow_rate;

   } lzham_decompress_params;
   
   // Initializes a decompressor.
   // pParams cannot be NULL. Be sure to initialize the pParams->m_struct_size member to sizeof(lzham_decompress_params) (along with the other members to reasonable values) before calling this function.
   // Note: With large dictionaries this function could take a while (due to memory allocation). To serially decompress multiple streams, it's faster to init a compressor once and 
   // reuse it using by calling lzham_decompress_reinit().
   LZHAM_DLL_EXPORT lzham_decompress_state_ptr LZHAM_CDECL lzham_decompress_init(const lzham_decompress_params *pParams);

   // Quickly re-initializes the decompressor to its initial state given an already allocated/initialized state (doesn't do any memory alloc unless necessary).
   LZHAM_DLL_EXPORT lzham_decompress_state_ptr LZHAM_CDECL lzham_decompress_reinit(lzham_decompress_state_ptr pState, const lzham_decompress_params *pParams);

   // Deinitializes a decompressor.
   // returns adler32 of decompressed data if compute_adler32 was true, otherwise it returns the adler32 from the compressed stream.
   LZHAM_DLL_EXPORT lzham_uint32 LZHAM_CDECL lzham_decompress_deinit(lzham_decompress_state_ptr pState);

   // Decompresses an arbitrarily sized block of compressed data, writing as much available decompressed data as possible to the output buffer. 
   // This method is implemented as a coroutine so it may be called as many times as needed. However, for best perf. try not to call it with tiny buffers.
   // pState - Pointer to internal decompression state, originally created by lzham_decompress_init.
   // pIn_buf, pIn_buf_size - Pointer to input data buffer, and pointer to a size_t containing the number of bytes available in this buffer. 
   //                         On return, *pIn_buf_size will be set to the number of bytes read from the buffer.
   // pOut_buf, pOut_buf_size - Pointer to the output data buffer, and a pointer to a size_t containing the max number of bytes that can be written to this buffer.
   //                         On return, *pOut_buf_size will be set to the number of bytes written to this buffer.
   // no_more_input_bytes_flag - Set to true to indicate that no more input bytes are available to compress (EOF). Once you call this function with this param set to true, it must stay set to true in all future calls.
   // Notes:
   // In unbuffered mode, the output buffer MUST be large enough to hold the entire decompressed stream. Otherwise, you'll receive the
   //  LZHAM_DECOMP_STATUS_FAILED_DEST_BUF_TOO_SMALL error (which is currently unrecoverable during unbuffered decompression).
   // In buffered mode, if the output buffer's size is 0 bytes, the caller is indicating that no more output bytes are expected from the
   //  decompressor. In this case, if the decompressor actually has more bytes you'll receive the LZHAM_DECOMP_STATUS_HAS_MORE_OUTPUT
   //  error (which is recoverable in the buffered case - just call lzham_decompress() again with a non-zero size output buffer).
   LZHAM_DLL_EXPORT lzham_decompress_status_t LZHAM_CDECL lzham_decompress(
      lzham_decompress_state_ptr pState,
      const lzham_uint8 *pIn_buf, size_t *pIn_buf_size,
      lzham_uint8 *pOut_buf, size_t *pOut_buf_size,
      lzham_bool no_more_input_bytes_flag);

   // Single function call interface.
   LZHAM_DLL_EXPORT lzham_decompress_status_t LZHAM_CDECL lzham_decompress_memory(
      const lzham_decompress_params *pParams,
      lzham_uint8* pDst_buf,
      size_t *pDst_len,
      const lzham_uint8* pSrc_buf,
      size_t src_len,
      lzham_uint32 *pAdler32);

   // ------------------- zlib-style API Definitions.
   
   // Important note: LZHAM doesn't internally support the Deflate algorithm, but for API compatibility the "Deflate" and "Inflate" names are retained here.

   typedef unsigned long lzham_z_ulong;

   // Heap allocation callbacks.
   // Note that lzham_alloc_func parameter types purposely differ from zlib's: items/size is size_t, not unsigned long.
   typedef void *(*lzham_z_alloc_func)(void *opaque, size_t items, size_t size);
   typedef void (*lzham_z_free_func)(void *opaque, void *address);
   typedef void *(*lzham_z_realloc_func)(void *opaque, void *address, size_t items, size_t size);

   #define LZHAM_Z_ADLER32_INIT (1)
   // lzham_adler32() returns the initial adler-32 value to use when called with ptr==NULL.
   LZHAM_DLL_EXPORT lzham_z_ulong lzham_z_adler32(lzham_z_ulong adler, const unsigned char *ptr, size_t buf_len);

   #define LZHAM_Z_CRC32_INIT (0)
   // lzham_crc32() returns the initial CRC-32 value to use when called with ptr==NULL.
   LZHAM_DLL_EXPORT lzham_z_ulong lzham_z_crc32(lzham_z_ulong crc, const unsigned char *ptr, size_t buf_len);

   // Compression strategies.
   enum 
   { 
      LZHAM_Z_DEFAULT_STRATEGY = 0, 
      LZHAM_Z_FILTERED = 1, 
      LZHAM_Z_HUFFMAN_ONLY = 2, 
      LZHAM_Z_RLE = 3, 
      LZHAM_Z_FIXED = 4 
   };

   // Method
   #define LZHAM_Z_DEFLATED 8
   #define LZHAM_Z_LZHAM 14

   #define LZHAM_Z_VERSION          "10.8.1"
   #define LZHAM_Z_VERNUM           0xA810
   #define LZHAM_Z_VER_MAJOR        10
   #define LZHAM_Z_VER_MINOR        8
   #define LZHAM_Z_VER_REVISION     1
   #define LZHAM_Z_VER_SUBREVISION  0

   // Flush values. 
   // For compression, you typically only need to use LZHAM_NO_FLUSH and LZHAM_FINISH. 
   // LZHAM_Z_SYNC_FLUSH and LZHAM_Z_FULL_FLUSH during compression forces compression of all buffered input.
   //
   // For decompression, you typically only need to use LZHAM_Z_SYNC_FLUSH or LZHAM_Z_FINISH. 
   // LZHAM_Z_FINISH during decompression guarantees that the output buffer is large enough to hold all remaining data to decompress.
   // See http://www.bolet.org/~pornin/deflate-flush.html
   // Must directly map to lzham_flush_t
   enum 
   { 
      LZHAM_Z_NO_FLUSH = 0,       // compression/decompression
      LZHAM_Z_PARTIAL_FLUSH = 1,  // compression/decompression, same as LZHAM_Z_SYNC_FLUSH
      LZHAM_Z_SYNC_FLUSH = 2,     // compression/decompression, when compressing: flush current block (if any), always outputs sync block (aligns output to byte boundary, a 0xFFFF0000 marker will appear in the output stream)
      LZHAM_Z_FULL_FLUSH = 3,     // compression/decompression, when compressing: same as LZHAM_Z_SYNC_FLUSH but also forces a full state flush (LZ dictionary, all symbol statistics)
      LZHAM_Z_FINISH = 4,         // compression/decompression
      LZHAM_Z_BLOCK = 5,          // not supported
      LZHAM_Z_TABLE_FLUSH = 10    // compression only, resets all symbol table update rates to maximum frequency (LZHAM extension)
   };

   // Return status codes. LZHAM_Z_PARAM_ERROR is non-standard.
   enum 
   { 
      LZHAM_Z_OK = 0, 
      LZHAM_Z_STREAM_END = 1, 
      LZHAM_Z_NEED_DICT = 2, 
      LZHAM_Z_ERRNO = -1, 
      LZHAM_Z_STREAM_ERROR = -2, 
      LZHAM_Z_DATA_ERROR = -3, 
      LZHAM_Z_MEM_ERROR = -4, 
      LZHAM_Z_BUF_ERROR = -5, 
      LZHAM_Z_VERSION_ERROR = -6, 
      LZHAM_Z_PARAM_ERROR = -10000 
   };

   // Compression levels.
   enum 
   { 
      LZHAM_Z_NO_COMPRESSION = 0,
      LZHAM_Z_BEST_SPEED = 1,
      LZHAM_Z_BEST_COMPRESSION = 9,
      LZHAM_Z_UBER_COMPRESSION = 10,      // uber = best with extreme parsing (can be very slow)
      LZHAM_Z_DEFAULT_COMPRESSION = -1 
   };

   // Window bits
   // Important note: The zlib-style API's default to 32KB dictionary for API compatibility. For improved compression, be sure to call deflateInit2/inflateInit2 and specify larger custom window_bits values!
   // If changing the calling code isn't practical, unremark LZHAM_Z_API_FORCE_WINDOW_BITS.
   #define LZHAM_Z_DEFAULT_WINDOW_BITS 15

   // Define LZHAM_Z_API_FORCE_WINDOW_BITS to force the entire library to use a constant value for window_bits (helps with porting) in all zlib API's.
   // TODO: Might be useful to provide an API to control this at runtime.
   //#define LZHAM_Z_API_FORCE_WINDOW_BITS 23

   // Data types
   #define LZHAM_Z_BINARY              0
   #define LZHAM_Z_TEXT                1
   #define LZHAM_Z_ASCII               1
   #define LZHAM_Z_UNKNOWN             2
   
   struct lzham_z_internal_state;

   // Compression/decompression stream struct.
   typedef struct
   {
     const unsigned char *next_in;           // pointer to next byte to read
     unsigned int avail_in;                  // number of bytes available at next_in
     lzham_z_ulong total_in;                 // total number of bytes consumed so far

     unsigned char *next_out;                // pointer to next byte to write
     unsigned int avail_out;                 // number of bytes that can be written to next_out
     lzham_z_ulong total_out;                // total number of bytes produced so far

     char *msg;                              // error msg (unused)
     struct lzham_z_internal_state *state;   // internal state, allocated by zalloc/zfree

     // LZHAM does not support per-stream heap callbacks. Use lzham_set_memory_callbacks() instead.
     // These members are ignored - they are here for backwards compatibility with zlib.
     lzham_z_alloc_func zalloc;              // optional heap allocation function (defaults to malloc)
     lzham_z_free_func zfree;                // optional heap free function (defaults to free)
     void *opaque;                           // heap alloc function user pointer

     int data_type;                          // data_type (unused)
     lzham_z_ulong adler;                    // adler32 of the source or uncompressed data
     lzham_z_ulong reserved;                 // not used
   } lzham_z_stream;

   typedef lzham_z_stream *lzham_z_streamp;

   LZHAM_DLL_EXPORT const char *lzham_z_version(void);

   // lzham_deflateInit() initializes a compressor with default options:
   // Parameters:
   //  pStream must point to an initialized lzham_stream struct.
   //  level must be between [LZHAM_NO_COMPRESSION, LZHAM_BEST_COMPRESSION].
   //  level 1 enables a specially optimized compression function that's been optimized purely for performance, not ratio.
   // Return values:
   //  LZHAM_OK on success.
   //  LZHAM_STREAM_ERROR if the stream is bogus.
   //  LZHAM_PARAM_ERROR if the input parameters are bogus.
   //  LZHAM_MEM_ERROR on out of memory.
   LZHAM_DLL_EXPORT int lzham_z_deflateInit(lzham_z_streamp pStream, int level);

   // lzham_deflateInit2() is like lzham_deflate(), except with more control:
   // Additional parameters:
   //   method must be LZHAM_Z_DEFLATED or LZHAM_Z_LZHAM (LZHAM_Z_DEFLATED will be internally converted to LZHAM_Z_LZHAM with a windowsize of LZHAM_Z_DEFAULT_WINDOW_BITS)
   //   window_bits must be LZHAM_DEFAULT_WINDOW_BITS (to wrap the deflate stream with zlib header/adler-32 footer) or -LZHAM_Z_DEFAULT_WINDOW_BITS (raw deflate/no header or footer)
   //   mem_level must be between [1, 9] (it's checked but ignored by lzham)
   LZHAM_DLL_EXPORT int lzham_z_deflateInit2(lzham_z_streamp pStream, int level, int method, int window_bits, int mem_level, int strategy);
      
   // Quickly resets a compressor without having to reallocate anything. Same as calling lzham_z_deflateEnd() followed by lzham_z_deflateInit()/lzham_z_deflateInit2().
   LZHAM_DLL_EXPORT int lzham_z_deflateReset(lzham_z_streamp pStream);

   // lzham_deflate() compresses the input to output, consuming as much of the input and producing as much output as possible.
   // Parameters:
   //   pStream is the stream to read from and write to. You must initialize/update the next_in, avail_in, next_out, and avail_out members.
   //   flush may be LZHAM_Z_NO_FLUSH, LZHAM_Z_PARTIAL_FLUSH/LZHAM_Z_SYNC_FLUSH, LZHAM_Z_FULL_FLUSH, or LZHAM_Z_FINISH.
   // Return values:
   //   LZHAM_Z_OK on success (when flushing, or if more input is needed but not available, and/or there's more output to be written but the output buffer is full).
   //   LZHAM_Z_STREAM_END if all input has been consumed and all output bytes have been written. Don't call lzham_z_deflate() on the stream anymore.
   //   LZHAM_Z_STREAM_ERROR if the stream is bogus.
   //   LZHAM_Z_PARAM_ERROR if one of the parameters is invalid.
   //   LZHAM_Z_BUF_ERROR if no forward progress is possible because the input and/or output buffers are empty. (Fill up the input buffer or free up some output space and try again.)
   LZHAM_DLL_EXPORT int lzham_z_deflate(lzham_z_streamp pStream, int flush);

   // lzham_deflateEnd() deinitializes a compressor:
   // Return values:
   //  LZHAM_Z_OK on success.
   //  LZHAM_Z_STREAM_ERROR if the stream is bogus.
   LZHAM_DLL_EXPORT int lzham_z_deflateEnd(lzham_z_streamp pStream);

   // lzham_deflateBound() returns a (very) conservative upper bound on the amount of data that could be generated by lzham_z_deflate(), assuming flush is set to only LZHAM_Z_NO_FLUSH or LZHAM_Z_FINISH.
   LZHAM_DLL_EXPORT lzham_z_ulong lzham_z_deflateBound(lzham_z_streamp pStream, lzham_z_ulong source_len);

   // Single-call compression functions lzham_z_compress() and lzham_z_compress2():
   // Returns LZHAM_Z_OK on success, or one of the error codes from lzham_z_deflate() on failure.
   LZHAM_DLL_EXPORT int lzham_z_compress(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len);
   LZHAM_DLL_EXPORT int lzham_z_compress2(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len, int level);

   // lzham_z_compressBound() returns a (very) conservative upper bound on the amount of data that could be generated by calling lzham_z_compress().
   LZHAM_DLL_EXPORT lzham_z_ulong lzham_z_compressBound(lzham_z_ulong source_len);

   // Initializes a decompressor.
   LZHAM_DLL_EXPORT int lzham_z_inflateInit(lzham_z_streamp pStream);

   // lzham_z_inflateInit2() is like lzham_z_inflateInit() with an additional option that controls the window size and whether or not the stream has been wrapped with a zlib header/footer:
   // window_bits must be LZHAM_Z_DEFAULT_WINDOW_BITS (to parse zlib header/footer) or -LZHAM_Z_DEFAULT_WINDOW_BITS (raw stream with no zlib header/footer).
   LZHAM_DLL_EXPORT int lzham_z_inflateInit2(lzham_z_streamp pStream, int window_bits);

   LZHAM_DLL_EXPORT int lzham_z_inflateReset(lzham_z_streamp pStream);

   // Decompresses the input stream to the output, consuming only as much of the input as needed, and writing as much to the output as possible.
   // Parameters:
   //   pStream is the stream to read from and write to. You must initialize/update the next_in, avail_in, next_out, and avail_out members.
   //   flush may be LZHAM_Z_NO_FLUSH, LZHAM_Z_SYNC_FLUSH, or LZHAM_Z_FINISH.
   //   On the first call, if flush is LZHAM_Z_FINISH it's assumed the input and output buffers are both sized large enough to decompress the entire stream in a single call (this is slightly faster).
   //   LZHAM_Z_FINISH implies that there are no more source bytes available beside what's already in the input buffer, and that the output buffer is large enough to hold the rest of the decompressed data.
   // Return values:
   //   LZHAM_Z_OK on success. Either more input is needed but not available, and/or there's more output to be written but the output buffer is full.
   //   LZHAM_Z_STREAM_END if all needed input has been consumed and all output bytes have been written. For zlib streams, the adler-32 of the decompressed data has also been verified.
   //   LZHAM_Z_STREAM_ERROR if the stream is bogus.
   //   LZHAM_Z_DATA_ERROR if the deflate stream is invalid.
   //   LZHAM_Z_PARAM_ERROR if one of the parameters is invalid.
   //   LZHAM_Z_BUF_ERROR if no forward progress is possible because the input buffer is empty but the inflater needs more input to continue, or if the output buffer is not large enough. Call lzham_inflate() again
   //   with more input data, or with more room in the output buffer (except when using single call decompression, described above).
   LZHAM_DLL_EXPORT int lzham_z_inflate(lzham_z_streamp pStream, int flush);

   // Deinitializes a decompressor.
   LZHAM_DLL_EXPORT int lzham_z_inflateEnd(lzham_z_streamp pStream);

   // Single-call decompression.
   // Returns LZHAM_OK on success, or one of the error codes from lzham_inflate() on failure.
   LZHAM_DLL_EXPORT int lzham_z_uncompress(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len);

   // Returns a string description of the specified error code, or NULL if the error code is invalid.
   LZHAM_DLL_EXPORT const char *lzham_z_error(int err);

   // Redefine zlib-compatible names to lzham equivalents, so lzham can be used as a more or less drop-in replacement for the subset of zlib that lzham supports.
   // Define LZHAM_NO_ZLIB_COMPATIBLE_NAMES to disable zlib-compatibility if you use zlib in the same project.
   #ifdef LZHAM_DEFINE_ZLIB_API
      typedef unsigned char Byte;
      typedef unsigned int uInt;
      typedef lzham_z_ulong uLong;
      typedef Byte Bytef;
      typedef uInt uIntf;
      typedef char charf;
      typedef int intf;
      typedef void *voidpf;
      typedef uLong uLongf;
      typedef void *voidp;
      typedef void *const voidpc;
      #define Z_NULL                0
      #define Z_NO_FLUSH            LZHAM_Z_NO_FLUSH
      #define Z_PARTIAL_FLUSH       LZHAM_Z_PARTIAL_FLUSH
      #define Z_SYNC_FLUSH          LZHAM_Z_SYNC_FLUSH
      #define Z_FULL_FLUSH          LZHAM_Z_FULL_FLUSH
      #define Z_FINISH              LZHAM_Z_FINISH
      #define Z_BLOCK               LZHAM_Z_BLOCK
      #define Z_OK                  LZHAM_Z_OK
      #define Z_STREAM_END          LZHAM_Z_STREAM_END
      #define Z_NEED_DICT           LZHAM_Z_NEED_DICT
      #define Z_ERRNO               LZHAM_Z_ERRNO
      #define Z_STREAM_ERROR        LZHAM_Z_STREAM_ERROR
      #define Z_DATA_ERROR          LZHAM_Z_DATA_ERROR
      #define Z_MEM_ERROR           LZHAM_Z_MEM_ERROR
      #define Z_BUF_ERROR           LZHAM_Z_BUF_ERROR
      #define Z_VERSION_ERROR       LZHAM_Z_VERSION_ERROR
      #define Z_PARAM_ERROR         LZHAM_Z_PARAM_ERROR
      #define Z_NO_COMPRESSION      LZHAM_Z_NO_COMPRESSION
      #define Z_BEST_SPEED          LZHAM_Z_BEST_SPEED
      #define Z_BEST_COMPRESSION    LZHAM_Z_BEST_COMPRESSION
      #define Z_DEFAULT_COMPRESSION LZHAM_Z_DEFAULT_COMPRESSION
      #define Z_DEFAULT_STRATEGY    LZHAM_Z_DEFAULT_STRATEGY
      #define Z_FILTERED            LZHAM_Z_FILTERED
      #define Z_HUFFMAN_ONLY        LZHAM_Z_HUFFMAN_ONLY
      #define Z_RLE                 LZHAM_Z_RLE
      #define Z_FIXED               LZHAM_Z_FIXED
      #define Z_DEFLATED            LZHAM_Z_DEFLATED
      #define Z_DEFAULT_WINDOW_BITS LZHAM_Z_DEFAULT_WINDOW_BITS
      #define alloc_func            lzham_z_alloc_func
      #define free_func             lzham_z_free_func
      #define internal_state        lzham_z_internal_state
      #define z_stream              lzham_z_stream
      #define deflateInit           lzham_z_deflateInit
      #define deflateInit2          lzham_z_deflateInit2
      #define deflateReset          lzham_z_deflateReset
      #define deflate               lzham_z_deflate
      #define deflateEnd            lzham_z_deflateEnd
      #define deflateBound          lzham_z_deflateBound
      #define compress              lzham_z_compress
      #define compress2             lzham_z_compress2
      #define compressBound         lzham_z_compressBound
      #define inflateInit           lzham_z_inflateInit
      #define inflateInit2          lzham_z_inflateInit2
      #define inflateReset          lzham_z_inflateReset
      #define inflate               lzham_z_inflate
      #define inflateEnd            lzham_z_inflateEnd
      #define uncompress            lzham_z_uncompress
      #define crc32                 lzham_z_crc32
      #define adler32               lzham_z_adler32
      #define MAX_WBITS             26
      #define MAX_MEM_LEVEL         9
      #define zError                lzham_z_error
      #define ZLIB_VERSION          LZHAM_Z_VERSION
      #define ZLIB_VERNUM           LZHAM_Z_VERNUM
      #define ZLIB_VER_MAJOR        LZHAM_Z_VER_MAJOR
      #define ZLIB_VER_MINOR        LZHAM_Z_VER_MINOR
      #define ZLIB_VER_REVISION     LZHAM_Z_VER_REVISION
      #define ZLIB_VER_SUBREVISION  LZHAM_Z_VER_SUBREVISION
      #define zlibVersion           lzham_z_version
      #define zlib_version          lzham_z_version()
      #define Z_BINARY              LZHAM_Z_BINARY
      #define Z_TEXT                LZHAM_Z_TEST
      #define Z_ASCII               LZHAM_Z_ASCII
      #define Z_UNKNOWN             LZHAM_Z_UNKNOWN
   #endif // #ifdef LZHAM_DEFINE_ZLIB_API

   // Exported function typedefs, to simplify loading the LZHAM DLL dynamically.
   typedef lzham_uint32 (LZHAM_CDECL *lzham_get_version_func)(void);
   typedef void (LZHAM_CDECL *lzham_set_memory_callbacks_func)(lzham_realloc_func pRealloc, lzham_msize_func pMSize, void* pUser_data);

   typedef lzham_compress_state_ptr (LZHAM_CDECL *lzham_compress_init_func)(const lzham_compress_params *pParams);
   typedef lzham_compress_state_ptr (LZHAM_CDECL *lzham_compress_reinit_func)(lzham_compress_state_ptr pState);
   typedef lzham_uint32 (LZHAM_CDECL *lzham_compress_deinit_func)(lzham_compress_state_ptr pState);
   typedef lzham_compress_status_t (LZHAM_CDECL *lzham_compress_func)(lzham_compress_state_ptr pState, const lzham_uint8 *pIn_buf, size_t *pIn_buf_size, lzham_uint8 *pOut_buf, size_t *pOut_buf_size, lzham_bool no_more_input_bytes_flag);
   typedef lzham_compress_status_t (LZHAM_CDECL *lzham_compress2_func)(lzham_compress_state_ptr pState, const lzham_uint8 *pIn_buf, size_t *pIn_buf_size, lzham_uint8 *pOut_buf, size_t *pOut_buf_size, lzham_flush_t flush_type);
   typedef lzham_compress_status_t (LZHAM_CDECL *lzham_compress_memory_func)(const lzham_compress_params *pParams, lzham_uint8* pDst_buf, size_t *pDst_len, const lzham_uint8* pSrc_buf, size_t src_len, lzham_uint32 *pAdler32);

   typedef lzham_decompress_state_ptr (LZHAM_CDECL *lzham_decompress_init_func)(const lzham_decompress_params *pParams);
   typedef lzham_decompress_state_ptr (LZHAM_CDECL *lzham_decompress_reinit_func)(lzham_compress_state_ptr pState, const lzham_decompress_params *pParams);
   typedef lzham_uint32 (LZHAM_CDECL *lzham_decompress_deinit_func)(lzham_decompress_state_ptr pState);
   typedef lzham_decompress_status_t (LZHAM_CDECL *lzham_decompress_func)(lzham_decompress_state_ptr pState, const lzham_uint8 *pIn_buf, size_t *pIn_buf_size, lzham_uint8 *pOut_buf, size_t *pOut_buf_size, lzham_bool no_more_input_bytes_flag);
   typedef lzham_decompress_status_t (LZHAM_CDECL *lzham_decompress_memory_func)(const lzham_decompress_params *pParams, lzham_uint8* pDst_buf, size_t *pDst_len, const lzham_uint8* pSrc_buf, size_t src_len, lzham_uint32 *pAdler32);

   typedef const char *(LZHAM_CDECL *lzham_z_version_func)(void);
   typedef int (LZHAM_CDECL *lzham_z_deflateInit_func)(lzham_z_streamp pStream, int level);
   typedef int (LZHAM_CDECL *lzham_z_deflateInit2_func)(lzham_z_streamp pStream, int level, int method, int window_bits, int mem_level, int strategy);
   typedef int (LZHAM_CDECL *lzham_z_deflateReset_func)(lzham_z_streamp pStream);
   typedef int (LZHAM_CDECL *lzham_z_deflate_func)(lzham_z_streamp pStream, int flush);
   typedef int (LZHAM_CDECL *lzham_z_deflateEnd_func)(lzham_z_streamp pStream);
   typedef lzham_z_ulong (LZHAM_CDECL *lzham_z_deflateBound_func)(lzham_z_streamp pStream, lzham_z_ulong source_len);
   typedef int (LZHAM_CDECL *lzham_z_compress_func)(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len);
   typedef int (LZHAM_CDECL *lzham_z_compress2_func)(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len, int level);
   typedef lzham_z_ulong (LZHAM_CDECL *lzham_z_compressBound_func)(lzham_z_ulong source_len);
   typedef int (LZHAM_CDECL *lzham_z_inflateInit_func)(lzham_z_streamp pStream);
   typedef int (LZHAM_CDECL *lzham_z_inflateInit2_func)(lzham_z_streamp pStream, int window_bits);
   typedef int (LZHAM_CDECL *lzham_z_inflateReset_func)(lzham_z_streamp pStream);
   typedef int (LZHAM_CDECL *lzham_z_inflate_func)(lzham_z_streamp pStream, int flush);
   typedef int (LZHAM_CDECL *lzham_z_inflateEnd_func)(lzham_z_streamp pStream);
   typedef int (LZHAM_CDECL *lzham_z_uncompress_func)(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len);
   typedef const char *(LZHAM_CDECL *lzham_z_error_func)(int err);
   
#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
// This optional interface is used by the dynamic/static link helpers defined in lzham_dynamic_lib.h and lzham_static_lib.h.
// It allows code to always call LZHAM the same way, independent of how it was linked into the app (statically or dynamically).
class ilzham
{
   ilzham(const ilzham &other);
   ilzham& operator= (const ilzham &rhs);

public:
   ilzham() { clear(); }

   virtual ~ilzham() { }
   virtual bool load() = 0;
   virtual void unload() = 0;
   virtual bool is_loaded() = 0;

   void clear()
   {
      this->lzham_get_version = NULL;
      this->lzham_set_memory_callbacks = NULL;
      
      this->lzham_compress_init = NULL;
      this->lzham_compress_reinit = NULL;
      this->lzham_compress_deinit = NULL;
      this->lzham_compress = NULL;
      this->lzham_compress2 = NULL;
      this->lzham_compress_memory = NULL;
      
      this->lzham_decompress_init = NULL;
      this->lzham_decompress_reinit = NULL;
      this->lzham_decompress_deinit = NULL;
      this->lzham_decompress = NULL;
      this->lzham_decompress_memory = NULL;

      this->lzham_z_version = NULL;
      this->lzham_z_deflateInit = NULL;
      this->lzham_z_deflateInit2 = NULL;
      this->lzham_z_deflateReset = NULL;
      this->lzham_z_deflate = NULL;
      this->lzham_z_deflateEnd = NULL;
      this->lzham_z_deflateBound = NULL;
      this->lzham_z_compress = NULL;
      this->lzham_z_compress2 = NULL;
      this->lzham_z_compressBound = NULL;
      this->lzham_z_inflateInit = NULL;
      this->lzham_z_inflateInit2 = NULL;
      this->lzham_z_inflate = NULL;
      this->lzham_z_inflateEnd = NULL;
      this->lzham_z_inflateReset = NULL;
      this->lzham_z_uncompress = NULL;
      this->lzham_z_error = NULL;
   }

   lzham_get_version_func           lzham_get_version;
   lzham_set_memory_callbacks_func  lzham_set_memory_callbacks;
   
   lzham_compress_init_func         lzham_compress_init;
   lzham_compress_reinit_func       lzham_compress_reinit;
   lzham_compress_deinit_func       lzham_compress_deinit;
   lzham_compress_func              lzham_compress;
   lzham_compress2_func             lzham_compress2;
   lzham_compress_memory_func       lzham_compress_memory;

   lzham_decompress_init_func       lzham_decompress_init;
   lzham_decompress_reinit_func     lzham_decompress_reinit;
   lzham_decompress_deinit_func     lzham_decompress_deinit;
   lzham_decompress_func            lzham_decompress;
   lzham_decompress_memory_func     lzham_decompress_memory;

   lzham_z_version_func             lzham_z_version;
   lzham_z_deflateInit_func         lzham_z_deflateInit;
   lzham_z_deflateInit2_func        lzham_z_deflateInit2;
   lzham_z_deflateReset_func        lzham_z_deflateReset;
   lzham_z_deflate_func             lzham_z_deflate;
   lzham_z_deflateEnd_func          lzham_z_deflateEnd;
   lzham_z_deflateBound_func        lzham_z_deflateBound;
   lzham_z_compress_func            lzham_z_compress;
   lzham_z_compress2_func           lzham_z_compress2;
   lzham_z_compressBound_func       lzham_z_compressBound;
   lzham_z_inflateInit_func         lzham_z_inflateInit;
   lzham_z_inflateInit2_func        lzham_z_inflateInit2;
   lzham_z_inflateReset_func        lzham_z_inflateReset;
   lzham_z_inflate_func             lzham_z_inflate;
   lzham_z_inflateEnd_func          lzham_z_inflateEnd;
   lzham_z_uncompress_func          lzham_z_uncompress;
   lzham_z_error_func               lzham_z_error;
};
#endif // #ifdef __cplusplus

#endif // #ifndef __LZHAM_H__

// Copyright (c) 2009-2012 Richard Geldreich, Jr. <richgel99@gmail.com>
//
// LZHAM uses the MIT License:
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.


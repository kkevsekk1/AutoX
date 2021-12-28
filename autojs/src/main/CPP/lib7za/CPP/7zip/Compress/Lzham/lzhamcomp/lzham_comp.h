// File: lzham_comp.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once
#include "lzham.h"

namespace lzham
{
   lzham_compress_state_ptr LZHAM_CDECL lzham_lib_compress_init(const lzham_compress_params *pParams);
   
   lzham_compress_state_ptr LZHAM_CDECL lzham_lib_compress_reinit(lzham_compress_state_ptr p);
   
   lzham_uint32 LZHAM_CDECL lzham_lib_compress_deinit(lzham_compress_state_ptr p);
   
   lzham_compress_status_t LZHAM_CDECL lzham_lib_compress(
      lzham_compress_state_ptr p,
      const lzham_uint8 *pIn_buf, size_t *pIn_buf_size, 
      lzham_uint8 *pOut_buf, size_t *pOut_buf_size,
      lzham_bool no_more_input_bytes_flag);

   lzham_compress_status_t LZHAM_CDECL lzham_lib_compress2(
      lzham_compress_state_ptr p,
      const lzham_uint8 *pIn_buf, size_t *pIn_buf_size, 
      lzham_uint8 *pOut_buf, size_t *pOut_buf_size,
      lzham_flush_t flush_type);
   
   lzham_compress_status_t LZHAM_CDECL lzham_lib_compress_memory(const lzham_compress_params *pParams, lzham_uint8* pDst_buf, size_t *pDst_len, const lzham_uint8* pSrc_buf, size_t src_len, lzham_uint32 *pAdler32);

   int lzham_lib_z_deflateInit(lzham_z_streamp pStream, int level);
   int lzham_lib_z_deflateInit2(lzham_z_streamp pStream, int level, int method, int window_bits, int mem_level, int strategy);
   int lzham_lib_z_deflateReset(lzham_z_streamp pStream);
   int lzham_lib_z_deflate(lzham_z_streamp pStream, int flush);
   int lzham_lib_z_deflateEnd(lzham_z_streamp pStream);
   lzham_z_ulong lzham_lib_z_deflateBound(lzham_z_streamp pStream, lzham_z_ulong source_len);
   int lzham_lib_z_compress2(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len, int level);
   int lzham_lib_z_compress(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len);
   lzham_z_ulong lzham_lib_z_compressBound(lzham_z_ulong source_len);

} // namespace lzham

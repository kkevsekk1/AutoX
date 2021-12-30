// File: lzham_lib.cpp - Static library entrypoints.
// See Copyright Notice and license at the end of include/lzham.h
#include "lzham_core.h"
#include "lzham_decomp.h"
#include "lzham_comp.h"

extern "C" lzham_uint32 LZHAM_CDECL lzham_get_version(void)
{
   return LZHAM_DLL_VERSION;
}

extern "C" void LZHAM_CDECL lzham_set_memory_callbacks(lzham_realloc_func pRealloc, lzham_msize_func pMSize, void* pUser_data)
{
   lzham::lzham_lib_set_memory_callbacks(pRealloc, pMSize, pUser_data);
}

extern "C" lzham_decompress_state_ptr LZHAM_CDECL lzham_decompress_init(const lzham_decompress_params *pParams)
{
   return lzham::lzham_lib_decompress_init(pParams);
}

extern "C" lzham_decompress_state_ptr LZHAM_CDECL lzham_decompress_reinit(lzham_decompress_state_ptr p, const lzham_decompress_params *pParams)
{
   return lzham::lzham_lib_decompress_reinit(p, pParams);
}

extern "C" lzham_uint32 LZHAM_CDECL lzham_decompress_deinit(lzham_decompress_state_ptr p)
{
   return lzham::lzham_lib_decompress_deinit(p);
}

extern "C" lzham_decompress_status_t LZHAM_CDECL lzham_decompress(
   lzham_decompress_state_ptr p,
   const lzham_uint8 *pIn_buf, size_t *pIn_buf_size, 
   lzham_uint8 *pOut_buf, size_t *pOut_buf_size,
   lzham_bool no_more_input_bytes_flag)
{
   return lzham::lzham_lib_decompress(p, pIn_buf, pIn_buf_size, pOut_buf, pOut_buf_size, no_more_input_bytes_flag);
}   

extern "C" lzham_decompress_status_t LZHAM_CDECL lzham_decompress_memory(const lzham_decompress_params *pParams, lzham_uint8* pDst_buf, size_t *pDst_len, const lzham_uint8* pSrc_buf, size_t src_len, lzham_uint32 *pAdler32)
{
   return lzham::lzham_lib_decompress_memory(pParams, pDst_buf, pDst_len, pSrc_buf, src_len, pAdler32);
}

extern "C" lzham_compress_state_ptr LZHAM_CDECL lzham_compress_init(const lzham_compress_params *pParams)
{
   return lzham::lzham_lib_compress_init(pParams);
}

extern "C" lzham_compress_state_ptr LZHAM_CDECL lzham_compress_reinit(lzham_compress_state_ptr p)
{
   return lzham::lzham_lib_compress_reinit(p);
}

extern "C" lzham_uint32 LZHAM_CDECL lzham_compress_deinit(lzham_compress_state_ptr p)
{
   return lzham::lzham_lib_compress_deinit(p);
}

extern "C" lzham_compress_status_t LZHAM_CDECL lzham_compress(
   lzham_compress_state_ptr p,
   const lzham_uint8 *pIn_buf, size_t *pIn_buf_size, 
   lzham_uint8 *pOut_buf, size_t *pOut_buf_size,
   lzham_bool no_more_input_bytes_flag)
{
   return lzham::lzham_lib_compress(p, pIn_buf, pIn_buf_size, pOut_buf, pOut_buf_size, no_more_input_bytes_flag);
}   

extern "C" lzham_compress_status_t LZHAM_CDECL lzham_compress2(
   lzham_compress_state_ptr p,
   const lzham_uint8 *pIn_buf, size_t *pIn_buf_size, 
   lzham_uint8 *pOut_buf, size_t *pOut_buf_size,
   lzham_flush_t flush_type)
{
   return lzham::lzham_lib_compress2(p, pIn_buf, pIn_buf_size, pOut_buf, pOut_buf_size, flush_type);
}   

extern "C" lzham_compress_status_t LZHAM_CDECL lzham_compress_memory(const lzham_compress_params *pParams, lzham_uint8* pDst_buf, size_t *pDst_len, const lzham_uint8* pSrc_buf, size_t src_len, lzham_uint32 *pAdler32)
{
   return lzham::lzham_lib_compress_memory(pParams, pDst_buf, pDst_len, pSrc_buf, src_len, pAdler32);
}

// ----------------- zlib-style API's

extern "C" const char * LZHAM_CDECL lzham_z_version(void)
{
   return LZHAM_Z_VERSION;
}

extern "C" lzham_z_ulong LZHAM_CDECL lzham_z_adler32(lzham_z_ulong adler, const unsigned char *ptr, size_t buf_len)
{
   return lzham::lzham_lib_z_adler32(adler, ptr, buf_len);
}

extern "C" lzham_z_ulong LZHAM_CDECL lzham_z_crc32(lzham_z_ulong crc, const lzham_uint8 *ptr, size_t buf_len)
{
   return lzham::lzham_lib_z_crc32(crc, ptr, buf_len);
}

extern "C" int LZHAM_CDECL lzham_z_deflateInit(lzham_z_streamp pStream, int level)
{
   return lzham::lzham_lib_z_deflateInit(pStream, level);
}

extern "C" int LZHAM_CDECL lzham_z_deflateInit2(lzham_z_streamp pStream, int level, int method, int window_bits, int mem_level, int strategy)
{
   return lzham::lzham_lib_z_deflateInit2(pStream, level, method, window_bits, mem_level, strategy);
}

extern "C" int LZHAM_CDECL lzham_z_deflateReset(lzham_z_streamp pStream)
{
   return lzham::lzham_lib_z_deflateReset(pStream);
}

extern "C" int LZHAM_CDECL lzham_z_deflate(lzham_z_streamp pStream, int flush)
{
   return lzham::lzham_lib_z_deflate(pStream, flush);
}

extern "C" int LZHAM_CDECL lzham_z_deflateEnd(lzham_z_streamp pStream)
{
   return lzham::lzham_lib_z_deflateEnd(pStream);
}

extern "C" lzham_z_ulong LZHAM_CDECL lzham_z_deflateBound(lzham_z_streamp pStream, lzham_z_ulong source_len)
{
   return lzham::lzham_lib_z_deflateBound(pStream, source_len);
}

extern "C" int LZHAM_CDECL lzham_z_compress(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len)
{
   return lzham::lzham_lib_z_compress(pDest, pDest_len, pSource, source_len);
}

extern "C" int LZHAM_CDECL lzham_z_compress2(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len, int level)
{
   return lzham::lzham_lib_z_compress2(pDest, pDest_len, pSource, source_len, level);
}

extern "C" lzham_z_ulong LZHAM_CDECL lzham_z_compressBound(lzham_z_ulong source_len)
{
   return lzham::lzham_lib_z_compressBound(source_len);
}

extern "C" int LZHAM_CDECL lzham_z_inflateInit(lzham_z_streamp pStream)
{
   return lzham::lzham_lib_z_inflateInit(pStream);
}

extern "C" int LZHAM_CDECL lzham_z_inflateInit2(lzham_z_streamp pStream, int window_bits)
{
   return lzham::lzham_lib_z_inflateInit2(pStream, window_bits);
}

extern "C" int LZHAM_CDECL lzham_z_inflateReset(lzham_z_streamp pStream)
{
   return lzham::lzham_lib_z_inflateReset(pStream);
}

extern "C" int LZHAM_CDECL lzham_z_inflate(lzham_z_streamp pStream, int flush)
{
   return lzham::lzham_lib_z_inflate(pStream, flush);
}

extern "C" int LZHAM_CDECL lzham_z_inflateEnd(lzham_z_streamp pStream)
{
   return lzham::lzham_lib_z_inflateEnd(pStream);
}

extern "C" int LZHAM_CDECL lzham_z_uncompress(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len)
{
   return lzham::lzham_lib_z_uncompress(pDest, pDest_len, pSource, source_len);
}

extern "C" const char * LZHAM_CDECL lzham_z_error(int err)
{
   return lzham::lzham_lib_z_error(err);
}

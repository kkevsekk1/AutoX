#pragma once

#define LZHAM_STATIC_LIB 1
#include "lzham.h"

#ifdef __cplusplus
// Like lzham_dynamic_lib, except it sets the function pointer members to point directly to the C functions in lzhamlib
class lzham_static_lib : public ilzham
{
   lzham_static_lib(const lzham_static_lib &other);
   lzham_static_lib& operator= (const lzham_static_lib &rhs);

public:
   lzham_static_lib() : ilzham() { }

   virtual ~lzham_static_lib() { }
   
   virtual bool load()
   {
      this->lzham_get_version = ::lzham_get_version;
      this->lzham_set_memory_callbacks = ::lzham_set_memory_callbacks;
      this->lzham_compress_init = ::lzham_compress_init;
      this->lzham_compress_deinit = ::lzham_compress_deinit;
      this->lzham_compress = ::lzham_compress;
      this->lzham_compress2 = ::lzham_compress2;
      this->lzham_compress_reinit = ::lzham_compress_reinit;
      this->lzham_compress_memory = ::lzham_compress_memory;
      this->lzham_decompress_init = ::lzham_decompress_init;
      this->lzham_decompress_reinit = ::lzham_decompress_reinit;
      this->lzham_decompress_deinit = ::lzham_decompress_deinit;
      this->lzham_decompress = ::lzham_decompress;
      this->lzham_decompress_memory = ::lzham_decompress_memory;

      this->lzham_z_version = ::lzham_z_version;
      this->lzham_z_deflateInit = ::lzham_z_deflateInit;
      this->lzham_z_deflateInit2 = ::lzham_z_deflateInit2;
      this->lzham_z_deflateReset = ::lzham_z_deflateReset;
      this->lzham_z_deflate = ::lzham_z_deflate;
      this->lzham_z_deflateEnd = ::lzham_z_deflateEnd;
      this->lzham_z_deflateBound = ::lzham_z_deflateBound;
      this->lzham_z_compress = ::lzham_z_compress;
      this->lzham_z_compress2 = ::lzham_z_compress2;
      this->lzham_z_compressBound = ::lzham_z_compressBound;
      this->lzham_z_inflateInit = ::lzham_z_inflateInit;
      this->lzham_z_inflateInit2 = ::lzham_z_inflateInit2;
      this->lzham_z_inflate = ::lzham_z_inflate;
      this->lzham_z_inflateEnd = ::lzham_z_inflateEnd;
      this->lzham_z_inflateReset = ::lzham_z_inflateReset;
      this->lzham_z_uncompress = ::lzham_z_uncompress;
      this->lzham_z_error = ::lzham_z_error;

      return true;
   }
   
   virtual void unload() { clear(); }
   
   virtual bool is_loaded() { return lzham_get_version != NULL; }
};
#endif // __cplusplus

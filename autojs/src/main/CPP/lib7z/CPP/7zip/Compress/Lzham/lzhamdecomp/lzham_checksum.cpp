// File: lzham_checksum.cpp
#include "lzham_core.h"
#include "lzham_checksum.h"

namespace lzham
{
   // Originally from the public domain stb.h header.
   uint adler32(const void* pBuf, size_t buflen, uint adler32)
   {
      if (!pBuf)
         return cInitAdler32;

      const uint8* buffer = static_cast<const uint8*>(pBuf);
      
      const unsigned long ADLER_MOD = 65521;
      unsigned long s1 = adler32 & 0xffff, s2 = adler32 >> 16;
      size_t blocklen;
      unsigned long i;

      blocklen = buflen % 5552;
      while (buflen) 
      {
         for (i=0; i + 7 < blocklen; i += 8) 
         {
            s1 += buffer[0], s2 += s1;
            s1 += buffer[1], s2 += s1;
            s1 += buffer[2], s2 += s1;
            s1 += buffer[3], s2 += s1;
            s1 += buffer[4], s2 += s1;
            s1 += buffer[5], s2 += s1;
            s1 += buffer[6], s2 += s1;
            s1 += buffer[7], s2 += s1;

            buffer += 8;
         }

         for (; i < blocklen; ++i)
            s1 += *buffer++, s2 += s1;

         s1 %= ADLER_MOD, s2 %= ADLER_MOD;
         buflen -= blocklen;
         blocklen = 5552;
      }
      return static_cast<uint>((s2 << 16) + s1);
   }

   // Karl Malbrain's compact CRC-32, with pre and post conditioning. 
   // See "A compact CCITT crc16 and crc32 C implementation that balances processor cache usage against speed": 
   // http://www.geocities.com/malbrain/
   static const lzham_uint32 s_crc32[16] = 
   { 
      0x00000000, 0x1db71064, 0x3b6e20c8, 0x26d930ac, 0x76dc4190, 0x6b6b51f4, 0x4db26158, 0x5005713c,
      0xedb88320, 0xf00f9344, 0xd6d6a3e8, 0xcb61b38c, 0x9b64c2b0, 0x86d3d2d4, 0xa00ae278, 0xbdbdf21c 
   };
   
   uint crc32(uint crc, const lzham_uint8 *ptr, size_t buf_len)
   {
      if (!ptr) 
         return cInitCRC32;

      crc = ~crc; 
      while (buf_len--) 
      { 
         lzham_uint8 b = *ptr++; 
         crc = (crc >> 4) ^ s_crc32[(crc & 0xF) ^ (b & 0xF)]; 
         crc = (crc >> 4) ^ s_crc32[(crc & 0xF) ^ (b >> 4)]; 
      }
      return ~crc;
   }

  
} // namespace lzham


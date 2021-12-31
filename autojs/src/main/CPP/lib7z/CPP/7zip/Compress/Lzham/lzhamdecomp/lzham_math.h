// File: lzham_math.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

#if defined(LZHAM_USE_MSVC_INTRINSICS) && !defined(__MINGW32__)
   #include <intrin.h>
   #if defined(_MSC_VER)
      #pragma intrinsic(_BitScanReverse)
   #endif
#endif

namespace lzham
{
   namespace math
   {
      // Yes I know these should probably be pass by ref, not val:
      // http://www.stepanovpapers.com/notes.pdf
      // Just don't use them on non-simple (non built-in) types!
      template<typename T> inline T minimum(T a, T b) { return (a < b) ? a : b; }

      template<typename T> inline T minimum(T a, T b, T c) { return minimum(minimum(a, b), c); }

      template<typename T> inline T maximum(T a, T b) { return (a > b) ? a : b; }

      template<typename T> inline T maximum(T a, T b, T c) { return maximum(maximum(a, b), c); }

      template<typename T> inline T clamp(T value, T low, T high) { return (value < low) ? low : ((value > high) ? high : value); }

      inline bool is_power_of_2(uint32 x) { return x && ((x & (x - 1U)) == 0U); }
      inline bool is_power_of_2(uint64 x) { return x && ((x & (x - 1U)) == 0U); }

      template<typename T> inline T align_up_pointer(T p, uint alignment)
      {
         LZHAM_ASSERT(is_power_of_2(alignment));
         ptr_bits_t q = reinterpret_cast<ptr_bits_t>(p);
         q = (q + alignment - 1) & (~((uint_ptr)alignment - 1));
         return reinterpret_cast<T>(q);
      }

      // From "Hackers Delight"
      // val remains unchanged if it is already a power of 2.
      inline uint32 next_pow2(uint32 val)
      {
         val--;
         val |= val >> 16;
         val |= val >> 8;
         val |= val >> 4;
         val |= val >> 2;
         val |= val >> 1;
         return val + 1;
      }

      // val remains unchanged if it is already a power of 2.
      inline uint64 next_pow2(uint64 val)
      {
         val--;
         val |= val >> 32;
         val |= val >> 16;
         val |= val >> 8;
         val |= val >> 4;
         val |= val >> 2;
         val |= val >> 1;
         return val + 1;
      }

      inline uint floor_log2i(uint v)
      {
         uint l = 0;
         while (v > 1U)
         {
            v >>= 1;
            l++;
         }
         return l;
      }

      inline uint ceil_log2i(uint v)
      {
         uint l = floor_log2i(v);
         if ((l != cIntBits) && (v > (1U << l)))
            l++;
         return l;
      }

      // Returns the total number of bits needed to encode v.
      inline uint total_bits(uint v)
      {
         unsigned long l = 0;
#if defined(__MINGW32__)
         if (v)
         {
            l = 32 -__builtin_clz(v);
         }
#elif defined(LZHAM_USE_MSVC_INTRINSICS)
         if (_BitScanReverse(&l, v))
         {
            l++;
         }
         else
         {
            l = 0;
         }
#else
         while (v > 0U)
         {
            v >>= 1;
            l++;
         }
#endif
         return static_cast<uint>(l);
      }

		inline uint compute_mask_size(uint x)
		{
			uint l = 0;
			while (x)
			{
				x &= (x - 1);
				l++;
			}
			return l;
		}

		inline uint compute_mask_shift(uint x)
		{
			if (!x)
				return 0;

			uint l = 0;
			while ((x & 1) == 0)
			{
				x >>= 1;
				l++;
			}

			return l;
		}

   }

} // namespace lzham


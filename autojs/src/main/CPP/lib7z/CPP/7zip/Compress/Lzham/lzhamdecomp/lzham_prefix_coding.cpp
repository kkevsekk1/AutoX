// File: lzham_prefix_coding.cpp
// See Copyright Notice and license at the end of include/lzham.h
#include "lzham_core.h"
#include "lzham_prefix_coding.h"

#ifdef LZHAM_BUILD_DEBUG
   //#define TEST_DECODER_TABLES
#endif

namespace lzham
{
   namespace prefix_coding
   {
      bool limit_max_code_size(uint num_syms, uint8* pCodesizes, uint max_code_size)
      {
         const uint cMaxEverCodeSize = 34;            
         
         if ((!num_syms) || (num_syms > cMaxSupportedSyms) || (max_code_size < 1) || (max_code_size > cMaxEverCodeSize))
         {
            LZHAM_LOG_ERROR(3000);
            return false;
         }
         
         uint num_codes[cMaxEverCodeSize + 1];
         utils::zero_object(num_codes);

         bool should_limit = false;
         
         for (uint i = 0; i < num_syms; i++)
         {
            uint c = pCodesizes[i];
            
            LZHAM_ASSERT(c <= cMaxEverCodeSize);
            
            num_codes[c]++;
            if (c > max_code_size)
               should_limit = true;
         }
         
         if (!should_limit)
            return true;
         
         uint ofs = 0;
         uint next_sorted_ofs[cMaxEverCodeSize + 1];
         for (uint i = 1; i <= cMaxEverCodeSize; i++)
         {
            next_sorted_ofs[i] = ofs;
            ofs += num_codes[i];
         }
            
         if ((ofs < 2) || (ofs > cMaxSupportedSyms))
            return true;
         
         if (ofs > (1U << max_code_size))
         {
            LZHAM_LOG_ERROR(3001);
            return false;
         }
                           
         for (uint i = max_code_size + 1; i <= cMaxEverCodeSize; i++)
            num_codes[max_code_size] += num_codes[i];
         
         // Technique of adjusting tree to enforce maximum code size from LHArc. 
			// (If you remember what LHArc was, you've been doing this for a LONG time.)
         
         uint total = 0;
         for (uint i = max_code_size; i; --i)
            total += (num_codes[i] << (max_code_size - i));

         if (total == (1U << max_code_size))  
            return true;
            
         do
         {
            num_codes[max_code_size]--;

            uint i;
            for (i = max_code_size - 1; i; --i)
            {
               if (!num_codes[i])
                  continue;
               num_codes[i]--;          
               num_codes[i + 1] += 2;   
               break;
            }
            if (!i)
            {
               LZHAM_LOG_ERROR(3002);
               return false;
            }

            total--;   
         } while (total != (1U << max_code_size));
         
         uint8 new_codesizes[cMaxSupportedSyms];
         uint8* p = new_codesizes;
         for (uint i = 1; i <= max_code_size; i++)
         {
            uint n = num_codes[i];
            if (n)
            {
               memset(p, i, n);
               p += n;
            }
         }
                                             
         for (uint i = 0; i < num_syms; i++)
         {
            const uint c = pCodesizes[i];
            if (c)
            {
               uint next_ofs = next_sorted_ofs[c];
               next_sorted_ofs[c] = next_ofs + 1;
            
               pCodesizes[i] = static_cast<uint8>(new_codesizes[next_ofs]);
            }
         }
            
         return true;
      }
            
      bool generate_codes(uint num_syms, const uint8* pCodesizes, uint16* pCodes)
      {
         uint num_codes[cMaxExpectedHuffCodeSize + 1];
         utils::zero_object(num_codes);

         for (uint i = 0; i < num_syms; i++)
         {
            uint c = pCodesizes[i];
            LZHAM_ASSERT(c <= cMaxExpectedHuffCodeSize);
            num_codes[c]++;
         }

         uint code = 0;

         uint next_code[cMaxExpectedHuffCodeSize + 1];
         next_code[0] = 0;
         
         for (uint i = 1; i <= cMaxExpectedHuffCodeSize; i++)
         {
            next_code[i] = code;
            
            code = (code + num_codes[i]) << 1;
         }

         if (code != (1 << (cMaxExpectedHuffCodeSize + 1)))
         {
            uint t = 0;
            for (uint i = 1; i <= cMaxExpectedHuffCodeSize; i++)
            {
               t += num_codes[i];
               if (t > 1)
               {
                  LZHAM_LOG_ERROR(3003);
                  return false;
               }
            }
         }

         for (uint i = 0; i < num_syms; i++)
         {
            uint c = pCodesizes[i];
            
            LZHAM_ASSERT(!c || (next_code[c] <= cUINT16_MAX));
            
            pCodes[i] = static_cast<uint16>(next_code[c]++);
            
            LZHAM_ASSERT(!c || (math::total_bits(pCodes[i]) <= pCodesizes[i]));
         }
         
         return true;
      }

      static const uint16 g_uint16_sequence[cMaxSupportedSyms] =
      {
         0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
         16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
         48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
         80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
         112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143,
         144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175,
         176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207,
         208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239,
         240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269, 270, 271,
         272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303,
         304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331, 332, 333, 334, 335,
         336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349, 350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367,
         368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 396, 397, 398, 399,
         400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 419, 420, 421, 422, 423, 424, 425, 426, 427, 428, 429, 430, 431,
         432, 433, 434, 435, 436, 437, 438, 439, 440, 441, 442, 443, 444, 445, 446, 447, 448, 449, 450, 451, 452, 453, 454, 455, 456, 457, 458, 459, 460, 461, 462, 463,
         464, 465, 466, 467, 468, 469, 470, 471, 472, 473, 474, 475, 476, 477, 478, 479, 480, 481, 482, 483, 484, 485, 486, 487, 488, 489, 490, 491, 492, 493, 494, 495,
         496, 497, 498, 499, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524, 525, 526, 527,
         528, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540, 541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 558, 559,
         560, 561, 562, 563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580, 581, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591,
         592, 593, 594, 595, 596, 597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607, 608, 609, 610, 611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623,
         624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 649, 650, 651, 652, 653, 654, 655,
         656, 657, 658, 659, 660, 661, 662, 663, 664, 665, 666, 667, 668, 669, 670, 671, 672, 673, 674, 675, 676, 677, 678, 679, 680, 681, 682, 683, 684, 685, 686, 687,
         688, 689, 690, 691, 692, 693, 694, 695, 696, 697, 698, 699, 700, 701, 702, 703, 704, 705, 706, 707, 708, 709, 710, 711, 712, 713, 714, 715, 716, 717, 718, 719,
         720, 721, 722, 723, 724, 725, 726, 727, 728, 729, 730, 731, 732, 733, 734, 735, 736, 737, 738, 739, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751,
         752, 753, 754, 755, 756, 757, 758, 759, 760, 761, 762, 763, 764, 765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778, 779, 780, 781, 782, 783,
         784, 785, 786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804, 805, 806, 807, 808, 809, 810, 811, 812, 813, 814, 815,
         816, 817, 818, 819, 820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 838, 839, 840, 841, 842, 843, 844, 845, 846, 847,
         848, 849, 850, 851, 852, 853, 854, 855, 856, 857, 858, 859, 860, 861, 862, 863, 864, 865, 866, 867, 868, 869, 870, 871, 872, 873, 874, 875, 876, 877, 878, 879,
         880, 881, 882, 883, 884, 885, 886, 887, 888, 889, 890, 891, 892, 893, 894, 895, 896, 897, 898, 899, 900, 901, 902, 903, 904, 905, 906, 907, 908, 909, 910, 911,
         912, 913, 914, 915, 916, 917, 918, 919, 920, 921, 922, 923, 924, 925, 926, 927, 928, 929, 930, 931, 932, 933, 934, 935, 936, 937, 938, 939, 940, 941, 942, 943,
         944, 945, 946, 947, 948, 949, 950, 951, 952, 953, 954, 955, 956, 957, 958, 959, 960, 961, 962, 963, 964, 965, 966, 967, 968, 969, 970, 971, 972, 973, 974, 975,
         976, 977, 978, 979, 980, 981, 982, 983, 984, 985, 986, 987, 988, 989, 990, 991, 992, 993, 994, 995, 996, 997, 998, 999, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007,
         1008, 1009, 1010, 1011, 1012, 1013, 1014, 1015, 1016, 1017, 1018, 1019, 1020, 1021, 1022, 1023 
      };
                       
      bool generate_decoder_tables(uint num_syms, const uint8* pCodesizes, decoder_tables* pTables, uint table_bits, const code_size_histogram &code_size_histo, bool sym_freq_all_ones)
      {
         uint min_codes[cMaxExpectedHuffCodeSize];
         
         if ((!num_syms) || (table_bits > cMaxTableBits))
         {
            LZHAM_LOG_ERROR(3004);
            return false;
         }
            
         pTables->m_num_syms = num_syms;
      
         uint sorted_positions[cMaxExpectedHuffCodeSize + 1];
               
         uint next_code = 0;

         uint total_used_syms = 0;
         uint max_code_size = 0;
         uint min_code_size = UINT_MAX;
         for (uint i = 1; i <= cMaxExpectedHuffCodeSize; i++)
         {
            const uint n = code_size_histo.m_num_codes[i];
            
            if (!n)
               pTables->m_max_codes[i - 1] = 0;//UINT_MAX;
            else
            {
               min_code_size = math::minimum(min_code_size, i);
               max_code_size = math::maximum(max_code_size, i);
                  
               min_codes[i - 1] = next_code;
               
               pTables->m_max_codes[i - 1] = next_code + n - 1;
               pTables->m_max_codes[i - 1] = 1 + ((pTables->m_max_codes[i - 1] << (16 - i)) | ((1 << (16 - i)) - 1));
               
               pTables->m_val_ptrs[i - 1] = total_used_syms;
               
               sorted_positions[i] = total_used_syms;
               
               next_code += n;
               total_used_syms += n;
            }

            next_code <<= 1;
         }
         
         pTables->m_total_used_syms = total_used_syms;

         if (total_used_syms > pTables->m_cur_sorted_symbol_order_size)
         {
            pTables->m_cur_sorted_symbol_order_size = total_used_syms;
            
            if (!math::is_power_of_2(total_used_syms))
               pTables->m_cur_sorted_symbol_order_size = math::minimum<uint>(num_syms, math::next_pow2(total_used_syms));
            
            if (pTables->m_sorted_symbol_order)
            {
               lzham_delete_array(pTables->m_malloc_context, pTables->m_sorted_symbol_order);
               pTables->m_sorted_symbol_order = NULL;
            }
            
            pTables->m_sorted_symbol_order = lzham_new_array<uint16>(pTables->m_malloc_context, pTables->m_cur_sorted_symbol_order_size);
            if (!pTables->m_sorted_symbol_order)
            {
               LZHAM_LOG_ERROR(3005);
               return false;
            }
         }
         
         pTables->m_min_code_size = static_cast<uint8>(min_code_size);
         pTables->m_max_code_size = static_cast<uint8>(max_code_size);

         if (sym_freq_all_ones)
         {
            if (min_code_size == max_code_size)
            {
               memcpy(pTables->m_sorted_symbol_order, g_uint16_sequence, num_syms * sizeof(uint16));
            }
            else
            {
               LZHAM_ASSERT((min_code_size + 1) == max_code_size);
               LZHAM_ASSERT(pCodesizes[0] == max_code_size);
               LZHAM_ASSERT(pCodesizes[code_size_histo.m_num_codes[max_code_size]] == min_code_size);
                              
               memcpy(pTables->m_sorted_symbol_order + sorted_positions[max_code_size], g_uint16_sequence, code_size_histo.m_num_codes[max_code_size] * sizeof(uint16));
               
               memcpy(pTables->m_sorted_symbol_order + sorted_positions[min_code_size], g_uint16_sequence + code_size_histo.m_num_codes[max_code_size], code_size_histo.m_num_codes[min_code_size] * sizeof(uint16));
            }

#ifdef LZHAM_BUILD_DEBUG
            for (uint i = 0; i < num_syms; i++)
            {
               uint c = pCodesizes[i];
               LZHAM_ASSERT(code_size_histo.m_num_codes[c]);

               uint sorted_pos = sorted_positions[c]++;

               LZHAM_ASSERT(sorted_pos < total_used_syms);

               LZHAM_ASSERT(pTables->m_sorted_symbol_order[sorted_pos] == i);
            }
#endif
         }
         else
         {
            for (uint i = 0; i < num_syms; i++)
            {
               uint c = pCodesizes[i];
               LZHAM_ASSERT(code_size_histo.m_num_codes[c]);
               
               uint sorted_pos = sorted_positions[c]++;
               
               LZHAM_ASSERT(sorted_pos < total_used_syms);
               
               pTables->m_sorted_symbol_order[sorted_pos] = static_cast<uint16>(i);
            }
         }

         if (table_bits <= pTables->m_min_code_size)
            table_bits = 0;                                       
         pTables->m_table_bits = table_bits;
                  
         if (table_bits)
         {
            uint table_size = 1 << table_bits;
            if (table_size > pTables->m_cur_lookup_size)
            {
               pTables->m_cur_lookup_size = table_size;
               
               if (pTables->m_lookup)
               {
                  lzham_delete_array(pTables->m_malloc_context, pTables->m_lookup);
                  pTables->m_lookup = NULL;
               }
                  
               pTables->m_lookup = lzham_new_array<uint32>(pTables->m_malloc_context, table_size);
               if (!pTables->m_lookup)
               {
                  LZHAM_LOG_ERROR(3006);
                  return false;
               }
            }
                        
            memset(pTables->m_lookup, 0xFF, static_cast<uint>(sizeof(pTables->m_lookup[0])) * (1UL << table_bits));
            
            for (uint codesize = 1; codesize <= table_bits; codesize++)
            {
               if (!code_size_histo.m_num_codes[codesize])
                  continue;
               
               const uint fillsize = table_bits - codesize;
               const uint fillnum = 1 << fillsize;
               
               const uint min_code = min_codes[codesize - 1];
               const uint max_code = pTables->get_unshifted_max_code(codesize);
               const uint val_ptr = pTables->m_val_ptrs[codesize - 1];
                      
               for (uint code = min_code; code <= max_code; code++)
               {
                  const uint sym_index = pTables->m_sorted_symbol_order[ val_ptr + code - min_code ];
                  LZHAM_ASSERT( pCodesizes[sym_index] == codesize );
                  
                  for (uint j = 0; j < fillnum; j++)
                  {
                     const uint t = j + (code << fillsize);
                     
                     LZHAM_ASSERT(t < (1U << table_bits));
                     
                     LZHAM_ASSERT(pTables->m_lookup[t] == cUINT32_MAX);
                     
                     pTables->m_lookup[t] = sym_index | (codesize << 16U);
                  }
               }
            }
         }         
         
         for (uint i = 0; i < cMaxExpectedHuffCodeSize; i++)
            pTables->m_val_ptrs[i] -= min_codes[i];
         
         pTables->m_table_max_code = 0;
         pTables->m_decode_start_code_size = pTables->m_min_code_size;

         if (table_bits)
         {
            uint i;
            for (i = table_bits; i >= 1; i--)
            {
               if (code_size_histo.m_num_codes[i])
               {
                  pTables->m_table_max_code = pTables->m_max_codes[i - 1];
                  break;
               }
            }
            if (i >= 1)
            {
               pTables->m_decode_start_code_size = table_bits + 1;
               for (i = table_bits + 1; i <= max_code_size; i++)
               {
                  if (code_size_histo.m_num_codes[i])
                  {
                     pTables->m_decode_start_code_size = i;
                     break;
                  }
               }
            }
         }

         // sentinels
         pTables->m_max_codes[cMaxExpectedHuffCodeSize] = UINT_MAX;
         pTables->m_val_ptrs[cMaxExpectedHuffCodeSize] = 0xFFFFF;

         pTables->m_table_shift = 32 - pTables->m_table_bits;

         return true;
      }
               
   } // namespace prefix_codig

} // namespace lzham



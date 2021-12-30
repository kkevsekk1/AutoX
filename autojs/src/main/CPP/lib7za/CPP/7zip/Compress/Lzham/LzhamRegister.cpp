// LzhamRegister.cpp
// Portions of this module are from 7-zip, by Igor Pavlov, which you can download here:
// http://www.7-zip.org/

//#include "StdAfx.h"

#ifndef _WIN32
#define WINAPI /* */
#endif

#include "lzham_core.h" // for LZHAM_64BIT_POINTERS

#include "C/Alloc.h"

//#include "../Common/RegisterCodec.h"
#include "Common/Common.h"
#include "Common/MyCom.h"
#include "7zip/ICoder.h"
#include "7zip/Common/StreamUtils.h"
#include "7zip/Common/RegisterCodec.h"
#include "Windows/System.h"

#include "lzham_static_lib.h"

#if 0
#include <stdio.h>
#define LZHAMCODEC_DEBUG_OUTPUT 1
#endif

#define LZHAM_PROPS_VER (Byte)(LZHAM_DLL_VERSION)

namespace NCompress
{
   namespace NLzham
   {
      struct CProps
      {
         CProps() { clear(); }

         void clear() 
		 { 
			 memset(this, 0, sizeof(*this)); 
			 _ver = LZHAM_PROPS_VER; 
			 _dict_size = 0; 
			 _level = LZHAM_COMP_LEVEL_UBER; 
			 _flags = 0; 
		 }

         Byte _ver;
         Byte _dict_size;
         Byte _level;
         Byte _flags;
         Byte _reserved[1];
      };

      class CDecoder:
         public ICompressCoder,
         public ICompressSetDecoderProperties2,
         public ICompressSetBufSize,
#ifndef NO_READ_FROM_CODER
         public ICompressSetInStream,
         public ICompressSetOutStreamSize,
         public ISequentialInStream,
#endif
         public CMyUnknownImp
      {
         CMyComPtr<ISequentialInStream> _inStream;
         Byte *_inBuf;
         Byte *_outBuf;
         UInt32 _inPos;
         UInt32 _inSize;
         
         lzham_decompress_state_ptr _state;

         CProps _props;
         bool _propsWereSet;
         
         bool _outSizeDefined;
         UInt64 _outSize;
         UInt64 _inSizeProcessed;
         UInt64 _outSizeProcessed;

         UInt32 _inBufSizeAllocated;
         UInt32 _outBufSizeAllocated;
         UInt32 _inBufSize;
         UInt32 _outBufSize;

         HRESULT CreateBuffers();
         HRESULT CodeSpec(ISequentialInStream *inStream, ISequentialOutStream *outStream, ICompressProgressInfo *progress);
         
         HRESULT SetOutStreamSizeResume(const UInt64 *outSize);
         HRESULT CreateDecompressor();

      public:
         MY_QUERYINTERFACE_BEGIN2(ICompressCoder)
            MY_QUERYINTERFACE_ENTRY(ICompressSetDecoderProperties2)
            MY_QUERYINTERFACE_ENTRY(ICompressSetBufSize)
#ifndef NO_READ_FROM_CODER
            MY_QUERYINTERFACE_ENTRY(ICompressSetInStream)
            MY_QUERYINTERFACE_ENTRY(ICompressSetOutStreamSize)
            MY_QUERYINTERFACE_ENTRY(ISequentialInStream)
#endif
            MY_QUERYINTERFACE_END
            MY_ADDREF_RELEASE

            STDMETHOD(Code)(ISequentialInStream *inStream, ISequentialOutStream *outStream,
            const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);
         STDMETHOD(SetDecoderProperties2)(const Byte *data, UInt32 size);
         STDMETHOD(SetOutStreamSize)(const UInt64 *outSize);
         STDMETHOD(SetInBufSize)(UInt32 streamIndex, UInt32 size);
         STDMETHOD(SetOutBufSize)(UInt32 streamIndex, UInt32 size);

#ifndef NO_READ_FROM_CODER

         STDMETHOD(SetInStream)(ISequentialInStream *inStream);
         STDMETHOD(ReleaseInStream)();
         STDMETHOD(Read)(void *data, UInt32 size, UInt32 *processedSize);

         HRESULT CodeResume(ISequentialOutStream *outStream, const UInt64 *outSize, ICompressProgressInfo *progress);
         UInt64 GetInputProcessedSize() const { return _inSizeProcessed; }

#endif
         
         CDecoder();
         virtual ~CDecoder();
      };

      CDecoder::CDecoder(): _inBuf(0), _outBuf(0), _propsWereSet(false), _outSizeDefined(false),
         _inBufSize(1 << 22),
         _outBufSize(1 << 22),
         _state(NULL),
         _inBufSizeAllocated(0),
         _outBufSizeAllocated(0),
         _inSizeProcessed(0),
         _outSizeProcessed(0)
      {
         _inSizeProcessed = 0;
         _inPos = _inSize = 0;
      }

      CDecoder::~CDecoder()
      {
         lzham_decompress_deinit(_state);
         MyFree(_inBuf);
         MyFree(_outBuf);
      }

      STDMETHODIMP CDecoder::SetInBufSize(UInt32 , UInt32 size) 
      { 
         _inBufSize = size; 
         return S_OK; 
      }

      STDMETHODIMP CDecoder::SetOutBufSize(UInt32 , UInt32 size) 
      { 
         _outBufSize = size; 
         return S_OK; 
      }

      HRESULT CDecoder::CreateBuffers()
      {
         if (_inBuf == 0 || _inBufSize != _inBufSizeAllocated)
         {
            MyFree(_inBuf);
            _inBuf = (Byte *)MyAlloc(_inBufSize);
            if (_inBuf == 0)
               return E_OUTOFMEMORY;
            _inBufSizeAllocated = _inBufSize;
         }

         if (_outBuf == 0 || _outBufSize != _outBufSizeAllocated)
         {
            MyFree(_outBuf);
            _outBuf = (Byte *)MyAlloc(_outBufSize);
            if (_outBuf == 0)
               return E_OUTOFMEMORY;
            _outBufSizeAllocated = _outBufSize;
         }

         return S_OK;
      }

      STDMETHODIMP CDecoder::SetDecoderProperties2(const Byte *prop, UInt32 size)
      {
         CProps *pProps = (CProps*)prop;

         if (size != sizeof(CProps))
            return E_FAIL;

         if (pProps->_ver != LZHAM_PROPS_VER)
            return E_FAIL;

         memcpy(&_props, pProps, sizeof(CProps));
                  
         _propsWereSet = true;
         
         return CreateBuffers();
      }

      HRESULT CDecoder::CreateDecompressor()
      {
         if (!_propsWereSet)
            return E_FAIL;
         
         lzham_decompress_params params;
         memset(&params, 0, sizeof(params));
         params.m_struct_size = sizeof(lzham_decompress_params);
         params.m_decompress_flags = 0;
         params.m_dict_size_log2 = _props._dict_size ? _props._dict_size : 26;
                  
         _state = lzham_decompress_reinit(_state, &params);
         if (!_state)
            return E_FAIL;

         return S_OK;
      }

      HRESULT CDecoder::SetOutStreamSizeResume(const UInt64 *outSize)
      {
         _outSizeDefined = (outSize != NULL);
         if (_outSizeDefined)
            _outSize = *outSize;
         _outSizeProcessed = 0;

         RINOK(CreateDecompressor());

         return S_OK;
      }

      STDMETHODIMP CDecoder::SetOutStreamSize(const UInt64 *outSize)
      {
         _inSizeProcessed = 0;
         _inPos = _inSize = 0;
         RINOK(SetOutStreamSizeResume(outSize));
         return S_OK;
      }

      HRESULT CDecoder::CodeSpec(ISequentialInStream *inStream, ISequentialOutStream *outStream, ICompressProgressInfo *progress)
      {
         if (_inBuf == 0 || !_propsWereSet)
            return S_FALSE;

         if (!_state)
         {
            if (CreateDecompressor() != S_OK)
               return E_FAIL;
         }

         UInt64 startInProgress = _inSizeProcessed;
         
         for (;;)
         {
            bool eofFlag = false;
            if (_inPos == _inSize)
            {
               _inPos = _inSize = 0;
               RINOK(inStream->Read(_inBuf, _inBufSizeAllocated, &_inSize));
               if (!_inSize)
                  eofFlag = true;
            }

            lzham_uint8 *pIn_bytes = _inBuf + _inPos;
            size_t num_in_bytes = _inSize - _inPos;
            lzham_uint8* pOut_bytes = _outBuf;
            size_t out_num_bytes = _outBufSize;
            if (_outSizeDefined)
            {
               UInt64 out_remaining = _outSize - _outSizeProcessed;
               if (out_num_bytes > out_remaining)
                  out_num_bytes = static_cast<size_t>(out_remaining);
            }
            
            lzham_decompress_status_t status = lzham_decompress(_state, pIn_bytes, &num_in_bytes, pOut_bytes, &out_num_bytes, eofFlag);
            
            if (num_in_bytes)
            {
               _inPos += (UInt32)num_in_bytes;
               _inSizeProcessed += (UInt32)num_in_bytes;
            }
                        
            if (out_num_bytes)
            {
               _outSizeProcessed += out_num_bytes;

               RINOK(WriteStream(outStream, _outBuf, out_num_bytes));
            }
            
            if (status >= LZHAM_DECOMP_STATUS_FIRST_FAILURE_CODE)
               return S_FALSE;

            if (status == LZHAM_DECOMP_STATUS_SUCCESS)
               break;
                        
            UInt64 inSize = _inSizeProcessed - startInProgress;
            if (progress)
            {
               RINOK(progress->SetRatioInfo(&inSize, &_outSizeProcessed));
            }
         }

         return S_OK;
      }

      STDMETHODIMP CDecoder::Code(ISequentialInStream *inStream, ISequentialOutStream *outStream,
         const UInt64 * inSize, const UInt64 *outSize, ICompressProgressInfo *progress)
      {
         if (_inBuf == 0)
            return E_INVALIDARG;
         SetOutStreamSize(outSize);
         return CodeSpec(inStream, outStream, progress);
      }
      
#ifndef NO_READ_FROM_CODER
      STDMETHODIMP CDecoder::SetInStream(ISequentialInStream *inStream) 
      { 
         _inStream = inStream; 
         return S_OK; 
      }

      STDMETHODIMP CDecoder::ReleaseInStream() 
      { 
         _inStream.Release(); 
         return S_OK; 
      }

      STDMETHODIMP CDecoder::Read(void *data, UInt32 size, UInt32 *processedSize)
      {
         if (_inBuf == 0 || !_propsWereSet)
            return S_FALSE;

         if (!_state)
         {
            if (CreateDecompressor() != S_OK)
               return E_FAIL;
         }

         if (processedSize)
            *processedSize = 0;

         while (size != 0)
         {
            bool eofFlag = false;
            if (_inPos == _inSize)
            {
               _inPos = _inSize = 0;
               RINOK(_inStream->Read(_inBuf, _inBufSizeAllocated, &_inSize));
               if (!_inSize)
                  eofFlag = true;
            }

            lzham_uint8 *pIn_bytes = _inBuf + _inPos;
            size_t num_in_bytes = _inSize - _inPos;
            lzham_uint8* pOut_bytes = (lzham_uint8*)data;
            size_t out_num_bytes = size;

            lzham_decompress_status_t status = lzham_decompress(_state, pIn_bytes, &num_in_bytes, pOut_bytes, &out_num_bytes, eofFlag);

            if (num_in_bytes)
            {
               _inPos += (UInt32)num_in_bytes;
               _inSizeProcessed += num_in_bytes;
            }

            if (out_num_bytes)
            {
               _outSizeProcessed += out_num_bytes;
               size -= (UInt32)out_num_bytes;
               if (processedSize)
                  *processedSize += (UInt32)out_num_bytes;
            }

            if (status >= LZHAM_DECOMP_STATUS_FIRST_FAILURE_CODE)
               return S_FALSE;

            if (status == LZHAM_DECOMP_STATUS_SUCCESS)
               break;
         }

         return S_OK;
      }

      HRESULT CDecoder::CodeResume(ISequentialOutStream *outStream, const UInt64 *outSize, ICompressProgressInfo *progress)
      {
         RINOK(SetOutStreamSizeResume(outSize));
         return CodeSpec(_inStream, outStream, progress);
      }
#endif

   } // namespace NLzham
} // namespace NCompress

static void *CreateCodec() 
{ 
   return (void *)(ICompressCoder *)(new NCompress::NLzham::CDecoder); 
}

#ifndef EXTRACT_ONLY

namespace NCompress
{
   namespace NLzham
   {
      class CEncoder:
         public ICompressCoder,
         public ICompressSetCoderProperties,
         public ICompressWriteCoderProperties,
         public CMyUnknownImp
      {
         lzham_compress_state_ptr _state;
         CProps _props;
         bool _dictSizeSet;
         int _num_threads;
         
         Byte *_inBuf;
         Byte *_outBuf;
         UInt32 _inPos;
         UInt32 _inSize;

         UInt32 _inBufSizeAllocated;
         UInt32 _outBufSizeAllocated;
         UInt32 _inBufSize;
         UInt32 _outBufSize;

         UInt64 _inSizeProcessed;
         UInt64 _outSizeProcessed;
         
         HRESULT CreateCompressor();
         HRESULT CreateBuffers();

      public:
         MY_UNKNOWN_IMP2(ICompressSetCoderProperties, ICompressWriteCoderProperties)

         STDMETHOD(Code)(ISequentialInStream *inStream, ISequentialOutStream *outStream,
            const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);

         STDMETHOD(SetCoderProperties)(const PROPID *propIDs, const PROPVARIANT *props, UInt32 numProps);

         STDMETHOD(WriteCoderProperties)(ISequentialOutStream *outStream);

         CEncoder();
         virtual ~CEncoder();
      };

      CEncoder::CEncoder() :
         _state(NULL),
         _dictSizeSet(false),
         _num_threads(-1),
         _inBuf(NULL),
         _outBuf(NULL),
         _inPos(0),
         _inSize(0),
         _inBufSizeAllocated(0),
         _outBufSizeAllocated(0),
         _inBufSize(1 << 22),
         _outBufSize(1 << 22),
         _inSizeProcessed(0),
         _outSizeProcessed(0)
      {
      }

      CEncoder::~CEncoder()
      {
         lzham_compress_deinit(_state);
         MyFree(_inBuf);
         MyFree(_outBuf);
      }

      STDMETHODIMP CEncoder::SetCoderProperties(const PROPID *propIDs,
         const PROPVARIANT *coderProps, UInt32 numProps)
      {
         _props.clear();
         
         for (UInt32 i = 0; i < numProps; i++)
         {
            const PROPVARIANT &prop = coderProps[i];
            PROPID propID = propIDs[i];
            switch (propID)
            {
               //case NCoderPropID::kEndMarker:
               //   if (prop.vt != VT_BOOL) return E_INVALIDARG; props.writeEndMark = (prop.boolVal == VARIANT_TRUE); break;
               case NCoderPropID::kAlgorithm:
               {
                  if (prop.vt != VT_UI4)
                     return E_INVALIDARG;

                  bool val = (UInt32)prop.ulVal != 0;

                  if (prop.boolVal)
                     _props._flags |= LZHAM_COMP_FLAG_DETERMINISTIC_PARSING;
                  else
                     _props._flags &= ~LZHAM_COMP_FLAG_DETERMINISTIC_PARSING;

#if LZHAMCODEC_DEBUG_OUTPUT                  
                  printf("Algorithm: %u\n", prop.ulVal);
#endif

                  break;
               }
               case NCoderPropID::kNumThreads:
               {
                  if (prop.vt != VT_UI4) 
                     return E_INVALIDARG; 
                  _num_threads = prop.ulVal; 

#if LZHAMCODEC_DEBUG_OUTPUT                  
                  printf("Num threads: %u\n", _num_threads);
#endif
                  break;
               }
               case NCoderPropID::kDictionarySize:
               {
                  if (prop.vt != VT_UI4)
                     return E_INVALIDARG;
                  lzham_uint32 bits = 15;
                  while ((1U << bits) < prop.ulVal)
                     bits++;
#if LZHAM_64BIT_POINTERS
                  if (bits > LZHAM_MAX_DICT_SIZE_LOG2_X64)
#else
                  if (bits > LZHAM_MAX_DICT_SIZE_LOG2_X86)
#endif
                  {
                     return E_INVALIDARG;
                  }

                  _props._dict_size = bits; 
                  
#if LZHAMCODEC_DEBUG_OUTPUT                  
                  printf("Dict size: %u\n", bits);
#endif

                  break;
               }
               case NCoderPropID::kLevel: 
               {
                  if (prop.vt != VT_UI4)
                     return E_INVALIDARG;
                                                         
                  switch (prop.ulVal)
                  {
                     case 0:
                        _props._level = 0; if (!_props._dict_size) _props._dict_size = 18;
                        break;
                     case 1:
                        _props._level = 0; if (!_props._dict_size) _props._dict_size = 20;
                        break;
                     case 2:
                        _props._level = 1; if (!_props._dict_size) _props._dict_size = 21;
                        break;
                     case 3:
                        _props._level = 2; if (!_props._dict_size) _props._dict_size = 21;
                        break;
                     case 4:
                        _props._level = 2; if (!_props._dict_size) _props._dict_size = 22;
                        break;
                     case 5:
                        _props._level = 3; if (!_props._dict_size) _props._dict_size = 22;
                        break;
                     case 6:
                        _props._level = 3; if (!_props._dict_size) _props._dict_size = 23;
                        break;
                     case 7:
                        _props._level = 4; if (!_props._dict_size) _props._dict_size = 25;
                        break;
                     case 8:
                        _props._level = 4; if (!_props._dict_size) _props._dict_size = 26;
                        break;
                     case 9:
                        _props._level = 4; if (!_props._dict_size) _props._dict_size = 26;
                        _props._flags |= LZHAM_COMP_FLAG_EXTREME_PARSING;
                        break;
                     default: 
                        return E_INVALIDARG;
                  }

#if LZHAMCODEC_DEBUG_OUTPUT                                                      
                  printf("Level: %u\n", prop.ulVal);
#endif
                  break;
               }
               default:
               {
                  //RINOK(SetLzmaProp(propID, prop, props));
                  break;
               }
            }
         }

         return S_OK;
      }

      STDMETHODIMP CEncoder::WriteCoderProperties(ISequentialOutStream *outStream)
      {
         return WriteStream(outStream, &_props, sizeof(_props));
      }

      HRESULT CEncoder::CreateBuffers()
      {
         if (_inBuf == 0 || _inBufSize != _inBufSizeAllocated)
         {
            MyFree(_inBuf);
            _inBuf = (Byte *)MyAlloc(_inBufSize);
            if (_inBuf == 0)
               return E_OUTOFMEMORY;
            _inBufSizeAllocated = _inBufSize;
         }

         if (_outBuf == 0 || _outBufSize != _outBufSizeAllocated)
         {
            MyFree(_outBuf);
            _outBuf = (Byte *)MyAlloc(_outBufSize);
            if (_outBuf == 0)
               return E_OUTOFMEMORY;
            _outBufSizeAllocated = _outBufSize;
         }

         return S_OK;
      }

      HRESULT CEncoder::CreateCompressor()
      {
         if (_state)
            lzham_compress_deinit(_state);

         lzham_compress_params params;
         memset(&params, 0, sizeof(params));
         params.m_struct_size = sizeof(lzham_compress_params);

#if 0
         SYSTEM_INFO system_info;
         GetSystemInfo(&system_info);

         if (_num_threads < 0)
         {
            if (system_info.dwNumberOfProcessors > 1)
               params.m_max_helper_threads = system_info.dwNumberOfProcessors - 1;
         }
         else if (_num_threads > 1)
         {
            params.m_max_helper_threads = _num_threads - 1;
         }

         if (system_info.dwNumberOfProcessors > 1)
         {
            if (params.m_max_helper_threads > ((int)system_info.dwNumberOfProcessors - 1))
               params.m_max_helper_threads = system_info.dwNumberOfProcessors - 1;
         }
#else
		UInt32 numCPUs = 1;
		#ifndef _7ZIP_ST
		numCPUs = NWindows::NSystem::GetNumberOfProcessors();
		#endif
         if (_num_threads < 0)
         {
            if (numCPUs > 1)
               params.m_max_helper_threads = numCPUs - 1;
         }
         else if (_num_threads > 1)
         {
            params.m_max_helper_threads = _num_threads - 1;
         }

         if (numCPUs > 1)
         {
            if (params.m_max_helper_threads > ((int)numCPUs - 1))
               params.m_max_helper_threads = numCPUs - 1;
         }

#endif

         if (params.m_max_helper_threads > LZHAM_MAX_HELPER_THREADS)
            params.m_max_helper_threads = LZHAM_MAX_HELPER_THREADS;

         params.m_dict_size_log2 = _props._dict_size ? _props._dict_size : 26;

         if (params.m_dict_size_log2 < LZHAM_MIN_DICT_SIZE_LOG2)
            params.m_dict_size_log2 = LZHAM_MIN_DICT_SIZE_LOG2;
         else 
         {
#if LZHAM_64BIT_POINTERS
            if (params.m_dict_size_log2 > LZHAM_MAX_DICT_SIZE_LOG2_X64)
               params.m_dict_size_log2 = LZHAM_MAX_DICT_SIZE_LOG2_X64;
#else
            if (params.m_dict_size_log2 > LZHAM_MAX_DICT_SIZE_LOG2_X86)
               params.m_dict_size_log2 = LZHAM_MAX_DICT_SIZE_LOG2_X86;
#endif
         }

         params.m_compress_flags = (lzham_compress_flags)_props._flags;

         params.m_level = (lzham_compress_level)_props._level;

#if LZHAMCODEC_DEBUG_OUTPUT
         printf("lzham_compress_params:\nmax_helper_threads: %u, dict_size_log2: %u, level: %u, deterministic_parsing: %u, extreme_parsing: %u\n", 
            params.m_max_helper_threads, params.m_dict_size_log2, params.m_level, (_props._flags & LZHAM_COMP_FLAG_DETERMINISTIC_PARSING) != 0, (_props._flags & LZHAM_COMP_FLAG_EXTREME_PARSING) != 0);
#endif

         _state = lzham_compress_init(&params);
         if (!_state)
            return S_FALSE;

         return S_OK;
      }

      STDMETHODIMP CEncoder::Code(ISequentialInStream *inStream, ISequentialOutStream *outStream,
         const UInt64 * /* inSize */, const UInt64 * /* outSize */, ICompressProgressInfo *progress)
      {
         RINOK(CreateCompressor());
         
         RINOK(CreateBuffers());

         UInt64 startInProgress = _inSizeProcessed;
         UInt64 startOutProgress = _outSizeProcessed;

         for (;;)
         {
            bool eofFlag = false;
            if (_inPos == _inSize)
            {
               _inPos = _inSize = 0;
               RINOK(inStream->Read(_inBuf, _inBufSizeAllocated, &_inSize));
               if (!_inSize)
                  eofFlag = true;
            }

            lzham_uint8 *pIn_bytes = _inBuf + _inPos;
            size_t num_in_bytes = _inSize - _inPos;
            lzham_uint8* pOut_bytes = _outBuf;
            size_t out_num_bytes = _outBufSize;
            
            lzham_compress_status_t status = lzham_compress(_state, pIn_bytes, &num_in_bytes, pOut_bytes, &out_num_bytes, eofFlag);

            if (num_in_bytes)
            {
               _inPos += (UInt32)num_in_bytes;
               _inSizeProcessed += (UInt32)num_in_bytes;
            }

            if (out_num_bytes)
            {
               _outSizeProcessed += out_num_bytes;

               RINOK(WriteStream(outStream, _outBuf, out_num_bytes));
            }

            if (status >= LZHAM_COMP_STATUS_FIRST_FAILURE_CODE)
               return S_FALSE;

            if (status == LZHAM_COMP_STATUS_SUCCESS)
               break;

            UInt64 inSize = _inSizeProcessed - startInProgress;
            UInt64 outSize = _outSizeProcessed - startOutProgress;
            if (progress)
            {
               RINOK(progress->SetRatioInfo(&inSize, &outSize));
            }
         }

         return S_OK;
      }
   }
}

static void *CreateCodecOut() 
{ 
   return (void *)(ICompressCoder *)(new NCompress::NLzham::CEncoder);  
}
#else
#define CreateCodecOut 0
#endif

static CCodecInfo g_CodecsInfo[1] =
{ 
   CreateCodec, 
   CreateCodecOut, 
   0x4F71001, 
   "LZHAM", 
   1, 
   false 
};

REGISTER_CODECS(LZHAM)

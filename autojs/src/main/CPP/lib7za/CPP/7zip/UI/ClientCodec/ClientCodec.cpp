// Client7z.cpp

#include "StdAfx.h"

#ifdef _WIN32
#include <initguid.h>
#else
#include "Common/MyInitGuid.h"
#endif

#include "Windows/DLL.h"
#include "../../ICoder.h"
#include "Windows/PropVariant.h"
#include "Common/MyCom.h"

using namespace NWindows;

class CFileIn : public ISequentialInStream, public CMyUnknownImp
{
  FILE *file_;
  size_t pos_;
  public:
     MY_UNKNOWN_IMP

     CFileIn() : file_(0) , pos_(0) { }
     ~CFileIn() { this->close(); }

     HRESULT open(const char *name)
     {
       file_ = fopen(name,"rb");
       if (file_) return S_OK;
       return E_FAIL;
     }
     void close()
     {
        if (file_) fclose(file_);
        file_ = 0;
        pos_ = 0;
     }
     HRESULT Read(void *data, UInt32 size, UInt32 *processedSize)
     {
       if (file_)
       {
          size_t ret = fread (data, 1, size, file_);
          *processedSize = ret;
          pos_ += ret;
          // TBD : if ret == 0, test for feof/ferror
          return S_OK;
       }
       return E_FAIL;
     }
     size_t pos() { return pos_; }
};

class CFileOut : public ISequentialOutStream, public CMyUnknownImp
{
  FILE *file_;
  size_t pos_;
  public:
     MY_UNKNOWN_IMP

     CFileOut() : file_(0) { }
     ~CFileOut() { this->close(); }

     HRESULT open(const char *name)
     {
       file_ = fopen(name,"wb");
       if (file_) return S_OK;
       return E_FAIL;
     }
     void close()
     {
        if (file_) fclose(file_);
        file_ = 0;
     }
     HRESULT Write(const void *data, UInt32 size, UInt32 *processedSize)
     {
       if (file_)
       {
          size_t ret = fwrite(data, 1, size, file_);
          *processedSize = ret;
          pos_ += ret;
          // TBD : if ret == 0, test for feof/ferror
          return S_OK;
       }
       return E_FAIL;
     }
     size_t pos() { return pos_; }
};

//////////////////////////////////////////////////////////////////////////
// Main function

static const char *kHelpString = 
"Usage: ClientCodec codec.so [c | d | i] [file_in file_out]\n"
"Examples:\n"
"  ClientCodec LZMA.so i                  : info about the codec\n"
"  ClientCodec LZMA.so e file_in file_out : encodes file_in to file_out\n"
"  ClientCodec LZMA.so d file_in file_out : decodes file_in to file_out\n"
;

typedef UINT32 (WINAPI * CreateObjectFunc)(
    const GUID *clsID, 
    const GUID *interfaceID, 
    void **outObject);

typedef UINT32 (WINAPI * GetNumberOfMethodsFunc)(UINT32 *numMethods);

typedef UINT32 (WINAPI * GetMethodPropertyFunc)(UINT32 index, PROPID propID, PROPVARIANT *value);

int main(int argc, char* argv[])
{
  if ((argc != 3) && (argc != 5))
  {
    printf(kHelpString);
    return 1;
  }

  if ((argc == 3) && (strcmp(argv[2],"i") != 0))
  {
    printf(kHelpString);
    return 1;
  }

  NWindows::NDLL::CLibrary library;
  if (!library.Load(argv[1]))
  {
    printf("Can not load library %s\n",argv[1]);
    return 1;
  }
  CreateObjectFunc createObjectFunc = (CreateObjectFunc)library.GetProcAddress("CreateObject");
  if (createObjectFunc == 0)
  {
    printf("Can not get CreateObject\n");
    return 1;
  }

    GetNumberOfMethodsFunc getNumberOfMethodsFunc = (GetNumberOfMethodsFunc)library.GetProcAddress("GetNumberOfMethods");
    if (getNumberOfMethodsFunc == 0)
    {
      printf("Can not get GetNumberOfMethodsFunc\n");
      return 1;
    }

    UINT32 numMethods = 0;
    HRESULT res = getNumberOfMethodsFunc(&numMethods);
    if (res != S_OK)
    {
      printf("Error in GetNumberOfMethods\n");
      return 1;
    }

    GetMethodPropertyFunc getMethodPropertyFunc = (GetMethodPropertyFunc)library.GetProcAddress("GetMethodProperty");
    if (getMethodPropertyFunc == 0)
    {
      printf("Can not get GetMethodProperty\n");
      return 1;
    }

  if (argv[2][0] == 'i')
  {
    printf("%s has %d method(s)\n",argv[1],(int)numMethods);

    for(UINT32 m = 0; m < numMethods ; m++)
    {
      printf("\tMethod %d :\n",(int)m);
      NCOM::CPropVariant propVariant;
      res = getMethodPropertyFunc(m,NMethodPropID::kName,&propVariant);
      if (res == S_OK)
      {
        if (propVariant.vt == VT_BSTR)
        {
          printf("\t\tName : %ls\n",propVariant.bstrVal); // Unicode Name
        } else {
          printf("\t\tName : Error\n");
        }
      } else {
        printf("\t\tName : Unknown\n");
      }
      res = getMethodPropertyFunc(m,NMethodPropID::kDecoder,&propVariant);
      if ((res == S_OK) && (propVariant.vt == VT_BSTR)) printf("\t\tDecoder : YES\n");
      else                                              printf("\t\tDecoder : NO\n");

      res = getMethodPropertyFunc(m,NMethodPropID::kEncoder,&propVariant);
      if ((res == S_OK) && (propVariant.vt == VT_BSTR)) printf("\t\tEncoder : YES\n");
      else                                              printf("\t\tEncoder : NO\n");
    }
  }

  int numMethod = 0; // TBD

  if (argv[2][0] == 'e')
  {
    NCOM::CPropVariant propVariant;
    res = getMethodPropertyFunc(numMethod,NMethodPropID::kEncoder,&propVariant);
    if ((res == S_OK) && (propVariant.vt == VT_BSTR))
    {
       CMyComPtr<ICompressCoder> outCoder;
       if (createObjectFunc((const GUID *)propVariant.bstrVal, &IID_ICompressCoder, (void **)&outCoder) != S_OK)
       {
         printf("Can not get class object\n");
         return 1;
       }
       printf("Encoding : ...\n");

       CMyComPtr<CFileIn> inStream = new CFileIn;
       res = inStream->open(argv[3]);
       if (res != S_OK)
       {
         printf("cannot open %s\n",argv[3]);
         return 1;
       }

       CMyComPtr<CFileOut> outStream = new CFileOut;
       res = outStream->open(argv[4]);
       if (res != S_OK)
       {
         printf("cannot open %s\n",argv[4]);
         return 1;
       }
{
       CMyComPtr<ICompressSetCoderProperties> setCoderProperties;
       outCoder.QueryInterface(IID_ICompressSetCoderProperties, &setCoderProperties);
       if (setCoderProperties != NULL)
       {
          printf("IID_ICompressSetCoderProperties : Found\n");
          PROPID propID = NCoderPropID::kEndMarker;
          NWindows::NCOM::CPropVariant value = true;          
          res = setCoderProperties->SetCoderProperties(&propID, &value, 1);
          if (res = S_OK) printf("kEndMarker : ON\n");
          else            printf("kEndMarker : KO KO\n");
       }
       else
       {
          printf("IID_ICompressSetCoderProperties : NOT Found\n");
       }
}

{
    CMyComPtr<ICompressWriteCoderProperties> writeCoderProperties;
    
    outCoder.QueryInterface(IID_ICompressWriteCoderProperties, &writeCoderProperties);
    
    if (writeCoderProperties != NULL)
    {
	UINT32 len = 5; // TBD
        UInt32 processedSize;
        outStream->Write(&len, sizeof(len), &processedSize);
        
	printf("IID_ICompressWriteCoderProperties : Found\n");
        size_t pos1 = outStream->pos();
        writeCoderProperties->WriteCoderProperties(outStream);
        size_t pos2 = outStream->pos();
        printf("SizeOfProp : %d\n",(int)(pos2-pos1));
/*
      CSequentialOutStreamImp *outStreamSpec = new CSequentialOutStreamImp;
      CMyComPtr<ISequentialOutStream> outStream(outStreamSpec);
      outStreamSpec->Init();
      writeCoderProperties->WriteCoderProperties(outStream);
      
      size_t size = outStreamSpec->GetSize();
      
      // encodingInfo.Properties.SetCapacity(size);
      if (encodingInfo.AltCoders.Size() == 0)
        encodingInfo.AltCoders.Add(CAltCoderInfo());
      CAltCoderInfo &altCoderInfo = encodingInfo.AltCoders.Front();
      altCoderInfo.Properties.SetCapacity(size);
      
      memmove(altCoderInfo.Properties, outStreamSpec->GetBuffer(), size);
*/
    }
    else
    {
	printf("IID_ICompressWriteCoderProperties : NOT Found\n");
	UINT32 len = 0;
        UInt32 processedSize;
        outStream->Write(&len, sizeof(len), &processedSize);
    }
}

       res = outCoder->Code(inStream,outStream,0,0,0);
       inStream->close();
       outStream->close();

       if (res == S_OK)
       {
         printf("Encoding : Done\n");
       } else {
         printf("Encoding : Error\n");
         return 1;
       }
    }
    else
    {
	printf("Encoder not available\n");
        return 1;
    }
  }

  if (argv[2][0] == 'd')
  {
    NCOM::CPropVariant propVariant;
    res = getMethodPropertyFunc(numMethod,NMethodPropID::kDecoder,&propVariant);
    if ((res == S_OK) && (propVariant.vt == VT_BSTR))
    {
       CMyComPtr<ICompressCoder> outCoder;
       if (createObjectFunc((const GUID *)propVariant.bstrVal, &IID_ICompressCoder, (void **)&outCoder) != S_OK)
       {
         printf("Can not get class object\n");
         return 1;
       }
       printf("Decoding : ...\n");

       CMyComPtr<CFileIn> inStream = new CFileIn;
       res = inStream->open(argv[3]);
       if (res != S_OK)
       {
         printf("cannot open %s\n",argv[3]);
         return 1;
       }

       CMyComPtr<CFileOut> outStream = new CFileOut;
       res = outStream->open(argv[4]);
       if (res != S_OK)
       {
         printf("cannot open %s\n",argv[4]);
         return 1;
       }
{
	UINT32 len = 0;
        UInt32 processedSize;
        inStream->Read(&len, sizeof(len), &processedSize);

        if (len > 0)
        {
           Byte props[256]; // TBD
           inStream->Read(props, len, &processedSize);

           CMyComPtr<ICompressSetDecoderProperties2> setDecoderProperties;
           outCoder->QueryInterface(IID_ICompressSetDecoderProperties2, (void **)&setDecoderProperties);
           if (setDecoderProperties)
           {
             setDecoderProperties->SetDecoderProperties2(props, len);
	     printf("IID_ICompressSetDecoderProperties2 : Found (%d)\n",(int)len);
           }
        }
}

       // FIXME UInt64 outSize = 10511; // res = outCoder->Code(inStream,outStream,0,&outSize,0);
       res = outCoder->Code(inStream,outStream,0,0,0);
       inStream->close();
       outStream->close();

       if (res == S_OK)
       {
         printf("Decoding : Done\n");
       } else {
         printf("Decoding : Error\n");
         return 1;
       }
    }
    else
    {
	printf("Decoder not available\n");
        return 1;
    }
  }

  return 0;
}

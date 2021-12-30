// Windows/DLL.cpp

#include "StdAfx.h"

#ifdef __APPLE_CC__
#include <mach-o/dyld.h>
#elif ENV_BEOS
#include <kernel/image.h>
#include <Path.h>
#else
#define UINT64 DLL_UINT64 // HP-UX , dlfcn.h defines UINT64 but p7zip also defines UINT64
#include <dlfcn.h>  // dlopen ...
#undef UINT64
#endif

#include "DLL.h"
#include "Defs.h"
#ifdef _UNICODE
#include "../Common/StringConvert.h"
#endif

#define NEED_NAME_WINDOWS_TO_UNIX
#include "myPrivate.h"

// #define TRACEN(u) u;
#define TRACEN(u)  /* */

namespace NWindows {
namespace NDLL {

bool CLibrary::Free()
{
TRACEN((printf("CLibrary::Free(this=%p,%p)\n",(void *)this,(void *)_module)))
  if (_module == 0)
    return true;

#ifdef __APPLE_CC__
  int ret = NSUnLinkModule ((NSModule)_module, 0);
#elif ENV_BEOS
  int ret = unload_add_on((image_id)_module);
#else
  int ret = dlclose(_module);
#endif
TRACEN((printf("CLibrary::Free dlclose(%p)=%d\n",(void *)_module,ret)))
  if (ret != 0) return false;
  _module = 0;
  return true;
}

static FARPROC local_GetProcAddress(HMODULE module,LPCSTR lpProcName)
{
  void *ptr = 0;
  TRACEN((printf("local_GetProcAddress(%p,%s)\n",(void *)module,lpProcName)))
  if (module) {
#ifdef __APPLE_CC__
    char name[MAX_PATHNAME_LEN];
    snprintf(name,sizeof(name),"_%s",lpProcName);
    name[sizeof(name)-1] = 0;
    TRACEN((printf("NSLookupSymbolInModule(%p,%s)\n",(void *)module,name)))
    NSSymbol sym;
    sym = NSLookupSymbolInModule((NSModule)module, name);
    if (sym) {
      ptr = NSAddressOfSymbol(sym);
    } else {
      ptr = 0;
    }
#elif ENV_BEOS
    if (get_image_symbol((image_id)module, lpProcName, B_SYMBOL_TYPE_TEXT, &ptr) != B_OK)
      ptr = 0;
#else
    ptr = dlsym (module, lpProcName);
#endif
    TRACEN((printf("CLibrary::GetProc : dlsym(%p,%s)=%p\n",(void *)module,lpProcName,ptr)))
  }
  return (FARPROC)ptr;
}

FARPROC CLibrary::GetProc(LPCSTR lpProcName) const
{
  TRACEN((printf("CLibrary::GetProc(%p,%s)\n",(void *)_module,lpProcName)))
  return local_GetProcAddress(_module,lpProcName);
}

bool CLibrary::Load(LPCTSTR lpLibFileName)
{
  if(!Free())
    return false;

  void *handler = 0;
  char  name[MAX_PATHNAME_LEN+1];
#ifdef _UNICODE
  AString name2 = UnicodeStringToMultiByte(lpLibFileName);
  strcpy(name,nameWindowToUnix((const char *)name2));
#else
  strcpy(name,nameWindowToUnix(lpLibFileName));
#endif

  // replace ".dll" with ".so"
  size_t len = strlen(name);
  if ((len >=4) && (strcmp(name+len-4,".dll") == 0)) {
    strcpy(name+len-4,".so");
  }

  TRACEN((printf("CLibrary::Load(this=%p,%ls) => %s\n",(void *)this,lpLibFileName,name)))

#ifdef __APPLE_CC__
  NSObjectFileImage image;
  NSObjectFileImageReturnCode nsret;

  nsret = NSCreateObjectFileImageFromFile (name, &image);
  if (nsret == NSObjectFileImageSuccess) {
     TRACEN((printf("NSCreateObjectFileImageFromFile(%s) : OK\n",name)))
     handler = (HMODULE)NSLinkModule(image,name,NSLINKMODULE_OPTION_RETURN_ON_ERROR
           | NSLINKMODULE_OPTION_PRIVATE | NSLINKMODULE_OPTION_BINDNOW);
  } else {
     TRACEN((printf("NSCreateObjectFileImageFromFile(%s) : ERROR\n",name)))
  }
#elif ENV_BEOS
  // normalize path (remove things like "./", "..", etc..), otherwise it won't work
  BPath p(name, NULL, true);
  status_t err = B_OK;
  image_id image = load_add_on(p.Path());
TRACEN((printf("load_add_on(%s)=%d\n",p.Path(),(int)image)))
  if (image < 0) {
    err = (image_id)handler;
    handler = 0;
  } else {
    err = 0;
    handler = (HMODULE)image;
  }
#else
  int options_dlopen = 0;
#ifdef RTLD_LOCAL
  options_dlopen |= RTLD_LOCAL;
#endif
#ifdef RTLD_NOW
  options_dlopen |= RTLD_NOW;
#endif
#ifdef RTLD_GROUP
  #if ! (defined(hpux) || defined(__hpux))
  options_dlopen |= RTLD_GROUP; // mainly for solaris but not for HPUX
  #endif
#endif
  TRACEN((printf("CLibrary::Load - dlopen(%s,0x%d)\n",name,options_dlopen)))
  handler = dlopen(name,options_dlopen);
#endif // __APPLE_CC__
  TRACEN((printf("CLibrary::Load(%s) => %p\n",name,handler)))
  if (handler) {

    // Call DllMain() like in Windows : useless now

    // Propagate the value of global_use_utf16_conversion into the plugins
    int *tmp = (int *)local_GetProcAddress(handler,"global_use_utf16_conversion");
    if (tmp) *tmp = global_use_utf16_conversion;
#ifdef ENV_HAVE_LSTAT
    tmp = (int *)local_GetProcAddress(handler,"global_use_lstat");
    if (tmp) *tmp = global_use_lstat;
#endif
    // test construtors calls
    void (*fctTest)(void) = (void (*)(void))local_GetProcAddress(handler,"sync_TestConstructor");
    if (fctTest) fctTest();

  } else {
#ifdef __APPLE_CC__
    NSLinkEditErrors c;
    int num_err;
    const char *file,*err;
    NSLinkEditError(&c,&num_err,&file,&err);
    printf("Can't load '%ls' (%s)\n", lpLibFileName,err);
#elif ENV_BEOS
    printf("Can't load '%ls' (%s)\n", lpLibFileName,strerror(err));
#else
    printf("Can't load '%ls' (%s)\n", lpLibFileName,dlerror());
#endif
  }

  _module = handler;
  TRACEN((printf("CLibrary::Load(this=%p,%ls) => _module=%p\n",(void *)this,lpLibFileName,_module)))

  return true;
}


}}


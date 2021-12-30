// NewHandler.cpp
 
#include "StdAfx.h"

#include "../../C/Alloc.h"


#ifdef DONT_REDEFINE_NEW

int g_NewHandler = 0;

#else

/* An overload function for the C++ new */
void * operator new(size_t size)
{
  return MyAlloc(size);
}

/* An overload function for the C++ new[] */
void * operator new[](size_t size)
{
    return MyAlloc(size);
}

/* An overload function for the C++ delete */
void operator delete(void *pnt)
{
    MyFree(pnt);
}

/* An overload function for the C++ delete[] */
void operator delete[](void *pnt)
{
    MyFree(pnt);
}

#endif


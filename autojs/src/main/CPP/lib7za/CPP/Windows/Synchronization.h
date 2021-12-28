// Windows/Synchronization.h

#ifndef __WINDOWS_SYNCHRONIZATION_H
#define __WINDOWS_SYNCHRONIZATION_H

#include "Defs.h"

extern "C" 
{ 
#include "../../C/Threads.h"
}

#ifdef _WIN32
#include "Handle.h"
#endif

namespace NWindows {
namespace NSynchronization {

class Uncopyable {
protected: 
  Uncopyable() {} // allow construction
  ~Uncopyable() {} // and destruction of derived objects...
private:
  Uncopyable(const Uncopyable&);             // ...but prevent copying
  Uncopyable& operator=(const Uncopyable&);
};


class CBaseEvent // FIXME : private Uncopyable
{
protected:
  ::CEvent _object;
public:
  bool IsCreated() { return Event_IsCreated(&_object) != 0; }
#ifdef _WIN32
  operator HANDLE() { return _object.handle; }
#endif
  CBaseEvent() { Event_Construct(&_object); }
  ~CBaseEvent() { Close(); }
  WRes Close() { return Event_Close(&_object); }
  #ifdef _WIN32
  WRes Create(bool manualReset, bool initiallyOwn, LPCTSTR name = NULL,
      LPSECURITY_ATTRIBUTES securityAttributes = NULL)
  {
    _object.handle = ::CreateEvent(securityAttributes, BoolToBOOL(manualReset),
        BoolToBOOL(initiallyOwn), name);
    if (_object.handle != 0)
      return 0;
    return ::GetLastError();
  }
  WRes Open(DWORD desiredAccess, bool inheritHandle, LPCTSTR name)
  {
    _object.handle = ::OpenEvent(desiredAccess, BoolToBOOL(inheritHandle), name);
    if (_object.handle != 0)
      return 0;
    return ::GetLastError();
  }
  #endif

  WRes Set() { return Event_Set(&_object); }
  // bool Pulse() { return BOOLToBool(::PulseEvent(_handle)); }
  WRes Reset() { return Event_Reset(&_object); }
  WRes Lock() { return Event_Wait(&_object); }
};

class CManualResetEvent: public CBaseEvent
{
public:
  WRes Create(bool initiallyOwn = false)
  {
    return ManualResetEvent_Create(&_object, initiallyOwn ? 1: 0);
  }
  WRes CreateIfNotCreated()
  {
    if (IsCreated())
      return 0;
    return ManualResetEvent_CreateNotSignaled(&_object);
  }
  #ifdef _WIN32
  WRes CreateWithName(bool initiallyOwn, LPCTSTR name)
  {
    return CBaseEvent::Create(true, initiallyOwn, name);
  }
  #endif
};

class CAutoResetEvent: public CBaseEvent
{
public:
  WRes Create()
  {
    return AutoResetEvent_CreateNotSignaled(&_object);
  }
  WRes CreateIfNotCreated()
  {
    if (IsCreated())
      return 0;
    return AutoResetEvent_CreateNotSignaled(&_object);
  }
};

#ifdef _WIN32
class CObject: public CHandle
{
public:
  WRes Lock(DWORD timeoutInterval = INFINITE)
    { return (::WaitForSingleObject(_handle, timeoutInterval) == WAIT_OBJECT_0 ? 0 : ::GetLastError()); }
};
class CMutex: public CObject
{
public:
  WRes Create(bool initiallyOwn, LPCTSTR name = NULL,
      LPSECURITY_ATTRIBUTES securityAttributes = NULL)
  {
    _handle = ::CreateMutex(securityAttributes, BoolToBOOL(initiallyOwn), name);
    if (_handle != 0)
      return 0;
    return ::GetLastError();
  }
  WRes Open(DWORD desiredAccess, bool inheritHandle, LPCTSTR name)
  {
    _handle = ::OpenMutex(desiredAccess, BoolToBOOL(inheritHandle), name);
    if (_handle != 0)
      return 0;
    return ::GetLastError();
  }
  WRes Release() 
  { 
    return ::ReleaseMutex(_handle) ? 0 : ::GetLastError();
  }
};
class CMutexLock
{
  CMutex *_object;
public:
  CMutexLock(CMutex &object): _object(&object) { _object->Lock(); } 
  ~CMutexLock() { _object->Release(); }
};
#endif

class CSemaphore : private Uncopyable
{
  ::CSemaphore _object;
public:
  CSemaphore() { Semaphore_Construct(&_object); }
  ~CSemaphore() { Close(); }
  WRes Close() {  return Semaphore_Close(&_object); }
#ifdef _WIN32
  operator HANDLE() { return _object.handle; }
#endif
  WRes Create(UInt32 initiallyCount, UInt32 maxCount)
  {
    return Semaphore_Create(&_object, initiallyCount, maxCount);
  }
  WRes Release() { return Semaphore_Release1(&_object); }
  WRes Release(UInt32 releaseCount) { return Semaphore_ReleaseN(&_object, releaseCount); }
  WRes Lock() { return Semaphore_Wait(&_object); }
};

class CCriticalSection : private Uncopyable
{
  ::CCriticalSection _object;
public:
  CCriticalSection() { CriticalSection_Init(&_object); }
  ~CCriticalSection() { CriticalSection_Delete(&_object); }
  void Enter() { CriticalSection_Enter(&_object); }
  void Leave() { CriticalSection_Leave(&_object); }
};

class CCriticalSectionLock : private Uncopyable
{
  CCriticalSection *_object;
  void Unlock()  { _object->Leave(); }
public:
  CCriticalSectionLock(CCriticalSection &object): _object(&object) {_object->Enter(); } 
  ~CCriticalSectionLock() { Unlock(); }
};

}}

#ifndef _WIN32
#include "Synchronization2.h"
#endif

#endif


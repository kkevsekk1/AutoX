// Windows/Synchronization.h

#ifdef ENV_BEOS
#include <Locker.h>
#include <kernel/OS.h>
#include <list>
#endif

/* Remark : WFMO = WaitForMultipleObjects */

namespace NWindows { namespace NSynchronization { struct CBaseHandleWFMO; } }

typedef NWindows::NSynchronization::CBaseHandleWFMO *HANDLE;

DWORD WINAPI WaitForMultipleObjects( DWORD count, const HANDLE *handles, BOOL wait_all, DWORD timeout );

namespace NWindows {
namespace NSynchronization {

#ifdef ENV_BEOS
class CSynchro : BLocker, private Uncopyable
{
#define MAX_THREAD 256
  thread_id _waiting[MAX_THREAD]; // std::list<thread_id> _waiting;
  int index_waiting;
public:
  CSynchro() { index_waiting = 0; }
  void Create() { index_waiting = 0; }
  ~CSynchro() {}
  void Enter() { Lock(); }
  void Leave() { Unlock(); }
  void WaitCond() { 
    _waiting[index_waiting++] = find_thread(NULL); // _waiting.push_back(find_thread(NULL));
    thread_id sender;
    Unlock();
    int msg = receive_data(&sender, NULL, 0);
    Lock();
  }
  void LeaveAndSignal() {
    // Unlock();
    // Lock();
    // for (std::list<thread_id>::iterator index = _waiting.begin(); index != _waiting.end(); index++)
    for(int index = 0 ; index < index_waiting ; index++)
    {
       send_data(_waiting[index], '7zCN', NULL, 0);
    }
    index_waiting = 0; // _waiting.clear();
    Unlock();
  }
};
#else // #ifdef ENV_BEOS
#ifdef DEBUG_SYNCHRO
class CSynchro: private Uncopyable
{
  pthread_mutex_t _object;
  pthread_cond_t _cond;
  bool _isValid;
  void dump_error(int ligne,int ret,const char *text,void *param);
public:
  CSynchro();
  ~CSynchro();
  void Create();
  void Enter();
  void Leave();
  void WaitCond();
  void LeaveAndSignal();
};
#else // #ifdef DEBUG_SYNCHRO
class CSynchro : private Uncopyable
{
  pthread_mutex_t _object;
  pthread_cond_t _cond;
  bool _isValid;
public:
  CSynchro() { _isValid = false; }
  ~CSynchro() {
    if (_isValid) {
      ::pthread_mutex_destroy(&_object);
      ::pthread_cond_destroy(&_cond);
    }
    _isValid = false;
  }
  void Create() {
    ::pthread_mutex_init(&_object,0);
    ::pthread_cond_init(&_cond,0);
  }
  void Enter() { 
     ::pthread_mutex_lock(&_object);
  }
  void Leave() {
    ::pthread_mutex_unlock(&_object);
  }
  void WaitCond() { 
    ::pthread_cond_wait(&_cond, &_object);
  }
  void LeaveAndSignal() { 
    ::pthread_cond_broadcast(&_cond);
    ::pthread_mutex_unlock(&_object);
  }
};
#endif // #ifdef DEBUG_SYNCHRO
#endif // #ifdef ENV_BEOS

struct CBaseHandleWFMO // FIXME : private Uncopyable
{
  CSynchro *_sync;

  CBaseHandleWFMO() { }

  operator HANDLE() { return this; }
  virtual bool IsSignaledAndUpdate() = 0;
};

class CBaseEventWFMO : public CBaseHandleWFMO
{
  bool _manual_reset;
  bool _state;

public:

  bool IsCreated()  { return (this->_sync != 0); }
  CBaseEventWFMO()  { this->_sync = 0; } 
  ~CBaseEventWFMO() { Close(); }

  WRes Close() { this->_sync = 0; return S_OK; }

  WRes Create(CSynchro *sync,bool manualReset, bool initiallyOwn)
  {
    this->_sync         = sync;
    this->_manual_reset = manualReset;
    this->_state        = initiallyOwn;
    return S_OK;
  }

  WRes Set() {
    this->_sync->Enter();
    this->_state = true;
    this->_sync->LeaveAndSignal();
    return S_OK;
  }

  WRes Reset() {
    this->_sync->Enter();
    this->_state = false;
    this->_sync->Leave();
    return S_OK;
  }
  virtual bool IsSignaledAndUpdate() {
    if (this->_state == true) {
      if (this->_manual_reset == false) this->_state = false;
      return true;
    }
    return false;
  }
};

class CManualResetEventWFMO: public CBaseEventWFMO
{
public:
  WRes Create(CSynchro *sync,bool initiallyOwn = false) { return CBaseEventWFMO::Create(sync,true, initiallyOwn); }
};

class CAutoResetEventWFMO: public CBaseEventWFMO
{
public:
  WRes Create(CSynchro *sync) { return CBaseEventWFMO::Create(sync,false, false); }
  WRes CreateIfNotCreated(CSynchro *sync)
  {
    if (IsCreated())
      return 0;
    return CBaseEventWFMO::Create(sync,false, false);
  }
};

class CSemaphoreWFMO : public CBaseHandleWFMO
{
  LONG _count;
  LONG _maxCount;

public:
  CSemaphoreWFMO() : _count(0), _maxCount(0) { this->_sync=0;} 
  WRes Create(CSynchro *sync,LONG initiallyCount, LONG maxCount)
  {
    if ((initiallyCount < 0) || (initiallyCount > maxCount) || (maxCount < 1)) return S_FALSE;
    this->_sync     = sync;
    this->_count    = initiallyCount;
    this->_maxCount = maxCount;
    return S_OK;
  }
  WRes Release(LONG releaseCount = 1) {
    if (releaseCount < 1) return S_FALSE;

    this->_sync->Enter();
    LONG newCount = this->_count + releaseCount;
    if (newCount > this->_maxCount)
    {
      this->_sync->Leave();
      return S_FALSE;
    }
    this->_count = newCount;

    this->_sync->LeaveAndSignal();

    return S_OK;
  }
  WRes Close() { this->_sync=0; return S_OK; }

  virtual bool IsSignaledAndUpdate() {
    if (this->_count > 0) {
      this->_count--;
      return true;
    }
    return false;
  }
};

}}


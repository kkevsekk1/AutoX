// Windows/Synchronization.cpp

#include "StdAfx.h"

#include "Synchronization.h"

// #define TRACEN(u) u;
#define TRACEN(u)  /* */

#define MAGIC 0x1234CAFE
class CSynchroTest
{
  int _magic;
  public:
  CSynchroTest() {
    _magic = MAGIC;
  }
  void testConstructor() {
    if (_magic != MAGIC) {
      printf("ERROR : no constructors called during loading of plugins (please look at LINK_SHARED in makefile.machine)\n");
      exit(EXIT_FAILURE);
    }
  }
};

static CSynchroTest gbl_synchroTest;

extern "C" void sync_TestConstructor(void) {
	gbl_synchroTest.testConstructor();
}


namespace NWindows {
namespace NSynchronization {


#ifndef ENV_BEOS
#ifdef DEBUG_SYNCHRO
  void CSynchro::dump_error(int ligne,int ret,const char *text,void *param)
  {
    printf("\n##T%d#ERROR2 (l=%d) %s : param=%p ret = %d (%s)##\n",(int)pthread_self(),ligne,text,param,ret,strerror(ret));
    // abort();
  }
  CSynchro::CSynchro() {
    TRACEN((printf("\nT%d : E1-CSynchro(this=%p,m=%p,cond=%p)\n",(int)pthread_self(),(void *)this,(void *)&_object,(void *)&_cond)))
    _isValid = false;
  }

  void CSynchro::Create() {
    TRACEN((printf("\nT%d : E1-CSynchro::Create(this=%p,m=%p,cond=%p)\n",(int)pthread_self(),(void *)this,(void *)&_object,(void *)&_cond)))
    pthread_mutexattr_t mutexattr;
    memset(&mutexattr,0,sizeof(mutexattr));
    int ret = pthread_mutexattr_init(&mutexattr);
    if (ret != 0) {
	dump_error(__LINE__,ret,"pthread_mutexattr_init",&mutexattr);
    }
    ret = pthread_mutexattr_settype(&mutexattr,PTHREAD_MUTEX_ERRORCHECK);
    if (ret != 0) dump_error(__LINE__,ret,"pthread_mutexattr_settype",&mutexattr);
    ret = ::pthread_mutex_init(&_object,&mutexattr);
    if (ret != 0) dump_error(__LINE__,ret,"pthread_mutex_init",&_object);
    ret = ::pthread_cond_init(&_cond,0);
    if (ret != 0) dump_error(__LINE__,ret,"pthread_cond_init",&_cond);
    TRACEN((printf("\nT%d : E2-CSynchro::Create(m=%p,cond=%p)\n",(int)pthread_self(),(void *)&_object,(void *)&_cond)))
  }
  CSynchro::~CSynchro() {
    TRACEN((printf("\nT%d : E1-~CSynchro(this=%p,m=%p,cond=%p)\n",(int)pthread_self(),(void *)this,(void *)&_object,(void *)&_cond)))
    if (_isValid) {
      int ret = ::pthread_mutex_destroy(&_object);
      if (ret != 0) dump_error(__LINE__,ret,"pthread_mutex_destroy",&_object);
      ret = ::pthread_cond_destroy(&_cond);
      if (ret != 0) dump_error(__LINE__,ret,"pthread_cond_destroy",&_cond);
      TRACEN((printf("\nT%d : E2-~CSynchro(m=%p,cond=%p)\n",(int)pthread_self(),(void *)&_object,(void *)&_cond)))
    }
    _isValid = false;
  }
  void CSynchro::Enter() { 
    TRACEN((printf("\nT%d : E1-CSynchro::Enter(%p)\n",(int)pthread_self(),(void *)&_object)))
    int ret = ::pthread_mutex_lock(&_object);
    if (ret != 0) {
      dump_error(__LINE__,ret,"CSynchro::Enter-pthread_mutex_lock",&_object);
    }
    TRACEN((printf("\nT%d : E2-CSynchro::Enter(%p)\n",(int)pthread_self(),(void *)&_object)))
  }
  void CSynchro::Leave() {
    TRACEN((printf("\nT%d : E1-CSynchro::Leave(%p)\n",(int)pthread_self(),(void *)&_object)))
    int ret = ::pthread_mutex_unlock(&_object);
    if (ret != 0) dump_error(__LINE__,ret,"Leave::pthread_mutex_unlock",&_object);
    TRACEN((printf("\nT%d : E2-CSynchro::Leave(%p)\n",(int)pthread_self(),(void *)&_object)))
  }
  void CSynchro::WaitCond() {
    TRACEN((printf("\nT%d : E1-CSynchro::WaitCond(%p,%p)\n",(int)pthread_self(),(void *)&_cond,(void *)&_object)))
    int ret = ::pthread_cond_wait(&_cond, &_object);
    if (ret != 0) dump_error(__LINE__,ret,"pthread_cond_wait",&_cond);
    TRACEN((printf("\nT%d : E2-CSynchro::WaitCond(%p,%p)\n",(int)pthread_self(),(void *)&_cond,(void *)&_object)))
  }
  void CSynchro::LeaveAndSignal() {
    TRACEN((printf("\nT%d : E1-CSynchro::LeaveAndSignal(%p)\n",(int)pthread_self(),(void *)&_cond)))
    int ret = ::pthread_cond_broadcast(&_cond);
    if (ret != 0) dump_error(__LINE__,ret,"pthread_cond_broadcast",&_cond);
    TRACEN((printf("\nT%d : E2-CSynchro::LeaveAndSignal(%p)\n",(int)pthread_self(),(void *)&_object)))
    ret = ::pthread_mutex_unlock(&_object);
    if (ret != 0) dump_error(__LINE__,ret,"LeaveAndSignal::pthread_mutex_unlock",&_object);
    TRACEN((printf("\nT%d : E3-CSynchro::LeaveAndSignal(%p)\n",(int)pthread_self(),(void *)&_cond)))
  }
#endif
#endif

}}

DWORD WINAPI WaitForMultipleObjects( DWORD count, const HANDLE *handles, BOOL wait_all, DWORD timeout )
{
    TRACEN((printf("\nT%d : E1-WaitForMultipleObjects(%d)\n",(int)pthread_self(),(int)count)))
  if (wait_all != FALSE) {
      printf("\n\n INTERNAL ERROR - WaitForMultipleObjects(...) wait_all(%d) != FALSE\n\n",(unsigned)wait_all);
      abort();
  }

  if (timeout != INFINITE) {
      printf("\n\n INTERNAL ERROR - WaitForMultipleObjects(...) timeout(%u) != INFINITE\n\n",(unsigned)timeout);
      abort();
  }

  if (count < 1) {
      printf("\n\n INTERNAL ERROR - WaitForMultipleObjects(...) count(%u) < 1\n\n",(unsigned)count);
      abort();
  }

  NWindows::NSynchronization::CSynchro *synchro = handles[0]->_sync;

  TRACEN((printf("\nT%d : E2-WaitForMultipleObjects(%d)\n",(int)pthread_self(),(int)count)))
  synchro->Enter();
  TRACEN((printf("\nT%d : E3-WaitForMultipleObjects(%d)\n",(int)pthread_self(),(int)count)))

#ifdef DEBUG_SYNCHRO
  for(DWORD i=1;i<count;i++) {
    if (synchro != handles[i]->_sync) {
      printf("\n\n INTERNAL ERROR - WaitForMultipleObjects(...) synchro(%p) != handles[%d]->_sync(%p)\n\n",
              synchro,(unsigned)i,handles[i]->_sync);
      abort();
    }
  }
#endif

  while(1) {
    for(DWORD i=0;i<count;i++) {
      if (handles[i]->IsSignaledAndUpdate()) {
        synchro->Leave();
  TRACEN((printf("\nT%d : E4-WaitForMultipleObjects(%d)\n",(int)pthread_self(),(int)count)))
        return WAIT_OBJECT_0+i;
      }
    }
    synchro->WaitCond();
  }
  synchro->Leave();
  return ETIMEDOUT; // WAIT_TIMEOUT;
}


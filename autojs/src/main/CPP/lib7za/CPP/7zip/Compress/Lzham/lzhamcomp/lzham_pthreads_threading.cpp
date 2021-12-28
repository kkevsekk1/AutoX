// File: lzham_task_pool_pthreads.cpp
// See Copyright Notice and license at the end of include/lzham.h
#include "lzham_core.h"
#include "lzham_pthreads_threading.h"
#include "lzham_timer.h"

#ifdef WIN32
#include <process.h>
#endif

#if defined(__GNUC__) && !defined(__APPLE__) && !defined(__MINGW32__) && !defined(__FreeBSD__)
#include <sys/sysinfo.h>
#endif

#if LZHAM_USE_PTHREADS_API

#ifdef WIN32
#pragma comment(lib, "../ext/libpthread/lib/pthreadVC2.lib")
#endif

namespace lzham
{
   task_pool::task_pool(lzham_malloc_context malloc_context) :
      m_task_stack(malloc_context),
      m_num_threads(0),
      m_tasks_available(0, 32767),
      m_malloc_context(malloc_context),
      m_num_outstanding_tasks(0),
      m_exit_flag(false)
   {
      utils::zero_object(m_threads);
   }

   task_pool::task_pool(lzham_malloc_context malloc_context, uint num_threads) :
      m_task_stack(malloc_context),
      m_num_threads(0),
      m_tasks_available(0, 32767),
      m_malloc_context(malloc_context),
      m_num_outstanding_tasks(0),
      m_exit_flag(false)
   {
      utils::zero_object(m_threads);

      bool status = init(num_threads);
      LZHAM_VERIFY(status);
   }

   task_pool::~task_pool()
   {
      deinit();
   }

   bool task_pool::init(uint num_threads)
   {
      LZHAM_ASSERT(num_threads <= cMaxThreads);
      num_threads = math::minimum<uint>(num_threads, cMaxThreads);

      deinit();

      bool succeeded = true;

      m_num_threads = 0;
      while (m_num_threads < num_threads)
      {
         int status = pthread_create(&m_threads[m_num_threads], NULL, thread_func, this);
         if (status)
         {
            succeeded = false;
            break;
         }

         m_num_threads++;
      }

      if (!succeeded)
      {
         deinit();
         return false;
      }

      return true;
   }

   void task_pool::deinit()
   {
      if (m_num_threads)
      {
         join();

         atomic_exchange32(&m_exit_flag, true);

         m_tasks_available.release(m_num_threads);

         for (uint i = 0; i < m_num_threads; i++)
            pthread_join(m_threads[i], NULL);

         m_num_threads = 0;

         atomic_exchange32(&m_exit_flag, false);
      }

      m_task_stack.clear();
      m_num_outstanding_tasks = 0;
   }

   bool task_pool::queue_task(task_callback_func pFunc, uint64 data, void* pData_ptr)
   {
      LZHAM_ASSERT(m_num_threads);
      LZHAM_ASSERT(pFunc);

      task tsk;
      tsk.m_callback = pFunc;
      tsk.m_data = data;
      tsk.m_pData_ptr = pData_ptr;
      tsk.m_flags = 0;

      if (!m_task_stack.try_push(tsk))
         return false;

      atomic_increment32(&m_num_outstanding_tasks);

      m_tasks_available.release(1);

      return true;
   }

   // It's the object's responsibility to delete pObj within the execute_task() method, if needed!
   bool task_pool::queue_task(executable_task* pObj, uint64 data, void* pData_ptr)
   {
      LZHAM_ASSERT(m_num_threads);
      LZHAM_ASSERT(pObj);

      task tsk;
      tsk.m_pObj = pObj;
      tsk.m_data = data;
      tsk.m_pData_ptr = pData_ptr;
      tsk.m_flags = cTaskFlagObject;

      if (!m_task_stack.try_push(tsk))
         return false;

      atomic_increment32(&m_num_outstanding_tasks);

      m_tasks_available.release(1);

      return true;
   }

   void task_pool::process_task(task& tsk)
   {
      if (tsk.m_flags & cTaskFlagObject)
         tsk.m_pObj->execute_task(tsk.m_data, tsk.m_pData_ptr);
      else
         tsk.m_callback(tsk.m_data, tsk.m_pData_ptr);

      atomic_decrement32(&m_num_outstanding_tasks);
   }

   void task_pool::join()
   {
      task tsk;
      while (atomic_add32(&m_num_outstanding_tasks, 0) > 0)
      {
         if (m_task_stack.pop(tsk))
         {
            process_task(tsk);
         }
         else
         {
            lzham_sleep(1);
         }
      }
   }

   void * task_pool::thread_func(void *pContext)
   {
      task_pool* pPool = static_cast<task_pool*>(pContext);
      task tsk;

      for ( ; ; )
      {
         if (!pPool->m_tasks_available.wait())
            break;

         if (pPool->m_exit_flag)
            break;

         if (pPool->m_task_stack.pop(tsk))
         {
            pPool->process_task(tsk);
         }
      }

      return NULL;
   }

   uint lzham_get_max_helper_threads()
   {
#if defined(__APPLE__) || defined(__FreeBSD__)
      int num_procs = static_cast<int>(sysconf(_SC_NPROCESSORS_ONLN));
      return (num_procs >= 1) ? (num_procs - 1) : 0;
#elif (1)
      uint num_procs = get_nprocs();
      return num_procs ? (num_procs - 1) : 0;
#else
      printf("TODO: lzham_get_max_helper_threads(): Implement system specific func to determine the max # of helper threads\n");
      // Just assume a dual-core machine.
      return 1;
#endif
   }

} // namespace lzham

#endif // LZHAM_USE_PTHREADS_API

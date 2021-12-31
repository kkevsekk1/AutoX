// File: lzham_task_pool_pthreads.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

#if LZHAM_USE_PTHREADS_API

#if LZHAM_NO_ATOMICS
#error No atomic operations defined in lzham_platform.h!
#endif

#ifdef __APPLE__
   #include <libkern/OSAtomic.h>
#endif

#include <pthread.h>
#include <semaphore.h>
#include <unistd.h>
#include <sys/time.h>

#define LZHAM_RND_CONG(jcong)  (69069U * jcong + 1234567U)

namespace lzham
{
   // semaphore

#ifdef __APPLE__
   class semaphore
   {
      LZHAM_NO_COPY_OR_ASSIGNMENT_OP(semaphore);

   public:
      inline semaphore(long initialCount = 0, long maximumCount = 1)
      {
         LZHAM_NOTE_UNUSED(maximumCount);
         LZHAM_ASSERT(maximumCount >= initialCount);

         for (uint tries = 0; tries < 16; tries++)
         {
            struct timeval tp;
            struct timezone tzp;
            gettimeofday(&tp, &tzp);
            uint x = tp.tv_usec;

            // Argh this stinks. Try to choose a name that won't conflict with anything the calling process uses.
            for (uint i = 0; i < sizeof(m_name) - 1; i++)
            {
               x = LZHAM_RND_CONG(x);
               char c = 'A' + (static_cast<uint8>(x ^ (x >> 20)) % 26);
               m_name[i] = c;
            }
            m_name[sizeof(m_name) - 1] = '\0';

            m_pSem = sem_open(m_name, O_CREAT | O_EXCL, S_IRWXU, initialCount);
            if (m_pSem != SEM_FAILED)
               break;
         }

         if (m_pSem == SEM_FAILED)
         {
            LZHAM_FAIL("semaphore: sem_init() failed");
         }
      }

      inline ~semaphore()
      {
         sem_close(m_pSem);
         sem_unlink(m_name);
      }

      inline void release(long releaseCount = 1)
      {
         LZHAM_ASSERT(releaseCount >= 1);

         int status = 0;
#ifdef WIN32
         if (1 == releaseCount)
            status = sem_post(m_pSem);
         else
            status = sem_post_multiple(m_pSem, releaseCount);
#else
         while (releaseCount > 0)
         {
            status = sem_post(m_pSem);
            if (status)
               break;
            releaseCount--;
         }
#endif

         if (status)
         {
            LZHAM_FAIL("semaphore: sem_post() or sem_post_multiple() failed");
         }
      }

      inline bool wait()
      {
         int status = sem_wait(m_pSem);

         if (status)
         {
            if (errno != ETIMEDOUT)
            {
               LZHAM_FAIL("semaphore: sem_wait() or sem_timedwait() failed");
            }
            return false;
         }

         return true;
      }

   private:
      sem_t *m_pSem;
      char m_name[16];
   };
#else
   class semaphore
   {
      LZHAM_NO_COPY_OR_ASSIGNMENT_OP(semaphore);

   public:
      inline semaphore(long initialCount = 0, long maximumCount = 1, const char* pName = NULL)
      {
         LZHAM_NOTE_UNUSED(maximumCount), LZHAM_NOTE_UNUSED(pName);
         LZHAM_ASSERT(maximumCount >= initialCount);
         if (sem_init(&m_sem, 0, initialCount))
         {
            LZHAM_FAIL("semaphore: sem_init() failed");
         }
      }

      inline ~semaphore()
      {
         sem_destroy(&m_sem);
      }

      inline void release(long releaseCount = 1)
      {
         LZHAM_ASSERT(releaseCount >= 1);

         int status = 0;
#ifdef WIN32
         if (1 == releaseCount)
            status = sem_post(&m_sem);
         else
            status = sem_post_multiple(&m_sem, releaseCount);
#else
         while (releaseCount > 0)
         {
            status = sem_post(&m_sem);
            if (status)
               break;
            releaseCount--;
         }
#endif

         if (status)
         {
            LZHAM_FAIL("semaphore: sem_post() or sem_post_multiple() failed");
         }
      }

      inline bool wait(uint32 milliseconds = cUINT32_MAX)
      {
         int status;
         if (milliseconds == cUINT32_MAX)
         {
            status = sem_wait(&m_sem);
         }
         else
         {
            struct timespec interval;
            interval.tv_sec = milliseconds / 1000;
            interval.tv_nsec = (milliseconds % 1000) * 1000000L;
            status = sem_timedwait(&m_sem, &interval);
         }

         if (status)
         {
            if (errno != ETIMEDOUT)
            {
               LZHAM_FAIL("semaphore: sem_wait() or sem_timedwait() failed");
            }
            return false;
         }

         return true;
      }

   private:
      sem_t m_sem;
   };
#endif
   
   // spinlock   

#ifdef __APPLE__
   class spinlock
   {
   public:
      inline spinlock() : m_lock(0)
      {
      }

      inline ~spinlock()
      {
      }

      inline void lock()
      {
         OSSpinLockLock(&m_lock);
      }

      inline void unlock()
      {
         OSSpinLockUnlock(&m_lock);
      }

   private:

      OSSpinLock m_lock;
   };
#else
   class spinlock
   {
   public:
      inline spinlock()
      {
         if (pthread_spin_init(&m_spinlock, 0))
         {
            LZHAM_FAIL("spinlock: pthread_spin_init() failed");
         }
      }

      inline ~spinlock()
      {
         pthread_spin_destroy(&m_spinlock);
      }

      inline void lock()
      {
         if (pthread_spin_lock(&m_spinlock))
         {
            LZHAM_FAIL("spinlock: pthread_spin_lock() failed");
         }
      }

      inline void unlock()
      {
         if (pthread_spin_unlock(&m_spinlock))
         {
            LZHAM_FAIL("spinlock: pthread_spin_unlock() failed");
         }
      }

   private:
      pthread_spinlock_t m_spinlock;
   };
#endif // __APPLE__

   // Thread safe stack

   template<typename T, uint cMaxSize>
   class tsstack
   {
   public:
      inline tsstack(lzham_malloc_context malloc_context) :
         m_top(0)
      {
         LZHAM_NOTE_UNUSED(malloc_context);
      }

      inline ~tsstack()
      {
      }

      inline void clear()
      {
         m_spinlock.lock();
         m_top = 0;
         m_spinlock.unlock();
      }

      inline bool try_push(const T& obj)
      {
         bool result = false;
         m_spinlock.lock();
         if (m_top < (int)cMaxSize)
         {
            m_stack[m_top++] = obj;
            result = true;
         }
         m_spinlock.unlock();
         return result;
      }

      inline bool pop(T& obj)
      {
         bool result = false;
         m_spinlock.lock();
         if (m_top > 0)
         {
            obj = m_stack[--m_top];
            result = true;
         }
         m_spinlock.unlock();
         return result;
      }

   private:
      spinlock m_spinlock;
      T m_stack[cMaxSize];
      int m_top;
   };
   
   // Simple task pool

   class task_pool
   {
   public:
      task_pool(lzham_malloc_context malloc_context);
      task_pool(lzham_malloc_context malloc_context, uint num_threads);
      ~task_pool();

      lzham_malloc_context get_malloc_context() const { return m_malloc_context; }

      enum { cMaxThreads = LZHAM_MAX_HELPER_THREADS };
      bool init(uint num_threads);
      void deinit();

      inline uint get_num_threads() const { return m_num_threads; }
      inline uint get_num_outstanding_tasks() const { return static_cast<uint>(m_num_outstanding_tasks); }

      // C-style task callback
      typedef void (*task_callback_func)(uint64 data, void* pData_ptr);
      bool queue_task(task_callback_func pFunc, uint64 data = 0, void* pData_ptr = NULL);

      class executable_task
      {
      public:
         virtual void execute_task(uint64 data, void* pData_ptr) = 0;
      };

      // It's the caller's responsibility to delete pObj within the execute_task() method, if needed!
      bool queue_task(executable_task* pObj, uint64 data = 0, void* pData_ptr = NULL);

      template<typename S, typename T>
      inline bool queue_object_task(S* pObject, T pObject_method, uint64 data = 0, void* pData_ptr = NULL);

      template<typename S, typename T>
      inline bool queue_multiple_object_tasks(S* pObject, T pObject_method, uint64 first_data, uint num_tasks, void* pData_ptr = NULL);

      void join();

   private:
      struct task
      {
         inline task() : m_data(0), m_pData_ptr(NULL), m_pObj(NULL), m_flags(0) { }

         uint64 m_data;
         void* m_pData_ptr;

         union
         {
            task_callback_func m_callback;
            executable_task* m_pObj;
         };

         uint m_flags;
      };

      tsstack<task, cMaxThreads> m_task_stack;

      uint m_num_threads;
      pthread_t m_threads[cMaxThreads];

      semaphore m_tasks_available;
      
      lzham_malloc_context m_malloc_context;

      enum task_flags
      {
         cTaskFlagObject = 1
      };

      volatile atomic32_t m_num_outstanding_tasks;
      volatile atomic32_t m_exit_flag;

      void process_task(task& tsk);

      static void* thread_func(void *pContext);
   };

   enum object_task_flags
   {
      cObjectTaskFlagDefault = 0,
      cObjectTaskFlagDeleteAfterExecution = 1
   };

   template<typename T>
   class object_task : public task_pool::executable_task
   {
   public:
      object_task(lzham_malloc_context malloc_context, uint flags = cObjectTaskFlagDefault) :
         m_malloc_context(malloc_context),
         m_pObject(NULL),
         m_pMethod(NULL),
         m_flags(flags)
      {
      }

      typedef void (T::*object_method_ptr)(uint64 data, void* pData_ptr);

      object_task(lzham_malloc_context malloc_context, T* pObject, object_method_ptr pMethod, uint flags = cObjectTaskFlagDefault) :
         m_malloc_context(malloc_context),
         m_pObject(pObject),
         m_pMethod(pMethod),
         m_flags(flags)
      {
         LZHAM_ASSERT(pObject && pMethod);
      }

      void init(lzham_malloc_context malloc_context, T* pObject, object_method_ptr pMethod, uint flags = cObjectTaskFlagDefault)
      {
         LZHAM_ASSERT(pObject && pMethod);
         
         m_malloc_context = malloc_context;
         m_pObject = pObject;
         m_pMethod = pMethod;
         m_flags = flags;
      }

      lzham_malloc_context get_malloc_context() const { return m_malloc_context; }

      T* get_object() const { return m_pObject; }
      object_method_ptr get_method() const { return m_pMethod; }

      virtual void execute_task(uint64 data, void* pData_ptr)
      {
         (m_pObject->*m_pMethod)(data, pData_ptr);

         if (m_flags & cObjectTaskFlagDeleteAfterExecution)
            lzham_delete(m_malloc_context, this);
      }

   protected:
      lzham_malloc_context m_malloc_context;
      T* m_pObject;

      object_method_ptr m_pMethod;

      uint m_flags;
   };

   template<typename S, typename T>
   inline bool task_pool::queue_object_task(S* pObject, T pObject_method, uint64 data, void* pData_ptr)
   {
      object_task<S> *pTask = lzham_new< object_task<S> >(m_malloc_context, m_malloc_context, pObject, pObject_method, cObjectTaskFlagDeleteAfterExecution);
      if (!pTask)
         return false;
      return queue_task(pTask, data, pData_ptr);
   }

   template<typename S, typename T>
   inline bool task_pool::queue_multiple_object_tasks(S* pObject, T pObject_method, uint64 first_data, uint num_tasks, void* pData_ptr)
   {
      LZHAM_ASSERT(m_num_threads);
      LZHAM_ASSERT(pObject);
      LZHAM_ASSERT(num_tasks);
      if (!num_tasks)
         return true;

      bool status = true;

      uint total_to_release = 0;

      for (int i = num_tasks - 1; i >= 0; --i)
      {
         task tsk;

         tsk.m_pObj = lzham_new< object_task<S> >(m_malloc_context, m_malloc_context, pObject, pObject_method, cObjectTaskFlagDeleteAfterExecution);
         if (!tsk.m_pObj)
         {
            status = false;
            break;
         }

         tsk.m_data = first_data + i;
         tsk.m_pData_ptr = pData_ptr;
         tsk.m_flags = cTaskFlagObject;

         if (!m_task_stack.try_push(tsk))
         {
            status = false;
            break;
         }

         total_to_release++;
      }

      if (total_to_release)
      {
         atomic_add32(&m_num_outstanding_tasks, total_to_release);

         m_tasks_available.release(total_to_release);
      }

      return status;
   }
   
   // Sleep

   inline void lzham_sleep(unsigned int milliseconds)
   {
#ifdef WIN32
      struct timespec interval;
      interval.tv_sec = milliseconds / 1000;
      interval.tv_nsec = (milliseconds % 1000) * 1000000L;
      pthread_delay_np(&interval);
#else
      while (milliseconds)
      {
         int msecs_to_sleep = LZHAM_MIN(milliseconds, 1000);
         usleep(msecs_to_sleep * 1000);
         milliseconds -= msecs_to_sleep;
      }
#endif
   }

   // Returns number of helper threads we can add to the process on the current system (i.e. for a 4 CPU system this returns 3).
   uint lzham_get_max_helper_threads();

} // namespace lzham

#endif // LZHAM_USE_PTHREADS_API

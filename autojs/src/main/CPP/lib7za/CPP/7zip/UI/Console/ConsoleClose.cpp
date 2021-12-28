// ConsoleClose.cpp

#include "StdAfx.h"

#include "ConsoleClose.h"

#include <signal.h>

static int g_BreakCounter = 0;
static const int kBreakAbortThreshold = 2;

namespace NConsoleClose {

static void HandlerRoutine(int)
{
  g_BreakCounter++;
  if (g_BreakCounter < kBreakAbortThreshold)
    return ;
  exit(EXIT_FAILURE);
}

bool TestBreakSignal()
{
  return (g_BreakCounter > 0);
}

void CheckCtrlBreak()
{
  if (TestBreakSignal())
    throw CCtrlBreakException();
}

CCtrlHandlerSetter::CCtrlHandlerSetter()
{
   memo_sig_int = signal(SIGINT,HandlerRoutine); // CTRL-C
   if (memo_sig_int == SIG_ERR)
    throw "SetConsoleCtrlHandler fails (SIGINT)";
   memo_sig_term = signal(SIGTERM,HandlerRoutine); // for kill -15 (before "kill -9")
   if (memo_sig_term == SIG_ERR)
    throw "SetConsoleCtrlHandler fails (SIGTERM)";
}

CCtrlHandlerSetter::~CCtrlHandlerSetter()
{
   signal(SIGINT,memo_sig_int); // CTRL-C
   signal(SIGTERM,memo_sig_term); // kill {pid}
}

}

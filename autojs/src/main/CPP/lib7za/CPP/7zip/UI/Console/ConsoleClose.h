// ConsoleCloseUtils.h

#ifndef __CONSOLECLOSEUTILS_H
#define __CONSOLECLOSEUTILS_H

namespace NConsoleClose {

bool TestBreakSignal();

class CCtrlHandlerSetter
{
  void (*memo_sig_int)(int);
  void (*memo_sig_term)(int);
public:
  CCtrlHandlerSetter();
  virtual ~CCtrlHandlerSetter();
};

class CCtrlBreakException 
{};

void CheckCtrlBreak();

}

#endif

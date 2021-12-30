// UserInputUtils.cpp

#include "StdAfx.h"

#include "../../../Common/StdInStream.h"
#include "../../../Common/StringConvert.h"

#include "UserInputUtils.h"

#ifdef USE_FLTK
// the programs like file-roller or xarchiver do not support archives with password
// these programs freeze because p7zip is waiting for a password
// defining USE_FLTK allows p7zip to use a popup in order to ask the password.
#include <FL/Fl.H>
#include <FL/Fl_Window.H>
#include <FL/fl_ask.H>
#else
#ifdef ENV_HAVE_GETPASS
#include <pwd.h>
#include <unistd.h>
#include "Common/MyException.h"
#endif
#endif

static const char kYes = 'y';
static const char kNo = 'n';
static const char kYesAll = 'a';
static const char kNoAll = 's';
static const char kAutoRenameAll = 'u';
static const char kQuit = 'q';

static const char *kFirstQuestionMessage = "? ";
static const char *kHelpQuestionMessage =
  "(Y)es / (N)o / (A)lways / (S)kip all / A(u)to rename all / (Q)uit? ";

// return true if pressed Quite;

NUserAnswerMode::EEnum ScanUserYesNoAllQuit(CStdOutStream *outStream)
{
  if (outStream)
    *outStream << kFirstQuestionMessage;
  for (;;)
  {
    if (outStream)
    {
      *outStream << kHelpQuestionMessage;
      outStream->Flush();
    }
    AString scannedString = g_StdIn.ScanStringUntilNewLine();
    scannedString.Trim();
    if (!scannedString.IsEmpty())
      switch(::MyCharLower_Ascii(scannedString[0]))
      {
        case kYes:    return NUserAnswerMode::kYes;
        case kNo:     return NUserAnswerMode::kNo;
        case kYesAll: return NUserAnswerMode::kYesAll;
        case kNoAll:  return NUserAnswerMode::kNoAll;
        case kAutoRenameAll: return NUserAnswerMode::kAutoRenameAll;
        case kQuit:   return NUserAnswerMode::kQuit;
      }
  }
}

#ifdef _WIN32
#ifndef UNDER_CE
#define MY_DISABLE_ECHO
#endif
#endif

#ifdef ENV_HAVE_GETPASS
#define MY_DISABLE_ECHO
#endif

UString GetPassword(CStdOutStream *outStream,bool verify)
{
#ifdef USE_FLTK 
  const char *r = fl_password("Enter password", 0);
  AString oemPassword = "";
  if (r) oemPassword = r;
  return MultiByteToUnicodeString(oemPassword, CP_OEMCP);
#else /* USE_FLTK */
  if (outStream)
  {
    *outStream << "\nEnter password"
      #ifdef MY_DISABLE_ECHO
      " (will not be echoed)"
      #endif
      ":";
    outStream->Flush();
  }
#ifdef ENV_HAVE_GETPASS
  AString oemPassword = getpass("");
  if ( (verify) && (outStream) )
  {
    (*outStream) << "Verify password (will not be echoed) :";
    outStream->Flush();
    AString oemPassword2 = getpass("");
    if (oemPassword != oemPassword2) throw "password verification failed";
  }
  return MultiByteToUnicodeString(oemPassword, CP_OEMCP);
#else
  return g_StdIn.ScanUStringUntilNewLine();
#endif
#endif /* USE_FLTK */
}

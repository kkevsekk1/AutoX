// Windows/Control/Edit.h

#ifndef __WINDOWS_CONTROL_EDIT_H
#define __WINDOWS_CONTROL_EDIT_H

#include "Windows/Window.h"
#include "Windows/Defs.h"

namespace NWindows {
namespace NControl {

class CEdit: public CWindow
{
public:
	void SetPasswordChar(WPARAM c);
	void Show(int cmdShow);
	void Show_Bool(bool show) { Show(show ? SW_SHOW: SW_HIDE); }
	virtual void SetText(LPCWSTR s);
	virtual bool GetText(CSysString &s);
};

}}

#endif


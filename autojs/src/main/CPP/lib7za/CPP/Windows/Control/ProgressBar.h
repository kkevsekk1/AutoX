// Windows/Control/ProgressBar.h

#ifndef __WINDOWS_CONTROL_PROGRESSBAR_H
#define __WINDOWS_CONTROL_PROGRESSBAR_H

#include "Windows/Window.h"
#include "Windows/Defs.h"

class wxGauge;

namespace NWindows {
namespace NControl {


class CProgressBar : public CWindow
{
protected:
	wxGauge* _window;
	int _minValue;
	int _range;
public:
	CProgressBar(wxWindow* newWindow = NULL);

	void Attach(wxWindow* newWindow);

	void SetRange32(int minValue, int maxValue);

	void SetPos(int pos);
};

}}

#endif


// Windows/Control/Static.h

#ifndef __WINDOWS_CONTROL_STATIC_H
#define __WINDOWS_CONTROL_STATIC_H

#include "Windows/Window.h"
#include "Windows/Defs.h"

typedef void * HICON;

namespace NWindows {
namespace NControl {

class CStatic : public CWindow
{
public:

	HICON SetIcon(HICON icon) { return 0; } // FIXME
};

}}

#endif

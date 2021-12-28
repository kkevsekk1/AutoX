// Windows/Control/DialogImpl.h

#ifndef __WINDOWS_CONTROL_DIALOGIMPL_H
#define __WINDOWS_CONTROL_DIALOGIMPL_H

#include "Windows/Window.h"
#include "Windows/Control/Dialog.h"

void myCreateHandle(int n); // FIXME - duplicate

enum {
    WORKER_EVENT=100    // this one gets sent from the worker thread
};

namespace NWindows {
	namespace NControl {

#define TIMER_ID_IMPL (1234)

		class CModalDialogImpl : public wxDialog
		{
			wxTimer _timer;

			CDialog *_dialog;
		public:
			CModalDialogImpl(CDialog *dialog, wxWindow* parent, wxWindowID id, const wxString& title,
				       	const wxPoint& pos = wxDefaultPosition, const wxSize& size = wxDefaultSize,
				       	long style = wxDEFAULT_DIALOG_STYLE );

			CDialog * Detach()
			{
				CDialog * oldDialog = _dialog;
				_dialog = NULL;
				return oldDialog;
			}

			void OnInit()
			{
				if (_dialog) _dialog->OnInit(this);
			}

			void OnAnyButton(wxCommandEvent& event);
			void OnAnyChoice(wxCommandEvent &event);
			void OnAnyTimer(wxTimerEvent &event);

/* FIXME			virtual void SetLabel(const wxString &title)
			{
				// Why we must do this "alias" ?
				this->SetTitle(title);
			}
*/
			//////////////////
			UINT_PTR SetTimer(UINT_PTR /* FIXME idEvent */, unsigned milliseconds)
			{
				_timer.Start(milliseconds);
				return TIMER_ID_IMPL;
			}
			void KillTimer(UINT_PTR idEvent)
			{
				if (idEvent == TIMER_ID_IMPL) _timer.Stop();
			}
			void OnWorkerEvent(wxCommandEvent& event)
			{
				int n = event.GetInt();
				// printf("CModalDialogImpl::OnWorkerEvent(n=%d)\n",n);
				myCreateHandle(n);
			}
		};
}
}

#endif


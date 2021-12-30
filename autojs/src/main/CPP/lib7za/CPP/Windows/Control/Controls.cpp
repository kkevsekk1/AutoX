// Dialog.cpp

#include "StdAfx.h"

// For compilers that support precompilation, includes "wx/wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
    #pragma hdrstop
#endif

// for all others, include the necessary headers (this file is usually all you
// need because it includes almost all "standard" wxWidgets headers)
#ifndef WX_PRECOMP
    #include "wx/wx.h"
    #include "wx/imaglist.h"
    #include "wx/listctrl.h"
#endif

#undef _WIN32

#include "Windows/Control/Dialog.h"

void verify_main_thread(void);

class LockGUI
{
    bool _IsMain;
    public:
        LockGUI() {
            verify_main_thread();
            _IsMain = wxThread::IsMain();
            if (!_IsMain) {
                // DEBUG
                printf("GuiEnter-Controls(0x%lx)\n",wxThread::GetCurrentId());
                abort(); // FIXME wxMutexGuiEnter();
            }
            }
        ~LockGUI() {
            if (!_IsMain) {
                wxMutexGuiLeave();
                // DEBUG printf("GuiLeave(0x%lx)\n",wxThread::GetCurrentId());
            }
            }
};
/////////////////////////

static const wxString CLASS_NAME_wxStaticText = wxT("wxStaticText");
static const wxString CLASS_NAME_wxTextCtrl = wxT("wxTextCtrl");

namespace NWindows {
    namespace NControl {

        void CDialogChildControl::SetText(LPCWSTR s)
        {
            LockGUI lock;
            const wxChar * class_name = _window->GetClassInfo()->GetClassName ();

            if ( CLASS_NAME_wxStaticText == class_name) {
                ((wxStaticText *)_window)->SetLabel(s);
            } else if ( CLASS_NAME_wxTextCtrl == class_name) {
                ((wxTextCtrl *)_window)->SetLabel(s);
            } else {
                // ((wxControl *)_window)->SetValue(s); // FIXME
                printf("INTERNAL ERROR - CDialogChildControl::SetText(class=%ls) not implemented\n",class_name);
                exit(-1);
            }
        }

        bool CDialogChildControl::GetText(CSysString &s)
        {
            wxString str;
            {
                LockGUI lock;
                const wxChar * class_name = _window->GetClassInfo()->GetClassName ();
                if ( CLASS_NAME_wxStaticText == class_name) {
                    str = ((wxStaticText *)_window)->GetLabel();
                } else if ( CLASS_NAME_wxTextCtrl == class_name) {
                    str = ((wxTextCtrl *)_window)->GetLabel();
                } else {
                    // FIXME str = ((wxTextCtrl *)_window)->GetValue();
                    printf("INTERNAL ERROR - CDialogChildControl::GetText(class=%ls) not implemented\n",class_name);
                    exit(-1);
                }
            }
            s = str;
            return true;
        }
    }
}

///////////////////////// Windows/Control/ComboBox.cpp
#include "Windows/Control/ComboBox.h"

namespace NWindows {
    namespace NControl {

        void CComboBox::Attach(wxWindow * newWindow) { _choice = (wxComboBox*)newWindow; }

        wxWindow * CComboBox::Detach()
        {
            wxWindow * window = _choice;
            _choice = NULL;
            return window;
        }

        CComboBox::operator HWND() const { return (HWND)_choice; }


            int CComboBox::AddString(const TCHAR * txt) {
                LockGUI lock;
                wxString item(txt);
                return _choice->Append(item);
            }

            void CComboBox::SetText(LPCTSTR s) {
                LockGUI lock;
                wxString str(s);
                _choice->SetValue(str);
            }

            void CComboBox::GetText(CSysString &s) {
                LockGUI lock;
                wxString str = _choice->GetValue();
                s = str;
            }

            int CComboBox::GetCount() const  {
                LockGUI lock;
                    return _choice->GetCount();
            }

            void CComboBox::GetLBText(int index, CSysString &s) {
                LockGUI lock;
                wxString str = _choice->GetString(index);
                s = str;
            }

            void CComboBox::SetCurSel(int index) {
                LockGUI lock;
                    _choice->SetSelection(index);
            }

            int CComboBox::GetCurSel() {
                LockGUI lock;
                    return _choice->GetSelection();
            }

            void CComboBox::SetItemData(int index, int val) {
                LockGUI lock;
                    _choice->SetClientData( index, (void *)(((char *)0) + val));
                }

            LRESULT CComboBox::GetItemData(int index)
            {
                LockGUI lock;
                void * data = _choice->GetClientData(index);
                LRESULT ret = (LRESULT)(((char *)data) - ((char *)0));
                return ret;
            }

            void CComboBox::Enable(bool state) {
                LockGUI lock;
                    _choice->Enable(state);
            }

            void CComboBox::ResetContent() {
                LockGUI lock;
                   _choice->Clear();
                }
    }
}

///////////////////////// Windows/Control/Edit.cpp
#include "Windows/Control/Edit.h"

namespace NWindows {
    namespace NControl {

        void CEdit::SetPasswordChar(WPARAM c)  // Warning : does not work for wxMSW
        {
                LockGUI lock;
            long style = _window->GetWindowStyle();
            if ( c == 0 ) style &= ~(wxTE_PASSWORD);
            else          style |= wxTE_PASSWORD;
            _window->SetWindowStyle(style);
            _window->Refresh();
        }


        void CEdit::Show(int cmdShow)
        {
                LockGUI lock;
            // FIXME    _window->Show(cmdShow != SW_HIDE);
            _window->Enable(cmdShow != SW_HIDE);
        }

        void CEdit::SetText(LPCWSTR s)
        {
            LockGUI lock;
            ((wxTextCtrl *)_window)->SetValue(s);
        }

        bool CEdit::GetText(CSysString &s)
        {
            wxString str;
            {
                LockGUI lock;
                str = ((wxTextCtrl *)_window)->GetValue();
            }
            s = str;
            return true;
        }

    }
}

///////////////////////// Windows/Control/ProgressBar.cpp
#include "Windows/Control/ProgressBar.h"

namespace NWindows {
    namespace NControl {

        CProgressBar::CProgressBar(wxWindow* newWindow):
                _window((wxGauge *)newWindow) , _minValue(0), _range(0) { }

    void CProgressBar::Attach(wxWindow* newWindow) {
        _window = (wxGauge *)newWindow;
        _minValue = 0;
        _range = 0;
    }

    void CProgressBar::SetRange32(int minValue, int maxValue) {
        int range = maxValue - minValue;
        if (range >= 1)
        {
                LockGUI lock;
            _minValue = minValue;
            _range    = range;
            _window->SetRange(_range);
        }
    }

    void CProgressBar::SetPos(int pos) {
        if (_range >= 1)
        {
                LockGUI lock;
            int value = pos - _minValue;
            if ((value >= 0) && (value <= _range)) _window->SetValue(value);
        }
    }

    }
}

///////////////////////// Windows/Control/StatusBar.cpp
#include "Windows/Control/StatusBar.h"

namespace NWindows {
    namespace NControl {

        void CStatusBar::Attach(wxWindow * newWindow) { _statusBar = (wxStatusBar*)newWindow; }

        wxWindow * CStatusBar::Detach()
        {
            wxWindow * window = _statusBar;
            _statusBar = NULL;
            return window;
        }

        void CStatusBar::SetText(int index, LPCTSTR text)
        {
            _statusBar->SetStatusText(text,index);
        }

    }

}

///////////////////////// Windows/Control/ListView.cpp
#include "Windows/Control/ListView.h"

namespace NWindows {
namespace NControl {

    void CListView::Attach(wxWindow * newWindow) {
        _list = (wxListCtrl *)newWindow;
    }

    CListView::operator HWND() const { return (HWND)_list; }

    int CListView::GetItemCount() const {return  _list->GetItemCount(); }

    int CListView::InsertItem(int index, LPCTSTR text) {
        return _list->InsertItem(index, text);
    }
    int CListView::InsertItem(const LVITEM* item) {
        /*
        int col = item->iSubItem;
        wxString text;
        if (item->mask & LVIF_TEXT) text = item->pszText;

        // printf("%p->InsertItem(id=%d,%ls)\n",_list,item->iItem, (const wchar_t *)text);
        return _list->InsertItem(item->iItem, text);
        */
        wxListItem info;
        long mask = 0;
        info.SetId(item->iItem);
        if (item->mask & LVIF_TEXT)
        {
            info.SetText(item->pszText);
            mask |= wxLIST_MASK_TEXT;
        }
        if (item->mask & LVIF_PARAM)
        {
            info.SetData(item->lParam);
            mask |= wxLIST_MASK_DATA;
        }
        if (item->mask & LVIF_STATE)
        {
            info.SetState(item->state);
            mask |= wxLIST_MASK_STATE;
        }
        // FIXME if (item->mask & LVIF_IMAGE)

        info.SetMask(mask);

        return _list->InsertItem(info);
    }

    void CListView::SetItem(const LVITEM* item)  {
        int col = item->iSubItem;
        wxString text;
        if (item->mask & LVIF_TEXT) text = item->pszText;
        // printf("%p->SetItem(id=%d,col=%d,%ls)\n",_list,item->iItem, col,(const wchar_t *)text);
        _list->SetItem(item->iItem, col, text);
    }

    int CListView::SetSubItem(int index, int subIndex, LPCTSTR text)
    {
        return _list->SetItem(index, subIndex, text);
    }

    void SetUnicodeFormat(bool fUnicode) { return ;  }

    void CListView::_InsertColumn(int columnIndex, LPCTSTR text, int format, int width)
    {
        _list->InsertColumn(columnIndex, text, format, width);
    }

    void CListView::InsertColumn(int columnIndex, LPCTSTR text, int width)
    {
          this->_InsertColumn(columnIndex, text, wxLIST_FORMAT_LEFT, width);
    }

    void CListView::InsertColumn(int columnIndex, const LVCOLUMNW *columnInfo)
    {
          wxString text;
          int format = wxLIST_FORMAT_LEFT;
          int width = -1;
          if (columnInfo->mask & LVCF_FMT)
          {
              if (columnInfo->fmt == LVCFMT_LEFT) format = wxLIST_FORMAT_LEFT;
              if (columnInfo->fmt == LVCFMT_RIGHT) format = wxLIST_FORMAT_RIGHT;
          }
          if (columnInfo->mask & LVCF_TEXT)  text = columnInfo->pszText;
          if (columnInfo->mask & LVCF_WIDTH) width   = columnInfo->cx;
          // FIXME LVCF_SUBITEM
        // printf("%p->InsertColumn(%d,%ls)\n",_list,columnIndex,(const wchar_t *)heading);
          // _list->InsertColumn(columnIndex, text, format, width);
          this->_InsertColumn(columnIndex, text, format, width);
      }

      void CListView::DeleteAllItems() {
          _list->DeleteAllItems();
          printf("%p->DeleteAllItems()\n",_list);
      }

    void CListView::SetRedraw(bool b) {
        if (b) _list->Thaw();
        else   _list->Freeze();
        printf(" %p->SetRedraw()\n",_list);
    }

    void CListView::SetItemCount(int count) {
        // ONLY IF VIRTUAL REPORT -- _list->SetItemCount(count);
        printf(" %p->SetItemCount(%d)\n",_list,count);
    }

      void CListView::InvalidateRect(void *, bool)  {
          printf("FIXME %p->InvalidateRect()\n",_list);/* FIXME */
      }

      int CListView::GetSelectedCount() const {
          int nb = _list->GetSelectedItemCount();
          printf(" %p->GetSelectedCount()=>%d\n",_list,nb);
          return nb;
      }

    void /* bool */ CListView::EnsureVisible(int index, bool partialOK) {

        printf(" %p->EnsureVisible(%d)\n",_list,index);

        if (index == -1) index = 0;
        _list->EnsureVisible(index);

        // return true;
    }

    void CListView::SetItemState(int index, UINT state, UINT mask) {
        // don't work  _list->SetItemState(index, state, mask); !?
        // try SetItem ...
        /*
        wxListItem info;

        info.m_mask   = wxLIST_MASK_STATE;
        info.m_itemId = index;
        info.m_col    = 0;
        info.m_state  = state;
        info.m_mask   = mask;

        _list->SetItem(info);
        */

        printf(" %p->SetItemState(%d,0x%x,0x%x)\n",_list,index,state,mask);

        if (index == -1) return;

        _list->SetItemState(index, state & (LVIS_SELECTED|LVIS_SELECTED), mask & (LVIS_SELECTED|LVIS_SELECTED));

/*
        if (mask & LVIS_FOCUSED) {
            printf(" %p->SetItemState(%d) => FOCUSED\n",_list,index);
            _list->SetItemState(index, state & LVIS_FOCUSED, mask & LVIS_FOCUSED);
        }

        if (mask & LVIS_SELECTED) {
            printf(" %p->SetItemState(%d) => SELECTED\n",_list,index);
            _list->SetItemState(index, state & LVIS_SELECTED, mask & LVIS_SELECTED);
        }
*/
      }

      UINT CListView::GetItemState(int index, UINT mask) const
      {
        UINT state = _list->GetItemState(index, mask);
        printf("FIXME %p->GetItemState(index=%d,mask=0x%x)=0x%x\n",_list,index,(unsigned)mask,(unsigned)state); /* FIXME */

        return state;
      }

      void /* bool */  CListView::Update() {
          printf("FIXME %p->Update()\n",_list); /* FIXME */
      }

      bool CListView::DeleteColumn(int columnIndex) {
          // printf("%p->DeleteColumn()\n",_list);
          if (_list->GetColumnCount() < 1) return false;
          return _list->DeleteColumn(columnIndex); // always return true !?
      }

      bool CListView::GetItemParam(int itemIndex, LPARAM &param) const
      {
        param = _list->GetItemData(itemIndex);

        // printf(" %p->GetItemParam(%d) => %ld\n",_list,itemIndex,(long)param);

        return true;
      }

      int CListView::GetNextItem(int startIndex, UINT flags) const
          {
        int item = _list->GetNextItem(startIndex, wxLIST_NEXT_ALL, flags);
        printf(" %p->GetNextItem(%d) => %d\n",_list,startIndex,item);
        return item;

      }

    int CListView::GetFocusedItem() const
    {
        int item = _list->GetNextItem(-1, wxLIST_NEXT_ALL, wxLIST_STATE_FOCUSED);
        printf(" %p->GetFocusedItem() => %d\n",_list,item);
        return item;
    }

      void CListView::RedrawAllItems()
      {
        printf("FIXME %p->RedrawAllItems()\n",_list);
      }

      // FIXME added
      int CListView::GetColumnCount()
      {
        return _list->GetColumnCount();
      }

      void CListView::SetFocus() { /* FIXME */ }

      void CListView::RedrawItem(int item) { /* FIXME */ }

    bool CListView::SortItems(PFNLVCOMPARE compareFunction, LPARAM dataParam) {
        printf(" %p->SortItems()\n",_list);
        return _list->SortItems(compareFunction, dataParam);
    }

    bool CListView::GetColumn(int columnIndex, LVCOLUMN* columnInfo)
    {
        columnInfo->cx = _list->GetColumnWidth(columnIndex);// FIXME

        bool ret = false;

        if (columnInfo->cx >= 1) ret = true;

        // printf("CListView::GetColumn(%d) cx=%d\n",columnIndex,(int)columnInfo->cx);

        return ret;
    }

    // HWND EditLabel(int itemIndex)
    void CListView::EditLabel(int itemIndex)
    {
        /* FIXME */
    }


    LRESULT CListView2::OnMessage(UINT message, WPARAM wParam, LPARAM lParam)
    {
        return 0; // FIXME return CallWindowProc(_origWindowProc, *this, message, wParam, lParam);
    }


}}


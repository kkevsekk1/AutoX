import { showConfirmDialog } from 'dialogs'
import { showToast } from 'toast'

const cilik = await showConfirmDialog("提示", {
    content: "对话框内容"
})

showToast('你点击了：' + (cilik ? '确认' : '取消'))
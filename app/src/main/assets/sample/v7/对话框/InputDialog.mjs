import { showInputDialog } from 'dialogs'
import { showToast } from 'toast'

const input = await showInputDialog("请输入一些内容")

showToast('你输入了：' + input)
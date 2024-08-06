import { showSingleChoiceDialog } from 'dialogs'
import { showToast } from 'toast'


const select = await showSingleChoiceDialog("标题", ['item1', 'item2', 'item3'])

showToast('你选择了：' + select)
import { showAlertDialog } from 'dialogs'

await showAlertDialog("AlertDialog", {
    content: "对话框内容"
})


await showAlertDialog("提示", {
    content: "不可通过点击返回键和点击外部取消的对话框",
    dismissOnBackPress: false,
    dismissOnClickOutside: false,
})
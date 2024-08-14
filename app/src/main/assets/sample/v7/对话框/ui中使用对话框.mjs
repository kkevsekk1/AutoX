import { showAlertDialog, DialogFactory } from 'dialogs'
import {
    createApp, xml, startActivity, Icons, defineComponent, ModifierExtension
} from "vue-ui";
import { showToast } from 'toast'

/**
 * 如果有ui界面，建议使用以下方法创建对话框，拥有更好的交互效果
 */
const factory = new DialogFactory()

function alert() {
    factory.showAlertDialog('提示', {
        content: '这是ui中的对话框'
    })
}

async function confirm() {
    const b = await factory.showConfirmDialog('提示', {
        content: '这是一个确认框'
    })
    showToast(b)
}
const app = createApp({
    render() {
        return xml`
    <column>
        <Button onClick=${alert}>弹出提示</Button>
        <Button onClick=${confirm}>弹出确认框</Button>
        <${factory.Dialog/**对话框的挂载点 */} />
    </column>
    `
    }
})

startActivity(app)
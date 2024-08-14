import { Color } from "@/vue-ui/theme"
import { DialogType, DialogInterface } from "."
import { Component, defineComponent, h, VNode, xml, PropType, shallowReactive } from "@/vue-ui"
import { fillMaxWidth, heightIn, padding, verticalScroll, widthIn } from "@/vue-ui/modifierExtension"

export type SecureTpye = TSecureTpye
/**
 * 创建对话框的一些通用选项
 */
export interface DialogBuilderOptions {
    /**对话框标题 */
    title?: string
    /**对话框内容，可以是一个vue组件对象 */
    content?: string | Component
    /**仅在content是字符串时有效 */
    contentColor?: Color
    /**尚未支持 */
    icon?: string
    /**positive的文本，不设置将不显示positive按钮 */
    positive?: string
    positiveColor?: Color
    /**negative的文本，不设置将不显示negative按钮 */
    negative?: string
    negativeColor?: Color
    neutralColor?: Color
    /**neutral的文本，不设置将不显示neutral按钮 */
    neutral?: string
    /**是否允许返回键取消对话框，默认为true */
    dismissOnBackPress?: boolean
    /**是否允许点击对话框外部取消对话框，默认为true */
    dismissOnClickOutside?: boolean
    /**设置对话框窗口的安全策略 */
    securePolicy?: SecureTpye
    type?: DialogType
}
export interface DialogEventListener {
    onPositive?: () => void
    onNegative?: () => void
    onNeutral?: () => void
}
export interface InputDialogOptions extends DialogBuilderOptions {
    /**输入框的提示 */
    inputHint?: string,
    /**输入框的默认文本 */
    inputPrefill?: string,
    /**输入框的lable */
    inputLable?: string
}

function dialogButton(text?: string, color?: Color, onClick?: () => void) {
    if (!text) return
    return xml`
        <Button onClick=${onClick} type="text">
            <text color=${color}>${text}</text>
        </Button>
    `
}

export function createDialogContent(options: DialogBuilderOptions) {
    const { title, content, contentColor, positive, positiveColor, icon,
        negative, negativeColor, neutralColor, neutral,
    } = options
    return defineComponent({
        props: {
            events: Object as PropType<DialogEventListener>
        },
        render() {
            let contentVnode: any;
            if (typeof content === 'string') {
                contentVnode = xml`<text color=${contentColor}>${content}</text>`
            } else if (content) {
                contentVnode = xml`<${content}/>`
            }
            const positiveVnode = dialogButton(positive, positiveColor, this.$props.events?.onPositive)
            const negativeVnode = dialogButton(negative, negativeColor, this.$props.events?.onNegative)
            const neutralVnode = dialogButton(neutral, neutralColor, this.$props.events?.onNeutral)

            return xml`
            <card>
                <column modifier=${[padding(16, 16, 16, 8)]}>
                    <row modifier=${[padding(0, 0, 0, 10)]}>
                        <text fontSize=${18}>${title}</text>
                    </row>
                    <column modifier=${[heightIn(35, 500), verticalScroll(), widthIn(140)]}>
                        ${contentVnode}
                    </column>
                    <row modifier=${[fillMaxWidth(), padding(0, 5, 0, 0)]}>
                        ${neutralVnode}
                        <row modifier=${[fillMaxWidth()]} horizontalArrangement="end">
                            ${negativeVnode}
                            ${positiveVnode}
                        </row>
                    </row>
                </column>
               </card>
            `
        }
    })
}

export interface IDialogs {
    showDialog(options: DialogBuilderOptions): DialogInterface
    showAlertDialog(title: string, options?: DialogBuilderOptions): Promise<void>
    showConfirmDialog(title: string, options?: DialogBuilderOptions): Promise<boolean>
    showInputDialog(title: string, prefill?: string, options?: InputDialogOptions): Promise<string | null>
    showSelectDialog(title: string, items: string[], options?: DialogBuilderOptions): Promise<number>
    showMultiChoiceDialog(title: string,
        items: string[],
        initialSelectedIndices?: number[],
        options?: DialogBuilderOptions): Promise<number[] | null>
    showSingleChoiceDialog(title: string,
        items: string[],
        initialSelectedIndex?: number,
        options?: DialogBuilderOptions): Promise<number>

}

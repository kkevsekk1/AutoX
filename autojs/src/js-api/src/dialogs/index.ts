
import { EventEmitter } from 'node:events'
import { xml, Component, ModifierExtension, defineComponent, createApp, ref, reactive } from '@/vue-ui'
import { nodeOps } from '@/vue-ui/nodeOps'
import { createDialogContent, DialogBuilderOptions, DialogEventListener, InputDialogOptions } from './options'
import { padding, fillMaxWidth, height, heightIn, clickable } from '@/vue-ui/modifierExtension'
import DialogFactory from './DialogFactory'

const dialogs = Autox.dialogs

export type DialogType = 'app' | 'overlay' | DialogFactory
export const defaultDialogType: DialogType = 'app'


export function showAppDialog(comp: Component, ops?: DialogOps) {
    const el = nodeOps.createElement('box')
    const app = createApp(comp)
    app.mount(el)
    const s = setInterval(() => { }, 2000)
    return dialogs.showDialog(el.__xel, Object.assign({
        dismissOnBackPress: true
    }, ops, {
        onDismiss() {
            app.unmount()
            clearInterval(s)
            ops?.onDismiss()
        }
    }))
}

export enum DialogEvent {
    /**@event */
    ON_DISMISS = 'dismiss',
    /**@event */
    ON_POSITIVE = 'positive',
    /**@event */
    ON_NEGATIVE = 'negative',
    /**@event */
    ON_NEUTRAL = 'neutral',
    /**@event */
    ON_INPUT_CHANGE = 'input_change',
}
export interface DialogInterface extends EventEmitter<Record<DialogEvent, any[]>> {
    dismiss(): void
}
class Dialog extends EventEmitter<Record<DialogEvent, any[]>>
    implements DialogInterface {
    _nv?: AppDialogBuilder
    destroyed: boolean = false
    constructor() {
        super()
        this.once(DialogEvent.ON_DISMISS, () => { this.destroyed = true; })
    }
    dismiss() {
        this._nv?.dismiss()
    }
}
export function showDialog(options: DialogBuilderOptions): DialogInterface {
    const { type = defaultDialogType, dismissOnBackPress, dismissOnClickOutside } = options
    const Content = createDialogContent(options)
    let dialog: DialogInterface
    const dialogEventListener: DialogEventListener = {
        onPositive() {
            dialog.emit(DialogEvent.ON_POSITIVE, dialog)
        },
        onNegative() {
            dialog.emit(DialogEvent.ON_NEGATIVE, dialog)
        },
        onNeutral() {
            dialog.emit(DialogEvent.ON_NEUTRAL, dialog)
        },
    }
    const comp = () => {
        return xml`
                <${Content} events=${dialogEventListener}/>
            `
    }
    const ops: DialogOps = {
        dismissOnBackPress: (typeof dismissOnBackPress === 'boolean') ? dismissOnBackPress : true,
        dismissOnClickOutside: (typeof dismissOnClickOutside === 'boolean') ? dismissOnClickOutside : true,
        onDismiss() {
            dialog.emit(DialogEvent.ON_DISMISS)
        },
    }

    if (type === 'app') {
        dialog = new Dialog();
        (dialog as Dialog)._nv = showAppDialog(comp, ops)
    } else if (type instanceof DialogFactory) {
        dialog = type._mountUi(comp, ops)
    } else {
        dialog = new Dialog();
        console.warn('Unknown Dialog type: ' + type);
    }
    return dialog
}
/**
 * 显示一个消息提示对话框，返回一个Promise
 * @param title 
 * @param options 
 * @returns Promise将在对话框消失时完成
 */
export async function showAlertDialog(title: string, options?: DialogBuilderOptions) {
    const f: DialogBuilderOptions = {
        title: title,
        positive: '确认',
    }
    const dialog = showDialog(Object.assign(f, options))

    return new Promise<void>((resolve, reject) => {
        dialog.once(DialogEvent.ON_DISMISS, resolve)
        dialog.once(DialogEvent.ON_POSITIVE, () => { dialog.dismiss() })
    })
}
/**
 * 显示一个确认对话框
 * @param title 
 * @param options 
 * @returns 只在点击positive按钮时返回true,其他情况返回false
 */
export async function showConfirmDialog(title: string, options?: DialogBuilderOptions) {
    const f: DialogBuilderOptions = {
        title: title,
        positive: '确认',
        negative: '取消',
    }
    const dialog = showDialog(Object.assign(f, options))
    let r = false
    return new Promise<boolean>((resolve, reject) => {
        dialog.once(DialogEvent.ON_DISMISS, () => resolve(r))
        dialog.once(DialogEvent.ON_POSITIVE, () => {
            r = true
            dialog.dismiss()
        })
        dialog.once(DialogEvent.ON_NEGATIVE, () => { dialog.dismiss() })
    })
}
/**
 * 显示一个输入框，提示用户输入信息
 * @param title 
 * @param prefill 输入框的默认内容
 * @param options 
 * @returns 点击positive时返回字符串，即使输入为空，被取消时返回null
 */
export async function showInputDialog(title: string, prefill?: string, options?: InputDialogOptions) {
    let input = prefill || ""
    const DialogContent = defineComponent(function () {
        function updateInput(value: string) {
            input = value
            dialog.emit(DialogEvent.ON_INPUT_CHANGE, value, dialog)
        }
        return function render() {
            return xml`
            <TextField value=${input} 
                placeholder=${options?.inputHint}
                label=${options?.inputLable}
                onValueChange=${updateInput} />
            `
        }
    })

    const f: InputDialogOptions = {
        title: title,
        inputPrefill: prefill,
        positive: '确认',
        negative: '取消',
    }
    const dialog = showDialog(Object.assign(f, options, { content: DialogContent, }))

    return new Promise<string | null>((resolve, reject) => {
        dialog.once(DialogEvent.ON_DISMISS, () => resolve(null))
        dialog.once(DialogEvent.ON_POSITIVE, () => {
            resolve(input)
            dialog.dismiss()
        })
        dialog.once(DialogEvent.ON_NEGATIVE, () => { dialog.dismiss() })
    })
}
/**
 * 显示一个选择对话框，选中任意项后消失
 * @param title 
 * @param items 选项数组
 * @param options 
 * @returns 返回选中的项目索引，被取消则返回-1
 */
export async function showSelectDialog(title: string, items: string[], options?: DialogBuilderOptions) {
    let select = -1
    const DialogContent = defineComponent(function () {
        function click(i: number) {
            select = i
            dialog.dismiss()
        }
        const modifier = [fillMaxWidth(), heightIn(50)]
        return function render() {
            return items.map((item, i) => {
                const onClick = click.bind(undefined, i)
                return xml`
                <box contentAlignment="center_start"
                    modifier=${[...modifier, clickable(onClick)]}>
                    <text fontSize=${15}>${item}</text>
                </box>
                `
            })
        }
    })

    const f: InputDialogOptions = {
        title: title
    }
    const dialog = showDialog(Object.assign(f, options, { content: DialogContent, }))

    return new Promise<number>((resolve, reject) => {
        dialog.once(DialogEvent.ON_DISMISS, () => resolve(select))
    })
}

/**
 * 显示一个多选对话框
 * @param title 
 * @param items 可多选的项目
 * @param initialSelectedIndices 初始选中的项目索引数组
 * @param options 
 * @returns 返回选中的项目索引数组，被取消则返回`null`
 */
export async function showMultiChoiceDialog(title: string,
    items: string[],
    initialSelectedIndices?: number[],
    options?: DialogBuilderOptions) {
    let select = new Set<number>()

    const DialogContent = defineComponent(function () {
        const state = reactive(items.map(() => false))
        if (initialSelectedIndices) {
            for (let i of initialSelectedIndices) {
                if (i >= items.length) continue;
                select.add(i)
                state[i] = true;
            }
        }
        function click(i: number) {
            const r = state[i] = !state[i]
            if (r) {
                select.add(i)
            } else select.delete(i);
        }
        const modifier = [fillMaxWidth(), heightIn(50)]
        return function render() {
            return items.map((item, i) => {
                const onCheckedChange = click.bind(undefined, i)
                return xml`
                <row verticalAlignment="center"
                    modifier=${[...modifier, clickable(onCheckedChange)]}>
                    <Checkbox checked=${state[i]} 
                        onCheckedChange=${onCheckedChange} />
                    <text fontSize=${15}>${item}</text>
                </row>
                `
            })
        }
    })

    const f: InputDialogOptions = {
        title: title,
        positive: '确认',
        negative: '取消',
    }
    const dialog = showDialog(Object.assign(f, options, { content: DialogContent, }))

    return new Promise<number[] | null>((resolve, reject) => {
        dialog.once(DialogEvent.ON_DISMISS, () => resolve(null))
        dialog.once(DialogEvent.ON_POSITIVE, () => {
            resolve(Array.from(select))
            dialog.dismiss()
        })
        dialog.once(DialogEvent.ON_NEGATIVE, () => { dialog.dismiss() })
    })
}
/**
 * 显示一个单选对话框
 * @param title 
 * @param items 
 * @param initialSelectedIndex 
 * @param options 
 * @returns 返回选中的项目索引，被取消则返回-1
 */
export async function showSingleChoiceDialog(title: string,
    items: string[],
    initialSelectedIndex?: number,
    options?: DialogBuilderOptions) {
    let select = ref(initialSelectedIndex || 0)

    const DialogContent = defineComponent(function () {
        function click(i: number) {
            select.value = i
        }
        const modifier = [fillMaxWidth(), heightIn(50)]
        return function render() {
            return items.map((item, i) => {
                const onCheckedChange = click.bind(undefined, i)
                return xml`
                <row verticalAlignment="center"
                    modifier=${[...modifier, clickable(onCheckedChange)]}>
                    <RadioButton selected=${select.value == i}
                         onClick=${onCheckedChange} />
                    <text fontSize=${15}>${item}</text>
                </row>
                `
            })
        }
    })

    const f: InputDialogOptions = {
        title: title,
        positive: '确认',
        negative: '取消',
    }
    const dialog = showDialog(Object.assign(f, options, { content: DialogContent, }))

    return new Promise<number>((resolve, reject) => {
        dialog.once(DialogEvent.ON_DISMISS, () => resolve(-1))
        dialog.once(DialogEvent.ON_POSITIVE, () => {
            resolve(select.value)
            dialog.dismiss()
        })
        dialog.once(DialogEvent.ON_NEGATIVE, () => { dialog.dismiss() })
    })
}

export { DialogFactory }
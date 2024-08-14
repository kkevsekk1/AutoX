import { xml } from "@/vue-ui"
import { shallowReactive, Component, defineComponent, reactive, watch } from "@vue/runtime-core"
import { DialogEvent, DialogInterface } from "."
import * as main from '.'
import { DialogBuilderOptions, IDialogs, InputDialogOptions } from "./options"
import { EventEmitter } from 'node:events'

type DialogStatus = DialogOps & {
    isShow: boolean
}

class MutxDialog extends EventEmitter<Record<DialogEvent, any[]>> implements DialogInterface {
    state: DialogStatus
    constructor(state: DialogStatus) {
        super()
        this.state = state
    }
    dismiss(): void {
        this.state.isShow = false
    }
}

export default class DialogFactory implements IDialogs {
    showin = shallowReactive(new Set<[Component, DialogStatus]>())

    get Dialog(): Component {
        const showin = this.showin
        return defineComponent({
            render() {
                const nodes = Array.from(showin).map((t) => {
                    const [comp, ops] = t
                    if (!ops.isShow) {
                        showin.delete(t)
                        return
                    }
                    return xml`
                    <Dialog
                        dismissOnBackPress=${ops.dismissOnBackPress}
                        dismissOnClickOutside=${ops.dismissOnClickOutside}
                        securePolicy=${ops.securePolicy}
                        onDismissRequest=${() => { ops.isShow = false }}>
                        <${comp} />
                    </Dialog>
                    `
                })
                return xml`
                ${nodes}
                `
            }
        })
    }

    _mountUi(comp: Component, ops: DialogOps): DialogInterface {
        const state: DialogStatus = reactive({
            ...ops,
            isShow: true
        })
        const p = watch(() => state.isShow, (isShow) => {
            if (isShow === false) {
                ops.onDismiss();
                p()
            }
        })
        this.showin.add([comp, state])
        return new MutxDialog(state)
    }
    showDialog(options: DialogBuilderOptions): DialogInterface {
        options.type = this
        return main.showDialog({
            ...options,
            type: this,
        })
    }
    showAlertDialog(title: string, options: DialogBuilderOptions): Promise<void> {
        return main.showAlertDialog(title, {
            ...options,
            type: this,
        })
    }
    showConfirmDialog(title: string, options: DialogBuilderOptions): Promise<boolean> {
        return main.showConfirmDialog(title, {
            ...options,
            type: this,
        })
    }
    showInputDialog(title: string, prefill?: string, options?: InputDialogOptions): Promise<string | null> {
        return main.showInputDialog(title, prefill, {
            ...options,
            type: this,
        })
    }
    showSelectDialog(title: string, items: string[], options?: DialogBuilderOptions): Promise<number> {
        return main.showSelectDialog(title, items, {
            ...options,
            type: this,
        })
    }
    showMultiChoiceDialog(title: string, items: string[], initialSelectedIndices?: number[], options?: DialogBuilderOptions): Promise<number[] | null> {
        return main.showMultiChoiceDialog(title, items, initialSelectedIndices, {
            ...options,
            type: this,
        })
    }
    showSingleChoiceDialog(title: string, items: string[], initialSelectedIndex?: number, options?: DialogBuilderOptions): Promise<number> {
        return main.showSingleChoiceDialog(title, items, initialSelectedIndex, {
            ...options,
            type: this,
        })
    }

}
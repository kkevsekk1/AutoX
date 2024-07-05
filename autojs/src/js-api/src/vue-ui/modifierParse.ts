/**
 * 
 * @description
 */

import { PxElement } from "./types"

const ui = Autox.ui
/**
 * 创建Modifier的选项
 * 
 */
export interface ModifierOptions {
    /**设置组件宽度，单位为DP */
    width?: number
    /**设置组件高度，单位为DP */
    height?: number
    /**组件的点击事件 */
    onClick?: () => void

}

const resModifierOptions: {
    [Property in keyof ModifierOptions]-?:
    (builder: ModifierBuilder, value: ModifierOptions[Property]) => void
} = {
    width: function (builder: ModifierBuilder, value: number | undefined): void {
        if (value) {
            builder.width(value)
        }
    },
    height: function (builder: ModifierBuilder, value: number | undefined): void {
        if (value) {
            builder.height(value)
        }
    },
    onClick: function (builder: ModifierBuilder, value: (() => void) | undefined): void {
        if (value) {
            builder.click(value)
        }
    },

}

const modifierCache = new Map<any, Modifier>()

export function parseModifier(
    modifierOps: ModifierOptions | ModifierOptions[],
    el: PxElement
) {
    if (modifierCache.has(modifierOps)) {
        el.__xel.modifier = modifierCache.get(modifierOps)!
    }
    let modifierOpsList: ModifierOptions[]

    if (modifierOps instanceof Array) {
        modifierOpsList = modifierOps
    } else {
        modifierOpsList = [modifierOps]
    }
    const builder = ui.createModifierBuilder(null)

    modifierOpsList.forEach((ops) => {
        for (const [key, value] of Object.entries(ops)) {
            resModifierOptions[key as keyof ModifierOptions]?.(builder, value)
        }
    })

    modifierCache.set(modifierOps, builder.modifier)
    el.__xel.modifier = builder.modifier
}

function createModifier() {

}
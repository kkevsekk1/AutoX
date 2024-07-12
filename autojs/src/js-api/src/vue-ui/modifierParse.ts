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
    fillMaxSize?: boolean
    fillMaxWidth?: boolean
    fillMaxHeight?: boolean
    background?: number | string
    padding?: number | number[]
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
    fillMaxSize: function (builder: ModifierBuilder, value?: boolean): void {
        if (value) {
            builder.fillMaxSize()
        }
    },
    fillMaxWidth: function (builder: ModifierBuilder, value?: boolean): void {
        if (value) {
            builder.fillMaxWidth()
        }
    },
    fillMaxHeight: function (builder: ModifierBuilder, value?: boolean): void {
        if (value) {
            builder.fillMaxHeight()
        }
    },
    padding: function (builder: ModifierBuilder, value: number | number[] | undefined): void {
        if (!value) return
        if (typeof value === 'number') {
            builder.padding(value)
        } else {
            if (value.length >= 4) {
                builder.padding(value[0], value[1], value[2], value[3])
            } else if (value.length >= 2) {
                builder.padding(value[0], value[1])
            }
        }
    },
    background: function (builder: ModifierBuilder, value: string | number | undefined): void {
        if (!value) return
        builder.background(value)
    }
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
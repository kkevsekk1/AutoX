/**
 * 
 * @description
 */

import { PxElement } from "./types"

const ui = Autox.ui

export function parseModifier(
    modifierExt: ModifierExt[],
    el: PxElement
) {
    el.__xel.clearModifierExts()
    modifierExt.forEach((ext) => {
        el.__xel.addModifierExt(ext)
    })
}



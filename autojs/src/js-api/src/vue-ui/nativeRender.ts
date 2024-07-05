import { PxText } from './nodeOps'
import { FunctionComponent, PxElement } from './types'

const ui = Autox.ui

function createText(text: string) {
    return ui.createComposeText(text)
}


function createElement(
    tag: string,
    props?: Record<string, any>,
    ...children: any[]): ComposeElement {
    props = props || {}
    if (typeof tag === 'function') {
        return (tag as FunctionComponent)(props, ...children)
    }

    if (tag === "text") {
        const text: string =
            children.find(val => typeof val === "string") || props.text || ""
        return createText(text)
    }

    return ui.createComposeElement(tag, props, children.map((node) => {
        if (typeof node === "string") {
            return createText(node)
        } else return node
    }))
}
export function setText(el: PxText, text: string) {
    el.__xel.text = text
    ui.updateComposeElement(el.__xel)
}
function removeElement(element: PxElement) {
    const parent = element.__xel.parentNode
    if (parent) {
        parent.removeChild(element.__xel)
        ui.updateComposeElement(parent)
    }
}
function insertElement(child: PxElement, parent: PxElement, ref?: PxElement) {
    if (child.tag === "template") {
        let name: any;
        for (const [key, value] of Object.entries(child.props)) {
            if (key === "v-slot") {
                name = value
                break
            }
            if (key.startsWith('#')) {
                name = key.substring(1)
                break
            }
        }
        if (typeof name === "string") {
            parent.__xel.addTemplate(name, child.__xel)
        } else {
            parent.__xel.insertChild(child.__xel, ref?.__xel || null)
        }
    } else {
        parent.__xel.insertChild(child.__xel, ref?.__xel || null)
    }
    ui.updateComposeElement(parent.__xel)
}
function patchElementProp(el: PxElement, key: string, prevValue: any,
    nextValue: any,) {
    ui.patchProp(el.__xel, key, nextValue)
    ui.updateComposeElement(el.__xel)
}

export { createElement, createText, removeElement, insertElement, patchElementProp }
import { markRaw } from '@vue/reactivity'
import { RendererOptions } from '@vue/runtime-core'
import { PxNode, PxNodeTypes } from './types'
import { createElement, insertElement, removeElement, setText } from './nativeRender';

export type DomRendererOptions = RendererOptions<PxNode, PxElement>
let nodeId: number = 0

export enum NodeOpTypes {
    CREATE = 'create',
    INSERT = 'insert',
    REMOVE = 'remove',
    SET_TEXT = 'setText',
    SET_ELEMENT_TEXT = 'setElementText',
    PATCH = 'patch',
}
export class PxElement implements PxNode {
    id: number = nodeId++;
    type: PxNodeTypes = PxNodeTypes.ELEMENT
    parentNode: PxElement | null = null
    tag: string
    __xel: ComposeElement
    children: PxNode[] = []
    props: Record<string, any> = {}
    eventListeners: Record<string, Function | Function[]> | null = {}
    constructor(tag: string) {
        this.tag = tag
        this.__xel = createElement(tag)

        this.__xel.id = this.id
    }
    setText(text: string) {
        if (this.tag === PxNodeTypes.TEXT) {
            (this.__xel as ComposeTextNode).text = text || ""
            return
        }
        this.children.forEach(c => {
            remove(c)
        })
        this.children = []
        if (text) {
            const el = new PxText(text)
            el.parentNode = this
            this.children.push(el)
            insertElement(el, this)
        }
    }
}
export class PxText extends PxElement {
    type: PxNodeTypes = PxNodeTypes.TEXT
    text: string
    declare __xel: ComposeTextNode;
    constructor(text: string) {
        super(PxNodeTypes.TEXT)
        this.text = text;
        this.__xel.text = text
    }
}



export class PxComment implements PxNode {
    id: number = -1
    type: PxNodeTypes = PxNodeTypes.COMMENT
    parentNode: PxElement | null = null
    text: string
    constructor(text: string) {
        this.text = text
    }
}

export interface NodeOp {
    type: NodeOpTypes
    nodeType?: PxNodeTypes
    tag?: string
    text?: string
    targetNode?: PxNode
    parentNode?: PxElement
    refNode?: PxNode | null
    propKey?: string
    propPrevValue?: any
    propNextValue?: any
}

let debug: boolean = false
export function setDebug(d: boolean) {
    debug = d
}
export function logNodeOp(op: NodeOp) {
    if (debug) {
        console.log(op);
        console.warn("--------------------------------");
    }
}

export const nodeOps: Omit<DomRendererOptions, 'patchProp'> = {
    createElement(tag: string): PxElement {
        const node: PxElement = new PxElement(tag)
        logNodeOp({
            type: NodeOpTypes.CREATE,
            nodeType: PxNodeTypes.ELEMENT,
            targetNode: node,
            tag,
        })
        // avoid test nodes from being observed
        markRaw(node)
        return node
    },
    createText(text: string) {
        const node = new PxText(text)
        logNodeOp({
            type: NodeOpTypes.CREATE,
            nodeType: PxNodeTypes.TEXT,
            targetNode: node,
            text,
        })
        // avoid test nodes from being observed
        markRaw(node)
        return node
    },
    createComment(text: string) {
        const node = new PxComment(text)
        logNodeOp({
            type: NodeOpTypes.CREATE,
            nodeType: PxNodeTypes.COMMENT,
            targetNode: node,
            text,
        })
        // avoid test nodes from being observed
        markRaw(node)
        return node
    },
    setText(node: PxText, text: string) {
        logNodeOp({
            type: NodeOpTypes.SET_TEXT,
            targetNode: node,
            text,
        })
        node.text = text
        setText(node, text)
    },
    insert(child: PxNode, parent, ref) {
        let refIndex
        if (ref) {
            refIndex = parent.children.indexOf(ref)
            if (refIndex === -1) {
                console.error('ref: ', ref)
                console.error('parent: ', parent)
                throw new Error('ref is not a child of parent')
            }
        }
        logNodeOp({
            type: NodeOpTypes.INSERT,
            targetNode: child,
            parentNode: parent,
            refNode: ref,
        })
        // remove the node first, but don't log it as a REMOVE op
        remove(child)
        // re-calculate the ref index because the child's removal may have affected it
        refIndex = ref ? parent.children.indexOf(ref) : -1
        if (refIndex === -1) {
            parent.children.push(child)
            child.parentNode = parent
        } else {
            parent.children.splice(refIndex, 0, child)
            child.parentNode = parent
        }
        if (child instanceof PxElement) {
            if (ref instanceof PxElement) {
                insertElement(child, parent, ref)
            } else insertElement(child, parent)
        }
    },
    remove,
    setElementText(el, text: string) {
        logNodeOp({
            type: NodeOpTypes.SET_ELEMENT_TEXT,
            targetNode: el,
            text,
        })
        el.setText(text)
    },
    parentNode(node): PxElement | null {
        return node.parentNode
    },

    nextSibling(node) {
        const parent = node.parentNode
        if (!parent) {
            return null
        }
        const i = parent.children.indexOf(node)
        return parent.children[i + 1] || null
    }
}

function remove(child: PxNode) {
    const parent = child.parentNode
    if (parent) {
        logNodeOp({
            type: NodeOpTypes.REMOVE,
            targetNode: child,
            parentNode: parent,
        })
        const i = parent.children.indexOf(child)
        if (i > -1) {
            parent.children.splice(i, 1)
        } else {
            console.error('target: ', child)
            console.error('parent: ', parent)
            throw Error('target is not a childNode of parent')
        }
        if (child instanceof PxElement) {
            removeElement(child)
        }
        child.parentNode = null
    }
}
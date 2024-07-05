import {
    App,
    Component,
    type CreateAppFunction,
    type RootRenderFunction,
    type VNode,
    createRenderer,
    h,
} from '@vue/runtime-core'
import { nodeOps, setDebug } from './nodeOps'
import { patchProp } from './patchProp'
import { extend } from '@vue/shared'
import htm from 'htm'
import { ActivityEventListener, PxElement } from './types'

const { render: baseRender, createApp: baseCreateApp } = createRenderer(
    extend({ patchProp }, nodeOps),
)
const ui = Autox.ui

export const render = baseRender as RootRenderFunction<PxElement>
export const createApp: CreateAppFunction<PxElement> = function (
    rootComponent: Component,
    rootProps?: any | null
): App<PxElement> {
    const app = baseCreateApp(rootComponent, rootProps)
    const { mount } = app
    app.mount = (containerOrSelector: PxElement | string): any => {
        if (containerOrSelector === 'activit') {
            containerOrSelector = nodeOps.createElement('box')
        }
        const proxy = mount(containerOrSelector)
        startActivity((containerOrSelector as PxElement).__xel)
        return proxy
    }
    return app
}

// convenience for one-off render validations
export function renderActivity(vnode: VNode, listener?: ActivityEventListener) {
    const root = nodeOps.createElement('box')
    render(vnode, root)
    startActivity(root.__xel, listener)
}

function startActivity(element: ComposeElement, listener?: ActivityEventListener) {
    let emit = null
    if (listener) {
        emit = (event: string, ...args: any[]) => {
            (listener as any)[event]?.(...args)
        }
    }
    ui.startActivity(element, emit)
}

export const xml = htm.bind(h)
export { nodeOps, setDebug }
export * from '@vue/runtime-core'
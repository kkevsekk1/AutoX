/**
 * 入门
 * @document README.md
 * @packageDocumentation
 */
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
import * as ModifierExtension from './modifierExtension'

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
            const proxy = mount(containerOrSelector)
            startUi((containerOrSelector as PxElement).__xel)
            return proxy
        } else {
            return mount(containerOrSelector)
        }
    }
    return app
}

// convenience for one-off render validations
export function renderActivity(vnode: VNode, listener?: ActivityEventListener) {
    const root = nodeOps.createElement('box')
    render(vnode, root)
    startUi(root.__xel, listener)
}

function startUi(element: ComposeElement, listener?: ActivityEventListener) {
    let emit = null
    if (listener) {
        emit = (event: string, ...args: any[]) => {
            (listener as any)[event]?.(...args)
        }
    }
    return ui.startActivity(element, emit)
}
/**
 * 启动Activity并挂载app实例作为内容
 * @param app Vue的app实例
 * @param listener 用于监听该Activity各种事件的监听器
 * @returns 当Activity创建完成后返回该Activity实例
 */
export function startActivity(app: App<PxElement>, listener?: ActivityEventListener) {
    const root = nodeOps.createElement('box')
    app.mount(root)
    let n = Object.create(listener || {})
    n.onDestroy = function (...args: any[]) {
        app.unmount()
        listener?.onDestroy?.(...args)
    }
    return startUi(root.__xel, n)
}
/**
 * 该函数用于创建VNode节点，详细参考[htm](https://github.com/developit/htm)
 */
export const xml = htm.bind(h)
export { setDebug }
export * from '@vue/runtime-core'
export * as Icons from './icons'
/**
 * 这个对象导出用于设置`Modifier`的函数
 */
export { ModifierExtension }
export { ActivityEventListener }
export * as Theme from './theme'
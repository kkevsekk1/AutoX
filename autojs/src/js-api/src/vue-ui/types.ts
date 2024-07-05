import { PxElement } from "./nodeOps"

export interface ActivityEventListener {
    onCreate(...args: any[]): any
    onStart(...args: any[]): any
    onResume(...args: any[]): any
    onPause(...args: any[]): any
    onStop(...args: any[]): any
    onDestroy(...args: any[]): any
    onActivityResult(...args: any[]): any
    onBackPressed(...args: any[]): any
    onNewIntent(...args: any[]): any
    onRecreate(...args: any[]): any
    onSaveInstanceState(...args: any[]): any
    onConfigurationChanged(...args: any[]): any
}

export type FunctionComponent = (
    props?: Record<string, any>,
    ...children: any[]) => ComposeElement


export enum PxNodeTypes {
    TEXT = 'text',
    ELEMENT = 'element',
    COMMENT = 'comment',
}

export interface PxNode {
    id: number
    type: PxNodeTypes
    parentNode: PxElement | null
}
export { PxElement }


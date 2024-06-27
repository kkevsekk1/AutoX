
type Context = {}
type JavaClass = {}
type PromiseCallback = {
    onResolve: (result) => void
    onReject: (error) => void
}
declare namespace root.java {
    function loadClass(name: string): JavaClass
    async function invokeUi<T>(
        javaobj: any,
        methodName: string,
        args: any[]): Promise<T>
    async function invokeDefault<T>(
        javaobj: any,
        methodName: string,
        args: any[]): Promise<T>
    async function invokeIo<T>(
        javaobj: any,
        methodName: string,
        args: any[]): Promise<T>
}
declare namespace root.toast {
    declare const SHORT: number
    declare const LONG: number
    declare function showToast(msg: string, duration?: number): void
}
declare namespace root.clipManager {
    declare function getClip(): string
    declare function setClip(text: String)
    declare function hasClip(): boolean
    declare function clearClip(): void
    declare function registerListener(
        onPrimaryClipChangedListener: { onClipChanged: () => void }
    ): void
}
declare namespace root.media {
    function scanFile(path: string)
    function createMediaPlayer(): NativeMediaPlayer
}
declare namespace root {
    function exit(): void
    const context: Context
}
declare const Autox = root

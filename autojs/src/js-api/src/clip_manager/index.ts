import { EventEmitter } from 'events'

const clipManager = Autox.clipManager

export const clipboardManager = new EventEmitter()
clipManager.registerListener({
    onClipChanged: () => {
        clipboardManager.emit('clip_changed')
    }
})
export function setClip(text: string) {
    clipManager.setClip(text);
}

export function getClip(): string {
    return clipManager.getClip();
}

export function hasClip(): boolean {
    return clipManager.hasClip();
}

export function clearClip(): void {
    clipManager.clearClip();
}
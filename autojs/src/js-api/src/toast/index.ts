import { format } from 'util'
interface ToastOptions {
    duration?: "short" | "long"
}

/**
 * 弹出一条toast
 * @example
 * import { showToast } from 'toast'
 * showToast('hello world')
 * @param message 要显示的消息
 * @param option 可以是"short" | "long"，表示弹出时长
 */
export function showToast(message: any, option?: ToastOptions | string) {
    let duration: number
    if (typeof option === "string") {
        if (option !== "short") {
            duration = Autox.toast.SHORT
        } else duration = Autox.toast.LONG
    } else if (option?.duration === 'long') {
        duration = Autox.toast.LONG
    } else {
        duration = Autox.toast.SHORT
    }
    Autox.toast.showToast(format(message), duration)
}
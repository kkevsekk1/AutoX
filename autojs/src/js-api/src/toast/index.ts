import { format } from 'util'
interface ToastOptions {
    duration?: "short" | "long"
}


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
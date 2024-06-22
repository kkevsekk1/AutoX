
interface ToastOptions {
    duration?: "short" | "long"
}


export function showToast(message: string, option?: ToastOptions | string) {
    Autox.toast.showToast(message)
}
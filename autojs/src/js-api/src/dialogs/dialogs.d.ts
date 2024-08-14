declare type TSecureTpye = 'SecureOn' | 'SecureOff' | 'Inherit'

declare type DialogOps = {
    dismissOnBackPress: boolean
    dismissOnClickOutside: boolean
    securePolicy?: TSecureTpye
    onDismiss: () => void
}
declare type AppDialogBuilder = {
    dismiss(): void
}
declare namespace root.dialogs {
    function showDialog(element: ComposeElement, ops: DialogOps?): AppDialogBuilder
}
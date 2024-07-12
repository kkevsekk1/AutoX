
interface Icons {
    Call: ImageVector
    Add: ImageVector
    ArrowBack: ImageVector
    Clear: ImageVector
    Edit: ImageVector
    Menu: ImageVector
    Search: ImageVector
    Close: ImageVector
    Star: ImageVector
    Home: ImageVector
    Notifications: ImageVector
    Settings: ImageVector
    MoreVert: ImageVector
    MailOutline: ImageVector
    Refresh: ImageVector
    AccountBox: ImageVector
}

export type IconExprot = {
    [Property in keyof Icons]:
    () => Icons[Property]
}
const ui = Autox.ui
function createLoadFn(group: string, name: string) {
    return () => ui.loadIcon(group, name)
}

let group = "Filled"
const Filled: IconExprot = {
    Call: createLoadFn(group, "Call"),
    Add: createLoadFn(group, "Add"),
    ArrowBack: createLoadFn(group, "ArrowBack"),
    Clear: createLoadFn(group, "Clear"),
    Edit: createLoadFn(group, "Edit"),
    Menu: createLoadFn(group, "Menu"),
    Search: createLoadFn(group, "Search"),
    Close: createLoadFn(group, "Close"),
    Star: createLoadFn(group, "Star"),
    Home: createLoadFn(group, "Home"),
    Notifications: createLoadFn(group, "Notifications"),
    Settings: createLoadFn(group, "Settings"),
    MoreVert: createLoadFn(group, "MoreVert"),
    MailOutline: createLoadFn(group, "MailOutline"),
    Refresh: createLoadFn(group, "Refresh"),
    AccountBox: createLoadFn(group, "AccountBox")
}
group = "Default"
const Default: Icons = {
    Call: createLoadFn(group, "Call"),
    Add: createLoadFn(group, "Add"),
    ArrowBack: createLoadFn(group, "ArrowBack"),
    Clear: createLoadFn(group, "Clear"),
    Edit: createLoadFn(group, "Edit"),
    Menu: createLoadFn(group, "Menu"),
    Search: createLoadFn(group, "Search"),
    Close: createLoadFn(group, "Close"),
    Star: createLoadFn(group, "Star"),
    Home: createLoadFn(group, "Home"),
    Notifications: createLoadFn(group, "Notifications"),
    Settings: createLoadFn(group, "Settings"),
    MoreVert: createLoadFn(group, "MoreVert"),
    MailOutline: createLoadFn(group, "MailOutline"),
    Refresh: createLoadFn(group, "Refresh"),
    AccountBox: createLoadFn(group, "AccountBox")
}
export { Filled, Default }
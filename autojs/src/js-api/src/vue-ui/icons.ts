import { memoize } from 'lodash'
export interface Icons {
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
    ArrowDropDown: ImageVector
    Done: ImageVector
    Person: ImageVector
}

const ui = Autox.ui

function createLoadProxy(group: string): unknown {
    const e = memoize((name: string) => {
        return ui.loadIcon(group, name)
    })
    return new Proxy({ e }, {
        get(target, propKey) {
            if (typeof propKey === 'string') {
                return target.e(propKey)
            } else {
                throw new TypeError(`Filled Not exported property: ${String(propKey)} in IconGroup ${group} `)
            }
        }
    })
}


const Filled: Icons = createLoadProxy("Filled") as Icons

const Default: Icons = createLoadProxy("Default") as Icons
export { Filled, Default }
const ui = Autox.ui
export type Color = string | bigint
export interface ColorSchemeOptions {
    primary?: Color,
    onPrimary?: Color,
    primaryContainer?: Color,
    onPrimaryContainer?: Color,
    inversePrimary?: Color,
    secondary?: Color,
    onSecondary?: Color,
    secondaryContainer?: Color,
    onSecondaryContainer?: Color,
    tertiary?: Color,
    onTertiary?: Color,
    tertiaryContainer?: Color,
    onTertiaryContainer?: Color,
    background?: Color,
    onBackground?: Color,
    surface?: Color,
    onSurface?: Color,
    surfaceVariant?: Color,
    onSurfaceVariant?: Color,
    surfaceTint?: Color,
    inverseSurface?: Color,
    inverseOnSurface?: Color,
    error?: Color,
    onError?: Color,
    errorContainer?: Color,
    onErrorContainer?: Color,
    outline?: Color,
    outlineVariant?: Color,
    scrim?: Color,
    surfaceBright?: Color,
    surfaceDim?: Color,
    surfaceContainer?: Color,
    surfaceContainerHigh?: Color,
    surfaceContainerHighest?: Color,
    surfaceContainerLow?: Color,
    surfaceContainerLowest?: Color,
}

export function darkColorScheme(ops: ColorSchemeOptions) {
    const map = new Map<string, Color>()
    for (let [key, value] of Object.entries(ops)) {
        map.set(key, value)
    }
    return ui.theme.darkColorScheme(map)
}
export function lightColorScheme(ops: ColorSchemeOptions) {
    const map = new Map<string, string | bigint>()
    for (let [key, value] of Object.entries(ops)) {
        map.set(key, value)
    }
    return ui.theme.lightColorScheme(map)
}

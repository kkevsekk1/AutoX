const ui = Autox.ui

const factoryCache = new Map<string, ModifierExtBuilder>()
function loadFactory(key: string): ModifierExtBuilder {
    if (factoryCache.has(key)) {
        return factoryCache.get(key)!
    } else {
        const f = ui.getModifierExtFactory(key)
        factoryCache.set(key, f)
        return f
    }
}
function checkNumber(numbers: any[], err?: Error) {
    numbers.forEach(n => {
        if (!n) return
        if (typeof n !== "number") {
            throw err || new Error("args Invalid number")
        }
    })
}
/**
 * 设置背景色，传入'theme'表示使用当前主题的背景色,
 * 使用数值表示时必须是bigint，普通数值在java会转成Int溢出范围
 * @param color 颜色值,如`0xffffcc12n`的数值或`#ffcc89`的字符串
 * @returns 
 */
export function background(color: string | bigint | 'theme'): ModifierExt {
    return loadFactory("background").createModifierExt([color])
}
/**
 * 仅在row和column直接子组件上使用有效,表示此组件占用父组件剩余空间的权重,
 * 假如有一个row剩余高度为300，有一个子组件这设置了此修饰符为1,那么它的高度为300,
 * 如果有两个组件都设为1，那么这两个组件各得150高度
 * @param i 
 * @returns 
 */
export function weight(i: number): ModifierExt {
    return loadFactory("weight").createModifierExt([i])
}
/**
 * 
 * @returns 
 */
export function fillMaxSize(): ModifierExt {
    return loadFactory("fillMaxSize").createModifierExt([])
}
export function fillMaxWidth() {
    return loadFactory("fillMaxWidth").createModifierExt([])
}
export function fillMaxHeight() {
    return loadFactory("fillMaxHeight").createModifierExt([])
}
/**
 * 设置高度，单位为dp
 * @param width 
 * @returns 
 */
export function width(width: number) {
    return loadFactory("width").createModifierExt([width])
}
/**
 * 设置高度，单位为dp
 * @param height 
 * @returns 
 */
export function height(height: number) {
    return loadFactory("height").createModifierExt([height])
}
/**
 * 设置旋转角度
 * @param angle 一般为0~360 
 * @returns 
 */
export function rotate(angle: number): ModifierExt {
    return loadFactory("rotate").createModifierExt([angle])
}
/**
 * 设置padding，传递一个值时表示四边均使用此值，
 * 传递两个值时第一个参数表示水平padding，第二个参数表示垂直padding
 * @param left 
 * @param top 
 * @param right 
 * @param bottom 
 * @returns 
 */
export function padding(left: number, top?: number, right?: number, bottom?: number): ModifierExt {
    checkNumber([left, top, right, bottom])
    if (typeof top !== "number") {
        return loadFactory("padding").createModifierExt([left, left, left, left])
    }
    if (typeof bottom !== "number") {
        return loadFactory("padding").createModifierExt([left, top, left, top])
    }
    return loadFactory('padding').createModifierExt([left, top, right, bottom])
}
/**
 * 设置组件可点击，大部分组件会自动添加相应的点击效果
 * @param clickable 
 * @returns 
 */
export function clickable(clickable: () => {}) {
    return loadFactory('clickable').createModifierExt([clickable])
}
/**
 * 设置可水平滚动
 * @returns 
 */
export function horizontalScroll() {
    return loadFactory('horizontalScroll').createModifierExt([])
}
/**
 * 设置可垂直滚动
 * @returns 
 */
export function verticalScroll() {
    return loadFactory("verticalScroll").createModifierExt([])
}
/**
 * 设置组件最小或最大宽度
 * @param min 最小值，可以为null
 * @param max 最大值，可以为null
 * @returns 
 */
export function widthIn(min?: number, max?: number) {
    return loadFactory('widthIn').createModifierExt([min || null, max || null])
}
/**
 * 设置组件最小或最大高度
 * @param min 最小值，可以为null
 * @param max 最大值，可以为null
 * @returns 
 */
export function heightIn(min?: number, max?: number) {
    return loadFactory('heightIn').createModifierExt([min || null, max || null])
}
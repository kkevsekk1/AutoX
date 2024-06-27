
const java = Autox.java

export function invokeDefault<T>(javaobj: any,
    methodName: string,
    args?: any[]): Promise<T> {
    return java.invokeDefault(javaobj, methodName, args || [])
}

export function loadClass(className: string): JavaClass {
    return java.loadClass(className)
}
export function invokeIo<T>(javaobj: any,
    methodName: string,
    args?: any[]): Promise<T> {
    return java.invokeIo(javaobj, methodName, args || [])

}

export function invokeUi<T>(javaobj: any,
    methodName: string,
    args?: any[]): Promise<T> {
    return java.invokeUi(javaobj, methodName, args || [])
}
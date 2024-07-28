/**
 * 该模块用于与java交互
 * @packageDocumentation
 */
const java = Autox.java
/**
 * 采用默认的计算线程池异步调用java方法，返回Promise接受结果
 * @alpha
 * @param javaobj java对象，不能是js对象
 * @param methodName 要调用的方法名
 * @param args 传递的参数
 * @returns 调用结果
 */
export function invokeDefault<T>(javaobj: any,
    methodName: string,
    args?: any[]): Promise<T> {
    return java.invokeDefault(javaobj, methodName, args || [])
}
/**
 * 加载并返回一个java类
 * @alpha
 * @param className java全类名
 * @returns 
 */
export function loadClass(className: string): JavaClass {
    return java.loadClass(className)
}
/**
 * 和{@link invokeDefault}类似，采用io线程池
 * @alpha
 * @param javaobj 
 * @param methodName 
 * @param args 
 * @returns 
 */
export function invokeIo<T>(javaobj: any,
    methodName: string,
    args?: any[]): Promise<T> {
    return java.invokeIo(javaobj, methodName, args || [])

}
/**
 * 和{@link invokeDefault}类似，采用ui线程
 * @alpha
 * @param javaobj 
 * @param methodName 
 * @param args 
 * @returns 
 */
export function invokeUi<T>(javaobj: any,
    methodName: string,
    args?: any[]): Promise<T> {
    return java.invokeUi(javaobj, methodName, args || [])
}
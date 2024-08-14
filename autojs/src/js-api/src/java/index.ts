/**
 * 该模块用于与java交互
 * @packageDocumentation
 */
import { memoize } from 'lodash'
const java = Autox.java

const createPackage = memoize(function (name?: string): any {
    if (name) {
        try {
            return loadClass(name)
        } catch (e: any) { }
    }
    return new Proxy({ name }, {
        get(target, propKey) {
            if (typeof propKey !== 'string') return undefined
            if (target.name) {
                return createPackage(target.name + '.' + propKey)
            } else {
                return createPackage(propKey)
            }
        }
    })
})
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
/**
 * 用于向rhino一样访问java类，如
 * `Packages.java`或`Packages.javax`
 * 此外该模块直接导出了常用的包
 * ```js
 * import { java, android, com } from 'java'
 * 
 * new java.io.File(...)
 * ```
 */
export const Packages = createPackage()

const javaPackage = Packages.java
const androidPackage = Packages.android
const javaxPackage = Packages.javax
const comPackage = Packages.com
const netPackage = Packages.net
const androidxPackage = Packages.androidx

export {
    javaPackage as java,
    androidPackage as android,
    javaxPackage as javax,
    comPackage as com,
    netPackage as net,
    androidxPackage as androidx,
}
import { EventEmitter } from 'node:events'
const engines = Autox.engines

export class ScriptEngineProxy extends EventEmitter {
    private engine: ScriptEngine
    get id(): number {
        return this.engine.id
    }
    get isDestroyed(): boolean {
        return this.engine.isDestroyed
    }

    constructor(engine: ScriptEngine) {
        super()
        this.engine = engine
    }
    emit<K>(eventName: string | symbol, ...args: any[]): boolean {
        super.emit(eventName, ...args)
        if (typeof eventName === 'string' && this.id !== selfEngine.id) {
            this.engine.emit(eventName, ...args)
        }
        return true
    }
    forceStop() {
        if (engines.selfEngine === this) {
            process.exit(1)
            return
        }
        this.engine.forceStop()
    }
    cwd(): string {
        return this.engine.cwd()
    }

}
/**
 * 当前运行的引擎
 */
export const selfEngine = new ScriptEngineProxy(engines.myEngine())
engines.setupJs({
    emitCallback(name, ...args) {
        selfEngine.emit(name, ...args)
    },
})
export interface ExecutionConfigOptions {
    workingDirectory?: string
    arguments?: Map<string, any>
    onStart?: (execution: ScriptExecution) => void
    onSuccess?: (execution: ScriptExecution, result: any) => void
    onException?: (execution: ScriptExecution, err: any) => void
}
/**
 * 获取当前运行的引擎
 * @returns 
 */
export function myEngine(): ScriptEngineProxy {
    return selfEngine
}
/**
 * 运行一个脚本文件
 * @param path 只能是绝对路径，不支持相对路径
 * @param ops 
 * @returns 
 */
export function execScriptFile(path: string, ops: ExecutionConfigOptions) {
    if (ops) {
        const executionConfig = engines.createExecutionConfig()
        if (ops.workingDirectory) {
            executionConfig.workingDirectory = ops.workingDirectory
        }
        if (ops.arguments) {
            ops.arguments.forEach((value, key) => {
                executionConfig.arguments.set(key, value)
            })
        }
        return engines.execScriptFile(path, executionConfig, (a, ...args) => {
            if (a == 0) {
                ops.onStart?.(args[0] as ScriptExecution)
            } else if (a == 1) {
                ops.onSuccess?.(args[0] as ScriptExecution, args[1])
            } else if (a == 2) {
                ops.onException?.(args[0] as ScriptExecution, args[1])
            }
        })
    }
    return engines.execScriptFile(path, null, null)
}
/**
 * 停止所有运行中的脚本，包括自身
 * @returns 
 */
export function stopAll() {
    engines.stopAll()
}
/**
 * 获取所有运行中的脚本
 * @returns 
 */
export function getRunningEngines() {
    const r: ScriptEngineProxy[] = []
    engines.allEngine().forEach((engine) => {
        r.push(new ScriptEngineProxy(engine))
    })
    return r
}
/**
 * 向所有运行中的脚本发送事件，相当于
 * ```js
 * getRunningEngines().forEach((engine) => {
        engine.emit(event, ...args)
    })
 * ```
 * @param event 
 * @param args 
 */
export function broadcast(event: string, ...args: any) {
    getRunningEngines().forEach((engine) => {
        engine.emit(event, ...args)
    })
}
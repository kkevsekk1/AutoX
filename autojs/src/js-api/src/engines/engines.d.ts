declare namespace root.engines {
    let selfEngine: any | undefined
    function myEngine(): NodeScriptEngine
    function allEngine(): Set<ScriptEngine>
    function stopAll()
    function stopAllAndToast()
    function createExecutionConfig(): ExecutionConfig
    function execScriptFile(path: String,
        config: ExecutionConfig?,
        listener: ((a: number, ...args) => void)?
    ): ScriptExecution
    function setupJs(ops: {
        emitCallback: (name: string, ...args: any[]) => void
    })
}

interface ScriptEngine {
    id: number
    isDestroyed: boolean
    forceStop()
    cwd(): string
    emit(name: String, ...args: any)
}
interface NodeScriptEngine extends ScriptEngine {

}
type ExecutionConfig = {
    workingDirectory: string
    arguments: Map<string, any>
}
type ScriptExecution = {}
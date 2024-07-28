import { execScriptFile, stopAll } from 'engines'
import { writeFile, rm } from 'fs/promises'
import { showToast } from 'toast'


const file = process.cwd() + '/test.js'
await writeFile(file, `
    setTimeout(() => { }, 2000)
    `, "utf8")
console.log(process.cwd());

execScriptFile(file, {
    onStart() {
        showToast('开始执行')
    },
    onSuccess() {
        showToast('执行成功')
        rm(file, { force: true })
    },
    onException() {
        showToast('执行出错')
        rm(file, { force: true })
    }
})

setTimeout(() => { }, 5000)
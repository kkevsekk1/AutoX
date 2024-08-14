import { selfEngine } from 'engines'
import { showToast } from 'toast'

// console.log(myEngine());

selfEngine.on('test', (data) => {
    showToast(`收到test事件, data:${data}`)
})

//保持脚本运行
setInterval(() => { }, 1000)
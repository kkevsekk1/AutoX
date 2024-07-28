events.on('test', (data) => {
    toast(`收到test事件, data:${data}`)
})

//保持脚本运行
setInterval(() => { }, 1000)
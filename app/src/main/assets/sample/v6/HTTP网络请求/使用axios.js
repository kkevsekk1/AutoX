const axios = require("axios/index.js");
const FormData = axios.browser.FormData;

/*
  下载文件
*/
axios('https://m.baidu.com', {
    responseType: 'blob'
}).then((res) => {
    const blob = res.data
    log('blob:', blob);
    //保存blob
    //return axios.utils.saveBlobToFile(blob, savePath)
}).then(() => {
    log('下载成功')
}).catch(console.error)


/*
  使用表单
*/
let form = new FormData()
form.set('a', 'b')
form.append('b', '123')
form.append('b', '测试')
axios.post('http://baidu.com', form).then(function (res) {
    log('请求成功1');
}).catch(console.error)

/*
  使用表单上传文件
*/
let blob = axios.utils.openFile('./使用axios.js')

form.enctype = 'multipart/form-data'
form.set('file', blob)
axios.post('http://baidu.com', form).then(function (res) {
    log('请求成功2');
}).catch(console.error)

/*
  也可以使用直接传输
*/
axios.post('http://baidu.com', blob).then(function (res) {
    log('请求成功3');
}).catch(console.error)
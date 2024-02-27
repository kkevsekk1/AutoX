// init autoX setting
auto.waitFor();
auto.setMode("fast");

//  wait user change active page to 'red packet detail'
var retry = 0;
toast("请在6秒内切换到红包详情页面");
for (; retry < 3; retry++) {
    if(!id("com.tencent.mm:id/gdh").exists()){
        var sleepTime = 2;
        sleep(sleepTime * 1000)
    }else{
        break;
    }
}
if(retry >= 3){
    toast("未切换到详情页, 脚本已终止");
    console.warn("未切换到详情页, 脚本已终止")
    exit();
}

// search all red packet receiver's info
var receivers = id("com.tencent.mm:id/iwc").find();

//  def object struct
var infoList = new Array();

//  find receiver needed info
console.verbose("receiver count is: %s", receivers.size());
for (var i = 0; i < receivers.size(); i++) {
    
    var info = {
        name: "",
        value: "",
        time: ""
    };
    info.name = receivers.get(i).findOne(id("com.tencent.mm:id/giz")).text();
    info.value = receivers.get(i).findOne(id("com.tencent.mm:id/git")).text();
    info.time = receivers.get(i).findOne(id("com.tencent.mm:id/gj0")).text();
    infoList[i] = handleInfo(info);
}
console.verbose("All receiver info is: ", infoList);

var result = buildCopyInfo(infoList);

// set result info on clipboard
setClip(result)

toast("内容已复制");
console.verbose("Copy red packet info success, result is: %s", result);


/**
 * handle origin info
 */
function handleInfo(info) {
    //  processing emoji on name

    //  processing "." and chinese on value

    //  format date

    return info;
}

/**
 * build info to copy, should be format info type.
 */
function buildCopyInfo(source) {
    var result = "红包详情数据\n";
    for (var i = 0; i < source.length; i++) {
        result += "[" + source[i].name + "] [" + source[i].value + "] [" + source[i].time + "]\n";
    }

    return result;
}

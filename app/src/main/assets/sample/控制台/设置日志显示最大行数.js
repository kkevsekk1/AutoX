console.show(t);
console.log("运行结束自动关闭");
console.log("调整大小...");
console.setSize(1000, 1000);
sleep(2000);
console.log("调整位置...");
console.setPosition(0, 500);
sleep(2000);
console.show(false);
console.setMaxLines(10);
var i=0;
while(true){
    console.log(i)
    i++;
    sleep(500);
}




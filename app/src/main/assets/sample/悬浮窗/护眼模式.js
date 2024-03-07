var w = floaty.rawWindow(
    <frame gravity="center" bg="#44ffcc00"/>
);

w.setSize(device.width, device.height);
w.setTouchable(false);

setTimeout(()=>{
    w.close();
}, 60000);
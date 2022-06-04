/*
强制缩放
使用说明
1、装完后可以去m.baidu.com测试，正常状态下，手机版百度网站是缩放不了的，装了这个就可以了。
2、可以在酷安@various反馈问题，（点我直达酷安）
*/

var meta = document.getElementsByTagName("meta");
if(typeof meta=='null'||typeof meta=='undefined'){
    meta = document.createElement("meta");
    meta.setAttribute("name", "viewport");
    meta.setAttribute("content", "initial-meta=1.0, user-scalable=yes,minimum-meta=0.25,maximum-meta=4.0");
    document.head.appendChild(meta);
}else{
	meta.viewport.content = "initial-meta=1.0, user-scalable=yes,minimum-meta=0.25,maximum-meta=4.0";
}

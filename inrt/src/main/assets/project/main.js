"ui";
ui.layout(
    <vertical padding="16" id="parent">
        <TextView text="权限设置" gravity="center" textSize="24sp" />
        <Switch id="autoService" text="无障碍服务" checked="{{auto.service != null}}" padding="8 8 8 8" textSize="15sp" />
      <Button id="ok" text="确定"/>
    </vertical>
);
ui.ok.click(function(){
    //通过getText()获取输入的内容
    engines.execScriptFile("main1.js");
});
ui.autoService.on("check", function (checked) {
    // 用户勾选无障碍服务的选项时，跳转到页面让用户去开启
    if (checked && auto.service == null) {
        app.startActivity({
            action: "android.settings.ACCESSIBILITY_SETTINGS"
        });
    }
    if (!checked && auto.service != null) {
        auto.service.disableSelf();
    }
});

log("测试");
toast("测试");
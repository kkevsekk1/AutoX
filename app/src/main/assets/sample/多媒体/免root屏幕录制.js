"ui";

importClass(android.content.Context);
importClass(android.hardware.display.DisplayManager);
importClass(android.media.MediaRecorder);
importClass(java.io.File);

runtime.requestPermissions(["WRITE_EXTERNAL_STORAGE", "READ_EXTERNAL_STORAGE", "RECORD_AUDIO"]);

mMediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
mMediaRecorder = new MediaRecorder();
mVirtualDisplay = null;
saveDir = "/sdcard";
saveWidth = device.width;
saveHeight = device.height;
saveTime = 10 * 1000; // 单位：毫秒
isRunning = false;

ui.layout(
    <vertical>
        <appbar>
            <toolbar title="免root屏幕录制" />
        </appbar>
        <Switch id="permissions" text="音频录制及存储权限" checked="true" gravity="center"/>
        <frame gravity="center">
            <text text="AutoX" gravity="center" />
        </frame>
        <button text="免root屏幕录制" style="Widget.AppCompat.Button.Colored" id="button" />
    </vertical>
);

ui.button.click(function () {
    if (isRunning) {
        stopRecord();
        ui.button.setText("免root屏幕录制");
    } else {
        activity.startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), 666);
    }
});
// 无障碍服务
ui.permissions.on("check", function (checked) {
    if (checked) {
        runtime.requestPermissions(["WRITE_EXTERNAL_STORAGE", "READ_EXTERNAL_STORAGE", "RECORD_AUDIO"]);
    } else {
        toastLog("权限不足！");
    }
});
ui.emitter.on("resume", function () {
    ui.permissions.checked = true;
});

// 获取屏幕录制授权
ui.emitter.on("activity_result", (requestCode, resultCode, data) => {
    mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
    if (mMediaProjection) {
        startRecord();
        ui.button.setText("视频录制中(点击停止)……");
        setTimeout(function () {
            stopRecord();
            ui.button.setText("免root屏幕录制");
        }, saveTime)
    }
});

events.on("exit", function () {
    stopRecord();
});

function startRecord() {
    if (mMediaProjection == null || isRunning) {
        return false;
    }
    file = new File(saveDir, "screen_record.mp4");
    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
    mMediaRecorder.setOutputFile(file.getAbsolutePath());
    mMediaRecorder.setVideoSize(saveWidth, saveHeight);
    mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    mMediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
    mMediaRecorder.setVideoFrameRate(30);
    try {
        mMediaRecorder.prepare();
    } catch (e) {
        toastLog(e);
    }
    mVirtualDisplay = mMediaProjection.createVirtualDisplay("免root屏幕录制", saveWidth, saveHeight, 1, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    mMediaRecorder.start();
    isRunning = true;
    return true;
}

function stopRecord() {
    if (!isRunning) {
        return false;
    }
    mMediaRecorder.stop();
    mMediaRecorder.reset();
    mVirtualDisplay.release();
    mMediaProjection.stop();
    isRunning = false;
    return true;
}


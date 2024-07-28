"ui";

ui.layout(
  <vertical>
    <text id="name" text="音乐播放器" textSize="22sp" textColor="#fbfbfe" bg="#00afff" w="*" gravity="center"></text>
    <button id="play">播放音乐</button>
    <button id="next">下一曲</button>
    <button id="pause">暂停</button>
  </vertical>
);
var musicDir = '/sdcard/Music';
if (!files.isDir(musicDir)) {
  toastLog(musicDir + "目录不存在！")
}
var musicFiles = files.listDir(musicDir, function (name) {
  return name.endsWith(".mp3") || name.endsWith(".wma") || name.endsWith(".wav")
});
var i = 0;
var musicPath = "";
if (musicFiles.length > 0) {
  musicPath = files.join(musicDir, musicFiles[i]);
  ui.name.setText(files.getNameWithoutExtension(musicPath));
} else {
  toastLog(musicDir + "目录下没有音频文件！")
}
ui.pause.click(function () {
  media.pauseMusic();
});
ui.next.click(function () {
  musicPath = files.join(musicDir, musicFiles[(i + 1) % musicFiles.length]);
  if (files.isFile(musicPath)) {
    ui.name.setText(files.getNameWithoutExtension(musicPath));
    media.playMusic(musicPath, 0.8);
  } else {
    toastLog(musicPath + "音频文件不存在！")
  }
}
);
ui.play.click(function () {
  if (media.isMusicPlaying()) {
    return true;
  } else {
    if (files.isFile(musicPath)) {
      ui.name.setText(files.getNameWithoutExtension(musicPath));
      media.playMusic(musicPath, 0.8);
    } else {
      toastLog(musicPath + "音频文件不存在！")
    }
  }
});
ui.emitter.on("pause", () => {
  if (media.isMusicPlaying()) {
    media.pauseMusic();
  }
});
ui.emitter.on("resume", () => {
  ui.post(function () {
    media.resumeMusic();
  }, 200);
});
events.on("exit", function () {
  media.stopMusic();
});
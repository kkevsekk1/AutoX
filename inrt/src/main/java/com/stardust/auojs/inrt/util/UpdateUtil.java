package com.stardust.auojs.inrt.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ProgressBar;

import com.linsh.utilseverywhere.ContextUtils;
import com.stardust.auojs.inrt.BuildConfig;
import com.stardust.auojs.inrt.R;
import com.stardust.util.IntentUtil;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by TCX on 2016/9/6.
 */
public class UpdateUtil {
    private static final int SHOW_NOTICE_DIALOG = 1;
    private static final int START_DOWNLOAD_APK = 2;
    private static final int DOWNLOAD = 3;
    private static final int DOWNLOAD_FINISH = 4;
    private static final String CHECK_UPDATE_URL = "http://120.25.164.233:8080/appstore/app/checkversion?id=22";
    private Context mContext;
    private ProgressBar mProgress;
    private Dialog mDownloadDialog;
    private String apkSavePath;

    // 服务器端的版本信息
    private int versionCode;
    private String description;
    private String apkurl;
    private String version;
    private String apkname;

    private boolean cancelUpdate;
    private int progress;

    public UpdateUtil(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * 检测软件更新
     */
    public void checkUpdate() {
        new Thread() {
            @Override
            public void run() {
                String result = HttpUtil.doPost(CHECK_UPDATE_URL);
                try {
                    JSONObject dataJson = new JSONObject(result);
                    versionCode = dataJson.getInt("versionCode");
                    description = (String) dataJson.get("description");
                    apkurl = (String) dataJson.get("apkurl");
                    version = (String) dataJson.get("version");
                    apkname = (String) dataJson.get("apkname");

                    if (isUpdate(versionCode)) {
                        // 显示提示对话框
                        Message msg = new Message();
                        msg.what = SHOW_NOTICE_DIALOG;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    /**
     * 获取软件版本号
     *
     * @param context
     * @return
     */
    private int getVersionCode(Context context) {
        int versionCode = 0;
        try {
            // 获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    private boolean isUpdate(int serviceCode) {
        boolean flag = false;
        // 获取当前软件版本
        int versionCode = getVersionCode(mContext);
        // 版本判断
        if (serviceCode > versionCode) {
            flag = true;
        }
        return flag;
    }

    private void showNoticeDialog() {
        // 构造对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("发现最新版本");
        builder.setMessage(description);
        builder.setCancelable(false);
        // 更新
        builder.setPositiveButton("直接更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // 显示下载对话框
                showDownloadDialog();
            }
        });
        // 稍后更新
        builder.setNeutralButton("稍后更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        // 稍后更新
        builder.setNegativeButton("浏览器下载", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Uri uri = Uri.parse(apkurl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                mContext.startActivity(intent);
            }
        });
        Dialog noticeDialog = builder.create();
        noticeDialog.show();
    }

    private void showDownloadDialog() {
        // 构造软件下载对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("正在下载");
        builder.setCancelable(false);
        // 给下载对话框增加进度条
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.softupdate_progress, null);
        mProgress = (ProgressBar) v.findViewById(R.id.update_progress);
        builder.setView(v);
        // 取消更新
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // 设置取消状态
                cancelUpdate = true;
            }
        });
        mDownloadDialog = builder.create();
        mDownloadDialog.show();
        // 下载文件
        Message downloadMessage = new Message();
        downloadMessage.what = START_DOWNLOAD_APK;
        handler.sendMessage(downloadMessage);
    }

    private void downloadApk() {
        new Thread() {
            @Override
            public void run() {
                Log.i("sysout", "开始下载");
                try {
                    String sdpath = Environment.getExternalStorageDirectory() + "/";
                    apkSavePath = sdpath + "/download";
                    URL url = new URL(apkurl);
                    // 创建连接
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();
                    // 获取文件大小
                    int length = conn.getContentLength();
                    // 创建输入流
                    InputStream is = conn.getInputStream();

                    File file = new File(apkSavePath);
                    // 判断文件目录是否存在
                    if (!file.exists()) {
                        file.mkdir();
                    }
                    File apkFile = new File(apkSavePath, apkname);
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    int count = 0;
                    // 缓存
                    byte buf[] = new byte[1024];
                    // 写入到文件中
                    do {
                        int numread = is.read(buf);
                        count += numread;
                        // 计算进度条位置
                        progress = (int) (((float) count / length) * 100);
                        // 更新进度
                        Message downloadMessage = new Message();
                        downloadMessage.what = DOWNLOAD;
                        handler.sendMessage(downloadMessage);
                        if (numread <= 0) {
                            // 下载完成
                            Log.i("sysout", "下载完成");
                            Message finishMessage = new Message();
                            finishMessage.what = DOWNLOAD_FINISH;
                            handler.sendMessage(finishMessage);
                            break;
                        }
                        // 写入文件
                        fos.write(buf, 0, numread);
                    } while (!cancelUpdate);// 点击取消就停止下载.
                    fos.flush();
                    fos.close();
                    is.close();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // 取消下载对话框显示
                mDownloadDialog.dismiss();
            }
        }.start();

    }

    private void installApk() {

        File apkfile = new File(apkSavePath, apkname);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

         String AUTHORITY =   ContextUtils.getPackageName()+ ".fileprovider";

        IntentUtil.installApkOrToast(mContext, apkfile.getPath(),AUTHORITY);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_NOTICE_DIALOG:
                    showNoticeDialog();
                    break;
                case START_DOWNLOAD_APK:
                    downloadApk();
                    break;
                case DOWNLOAD:
                    // 设置进度条位置
                    mProgress.setProgress(progress);
                    break;
                case DOWNLOAD_FINISH:
                    // 安装文件
                    installApk();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
}

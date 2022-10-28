package com.mojin.qidon;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.alibaba.fastjson.JSONObject;
import java.io.File;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApkUpdate {
    static View UpdateView;
    static PopupWindow popupWindow;

    public static void update(final Activity context) {
        new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        //获取JSON并解析
                        OkHttpClient okHttpClient = new OkHttpClient();
                        Request request = new Request.Builder()
                            .url("http://106.53.148.51/qdjgx/bbgx.json")
                            .get()
                            .build();
                        Response response = okHttpClient.newCall(request).execute();
                        String ApiJson = response.body().string();

                        
                        JSONObject Json = JSONObject.parseObject(ApiJson);
                        final String Content = Json.getString("nr");
                        final String Version = Json.getString("bb");

                        context.runOnUiThread(new Runnable(){
                                @Override
                                public void run() {
                                    try {
                                        //返回文本
                                        if (Version == null) {
                                            Toast.makeText(context, "网络出现故障，请检查你的网络", Toast.LENGTH_SHORT).show();
                                        } else {
                                            if (Version.equals(GetVersion(context))) {
                                                Toast.makeText(context, "恭喜，已经是最新版本！", Toast.LENGTH_SHORT).show();
                                            } else {
                                                //弹窗相关
                                                UpdateView = context.getLayoutInflater().inflate(R.layout.updatelayout, null);
                                                popupWindow = new PopupWindow(UpdateView, -1, -1);
                                                popupWindow.setClippingEnabled(false);
                                                popupWindow.showAtLocation(context.findViewById(R.id.StartGameView), Gravity.CENTER, 0, 0);

                                                //更新内容
                                                TextView ContentView = UpdateView.findViewById(R.id.UpdateLayoutContent);
                                                ContentView.setText(Content);

                                                //背景
                                                UpdateView.findViewById(R.id.UpdateLayoutBackGround).setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            popupWindow.dismiss();
                                                        }
                                                    });

                                                UpdateView.findViewById(R.id.UpdateLayoutCard).setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                        }
                                                    });

                                                //稍后更新
                                                UpdateView.findViewById(R.id.UpdateLayoutBack).setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            popupWindow.dismiss();

                                                            //震动
                                                            Vibrator vibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
                                                            vibrator.vibrate(100);
                                                        }
                                                    });

                                                //立即更新
                                                UpdateView.findViewById(R.id.UpdateLayoutStart).setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            new Thread(new Runnable(){
                                                                    @Override
                                                                    public void run() {
                                                                        try {
                                                                            final String ApkJson = url("/GG/PSP魔禁相关/游戏本体相关/[手机]魔禁启动姬_" + Version + ".apk", context);
                                                                            JSONObject ApkJsonObject = JSONObject.parseObject(ApkJson);
                                                                            ApkJsonObject = JSONObject.parseObject(ApkJsonObject.getString("data"));
                                                                            final String DownloadURL = ApkJsonObject.getString("raw_url");
                                                                            AppNotification.send(context, "启动姬更新", "0%", 5);
                                                                            DownloadFiles.get().download(DownloadURL, "/storage/emulated/0/Download", "[手机]魔禁启动姬_" + Version + ".apk", new DownloadFiles.OnDownloadListener() {
                                                                                    //下载完成
                                                                                    @Override
                                                                                    public void onDownloadSuccess() {
                                                                                        try {
                                                                                            //安装程序的apk文件路径     
                                                                                            File apk = new File("/storage/emulated/0/Download/[手机]魔禁启动姬_" + Version + ".apk");
                                                                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                                                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                                                                //注意第二个参数，要保持和manifest中android:authorities的值相同
                                                                                                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", apk);
                                                                                                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                                                                                            } else {
                                                                                                intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
                                                                                            }
                                                                                            
                                                                                            //设置服务
                                                                                            PendingIntent pendingIntent;
                                                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                                                                Toast.makeText(context,String.valueOf(Build.VERSION.SDK_INT),Toast.LENGTH_SHORT).show();
                                                                                                pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                                                                                            } else {
                                                                                                pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
                                                                                            }
                                                                                            
                                                                                            //发送通知
                                                                                            AppNotification.delete(context, 5);
                                                                                            AppNotification.ordinary(context, "启动姬更新", "下载完成，点击我进行安装", 15, pendingIntent, false);
                                                                                            
                                                                                        } catch (Exception e) {
                                                                                            AppNotification.error(context, ErrorGet.Log(e));
                                                                                        }
                                                                                    }

                                                                                    //下载进度
                                                                                    @Override
                                                                                    public void onDownloading(int progress) {
                                                                                        AppNotification.update(context, "启动姬更新", String.valueOf(progress) + "%", 5, progress);
                                                                                    }

                                                                                    //下载失败
                                                                                    @Override
                                                                                    public void onDownloadFailed() {
                                                                                        //发送通知
                                                                                        AppNotification.ordinary(context, "启动姬更新", "下载失败，可能是网络问题", 5, null, false);
                                                                                    }
                                                                                });
                                                                        } catch (Exception e) {
                                                                            AppNotification.error(context, ErrorGet.Log(e));

                                                                        }
                                                                    }
                                                                }).start();

                                                            //震动
                                                            Vibrator vibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
                                                            vibrator.vibrate(100);
                                                        }
                                                    });

                                            }
                                        }
                                    } catch (Exception e) {
                                        AppNotification.error(context, ErrorGet.Log(e));
                                    }
                                }
                            });
                            
                    } catch (Exception e) {
                        AppNotification.error(context, ErrorGet.Log(e));
                    }
                }
            }).start();
    }

    private static String GetVersion(Context context) {
        PackageManager manager = context.getPackageManager();
        String version = null;
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            AppNotification.error(context, ErrorGet.Log(e));
        }
        return version;
    }

    private static String url(String path, Context context) {
        try {
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                .add("page_size", "10")//设置参数名称和参数值
                .add("page_index", "1")
                .add("path", path)
                .build();

            Request request = new Request.Builder()
                .url("http://106.53.148.51:3100/api/fs/get")
                .post(body)
                .addHeader("content-type", "multipart/form-data; boundary=---011000010111000001101001")
                .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
            return null;
        }
    }
}

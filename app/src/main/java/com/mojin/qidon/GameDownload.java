package com.mojin.qidon;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import com.alibaba.fastjson.JSONObject;
import com.sd.lib.switchbutton.SwitchButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GameDownload {
    //设置文件和目录的变量
    final static String api = "http://106.53.148.51:3100/api/fs/get";
    final static String GameIso = "/GG/PSP魔禁相关/游戏本体相关/魔法禁书目录.iso";
    final static String MisakaApk = "/GG/PSP魔禁相关/游戏本体相关/御坂网络.apk";
    final static String PPSSPPApk = "/GG/PSP魔禁相关/游戏本体相关/ppsspp.apk";
    final static String SaveData = "yxbt/SAVEDATA.zip";
    final static String SaveDataPath = "/storage/emulated/0/PSP/SAVEDATA/";
    final static String SavePath = "/storage/emulated/0/PSP/GAME/";

    public static void GameDownload(SwitchButton Game_ISO, SwitchButton MisakaFCN, SwitchButton GamePPSSPP, SwitchButton GameSave, final Activity context) {
        boolean XuanZe = false;

        //获取开关选中状态
        boolean GameISOChecked = Game_ISO.isChecked();
        boolean MisakaFCNChecked = MisakaFCN.isChecked();
        boolean GamePPSSPPChecked = GamePPSSPP.isChecked();
        boolean GameSaveChecked = GameSave.isChecked();

        if (((MainApplication)context.getApplication()).Countdown != 0) {
            Toast.makeText(context, "在" + String.valueOf(((MainApplication)context.getApplication()).Countdown) + "秒后可以再次尝试", Toast.LENGTH_SHORT).show();
        } else {
            //下载游戏本体
            if (GameISOChecked) {
                new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                final String ApiJson = url(GameIso, context);
                                JSONObject Json = JSONObject.parseObject(ApiJson);
                                Json = JSONObject.parseObject(Json.getString("data"));
                                final String DownloadURL = Json.getString("raw_url");
                                AppNotification.send(context, "游戏本体", "0%", 0);
                                DownloadFiles.get().download(DownloadURL, SavePath, "魔法禁书目录.iso", new DownloadFiles.OnDownloadListener() {
                                        //下载完成
                                        @Override
                                        public void onDownloadSuccess() {
                                            //发送通知
                                            AppNotification.delete(context, 0);
                                            AppNotification.ordinary(context, "游戏本体", "下载完成！快点立即开始进游戏玩吧", 10, null, false);
                                        }

                                        //下载进度
                                        @Override
                                        public void onDownloading(int progress, String length) {
                                            AppNotification.update(context, "游戏本体", length + " ● " + String.valueOf(progress) + "%", 0, progress);
                                        }

                                        //下载失败
                                        @Override
                                        public void onDownloadFailed() {
                                            //发送通知
                                            AppNotification.ordinary(context, "游戏本体", "下载失败，可能是网络问题", 0, null, false);
                                        }
                                    });
                            } catch (Exception e) {
                                AppNotification.error(context, ErrorGet.Log(e));
                            }
                        }
                    }).start();
            }

            //解压毕业存档
            if (GameSaveChecked) {
                ZipInputStream ZipInput = null;
                ZipEntry entry;
                File FilePath = new File(SaveDataPath);

                try {
                    ZipInput = new ZipInputStream(context.getResources().getAssets().open(SaveData));
                    while (true) {
                        entry = ZipInput.getNextEntry();
                        if (entry == null) {
                            break;
                        }

                        if (entry.isDirectory()) {
                            continue;
                        }

                        File f = new File(FilePath , entry.getName());
                        if (!f.getParentFile().exists()) {
                            f.getParentFile().mkdirs();
                        }

                        int count = -1;
                        byte[] buf = new byte[1024];
                        //输出流

                        FileOutputStream fos = new FileOutputStream(f);
                        //压缩流循环读取 压缩包中的文件 直到读完
                        while ((count = ZipInput.read(buf)) != -1) {
                            //写入
                            fos.write(buf , 0 , count);
                            //流刷新（提升效率）
                            fos.flush();
                        }
                        //关闭流
                        fos.close();
                        //关闭本次条目，跳到下一个
                        ZipInput.closeEntry();
                    }
                } catch (Exception e) {
                    //发送通知
                    AppNotification.ordinary(context, "毕业存档", "存档...存档它寄了", 1, null, false);
                    AppNotification.error(context, ErrorGet.Log(e));
                } finally {
                    if (ZipInput != null) {
                        try {
                            ZipInput.close();
                        } catch (IOException e) {
                            AppNotification.error(context, ErrorGet.Log(e));
                        }
                    }
                    //发送通知
                    AppNotification.ordinary(context, "毕业存档", "存档弄好了！", 1, null, false);
                }
            }
            //下载御坂网络
            if (MisakaFCNChecked) {
                new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                final String ApiJson = url(MisakaApk, context);
                                JSONObject Json = JSONObject.parseObject(ApiJson);
                                Json = JSONObject.parseObject(Json.getString("data"));
                                final String DownloadURL = Json.getString("raw_url");
                                AppNotification.send(context, "御坂网络", "0%", 2);
                                DownloadFiles.get().download(DownloadURL, SavePath, "御坂网络.apk", new DownloadFiles.OnDownloadListener() {
                                        //下载完成
                                        @Override
                                        public void onDownloadSuccess() {
                                            try {
                                                //安装程序的apk文件路径     
                                                File apk = new File("/storage/emulated/0/PSP/GAME/御坂网络.apk");
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

                                                //安装御坂网络
                                                context.startActivity(intent);

                                                //设置服务
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                PendingIntent pendingIntent;
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                                                } else {
                                                    pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
                                                }

                                                //发送通知
                                                AppNotification.delete(context, 2);
                                                AppNotification.ordinary(context, "御坂网络", "下载完成，点击我进行安装", 12, pendingIntent, false);
                                            } catch (Exception e) {
                                                AppNotification.error(context, ErrorGet.Log(e));
                                            }
                                        }

                                        //下载进度
                                        @Override
                                        public void onDownloading(int progress, String length) {
                                            AppNotification.update(context, "御坂网络", length + " ● " + String.valueOf(progress) + "%", 2, progress);
                                        }

                                        //下载失败
                                        @Override
                                        public void onDownloadFailed() {
                                            //发送通知
                                            AppNotification.ordinary(context, "御坂网络", "下载失败，可能是网络问题", 2, null, false);
                                        }
                                    });
                            } catch (Exception e) {
                                AppNotification.error(context, ErrorGet.Log(e));

                            }
                        }
                    }).start();
            }


            //下载模拟器
            if (GamePPSSPPChecked) {
                new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                final String ApiJson = url(PPSSPPApk, context);
                                JSONObject Json = JSONObject.parseObject(ApiJson);
                                Json = JSONObject.parseObject(Json.getString("data"));
                                final String DownloadURL = Json.getString("raw_url");
                                context.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AppNotification.send(context, "PSP模拟器", "0%", 3);
                                        }
                                    });
                                DownloadFiles.get().download(DownloadURL, SavePath, "PPSSPP.apk", new DownloadFiles.OnDownloadListener() {
                                        //下载完成
                                        @Override
                                        public void onDownloadSuccess() {
                                            try {
                                                File apk = new File("/storage/emulated/0/PSP/GAME/PPSSPP.apk");
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

                                                //安装模拟器
                                                context.startActivity(intent);

                                                //设置服务
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                PendingIntent pendingIntent;
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
                                                } else {
                                                    pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
                                                }
                                                //发送通知
                                                AppNotification.delete(context, 3);
                                                AppNotification.ordinary(context, "PSP模拟器", "下载完成，点击我进行安装", 13, pendingIntent, false);
                                            } catch (Exception e) {
                                                AppNotification.error(context, ErrorGet.Log(e));
                                            }
                                        }

                                        //下载进度
                                        @Override
                                        public void onDownloading(int progress, String length) {
                                            AppNotification.update(context, "PSP模拟器", length + " ● " + String.valueOf(progress) + "%", 3, progress);
                                        }

                                        //下载失败
                                        @Override
                                        public void onDownloadFailed() {
                                            //发送通知
                                            AppNotification.ordinary(context, "PSP模拟器", "下载失败，可能是网络问题", 3, null, false);
                                        }
                                    });
                            } catch (Exception e) {
                                AppNotification.error(context, ErrorGet.Log(e));

                            }
                        }
                    }).start();
            }

            //判断有无选择
            if (!GameISOChecked && !MisakaFCNChecked && !GamePPSSPPChecked && !GameSaveChecked) {
                Toast.makeText(context, "欸？一个也没有？", Toast.LENGTH_SHORT).show();
                XuanZe = true;
            }


            //提示
            if (!XuanZe) {
                Toast.makeText(context, "火力全开！", Toast.LENGTH_SHORT).show();
            }

            //CD限制
            ((MainApplication)context.getApplication()).Countdown = 15;
            new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            for (int i = 1 ;i <= 15 ;i++) {
                                ((MainApplication)context.getApplication()).Countdown--;
                                Thread.sleep(1000);
                            }
                        } catch (Exception e) {
                            AppNotification.error(context, ErrorGet.Log(e));
                        }
                    }
                }).start();
        }
    }


    public static String url(String path, Context context) {
        try {
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                .add("page_size", "10")//设置参数名称和参数值
                .add("page_index", "1")
                .add("path", path)
                .build();

            Request request = new Request.Builder()
                .url(api)
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

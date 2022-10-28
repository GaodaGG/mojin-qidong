package com.mojin.qidon;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.app.NotificationManagerCompat;
import com.mojin.qidon.StartServer.ServerA;
import com.mojin.qidon.StartServer.ServerB;
import com.mojin.qidon.StartServer.ServerC;
import com.mojin.qidon.StartServer.ServerD;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class StartUpMap {
    public static void StatUpMap(Context context) {
        // File files = new File("/sdcard/Android/data/com.mojin.qidon/files/");
        File SetUpFile = new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini");
        String NewSetUp = "[功能性]\n自动开始游戏 = False\n第一次点击立即开始 = True\n快捷方式 = False\n自动更新 = True\n自动打开悬浮窗 = False\n[娱乐性]\n虚幻魔禁 = False\n[御坂网络]\n御坂网络选择 = 御坂网络\n密码 = default\n服务器自动选择 = 2600\n自动输入账号密码 = False";
        //检测设置文件

        if (!SetUpFile.exists()) {
            try {
                context.getExternalFilesDir("SetUp");
                SetUpFile.createNewFile();
                FileOutputStream out = new FileOutputStream(SetUpFile);
                byte Outputbuy[] = NewSetUp.getBytes();
                out.write(Outputbuy);
                out.close();
            } catch (Exception e) {
                AppNotification.error(context, ErrorGet.Log(e));
            }
        }

        //获取设置内容
        try {
            FileInputStream in = new FileInputStream(SetUpFile);
            byte Inputbyt[] = new byte[1024];
            int len = in.read(Inputbyt);
            String SetUpText = new String(Inputbyt, 0, len);
            in.close();
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
        }


        //检测通知权限
        try {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            // areNotificationsEnabled方法的有效性官方只最低支持到API 19，低于19的仍可调用此方法不过只会返回true，即默认为用户已经开启了通知。
            boolean isOpened = manager.areNotificationsEnabled();
            if (!isOpened) {
                Toast.makeText(context, "检测到没有给予通知权限，请先给予权限", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                if (Build.VERSION.SDK_INT >= 26) {
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                    intent.putExtra(Notification.EXTRA_CHANNEL_ID, context.getApplicationInfo().uid);
                } else if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT <= 25) {
                    intent.putExtra("app_package", context.getPackageName());
                    intent.putExtra("app_uid", context.getApplicationInfo().uid);
                }
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Intent intent = new Intent();
            //下面这种方案是直接跳转到当前应用的设置界面。
            //https://blog.csdn.net/ysy950803/article/details/71910806
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            context.startActivity(intent);
        }

        /*
         *检查通知渠道
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //下载通知
            NotificationChannel DownloadChannel = new NotificationChannel("ProgressNotification", "下载进度", 5);
            DownloadChannel.enableVibration(false);
            DownloadChannel.setSound(null, null);
            DownloadChannel.setImportance(1);
            NotificationManager DownloadNotificationManager = context.getSystemService(NotificationManager.class);
            DownloadNotificationManager.createNotificationChannel(DownloadChannel);

            //错误通知
            NotificationChannel ErrorChannel = new NotificationChannel("ErrorNotification", "错误通知", NotificationManager.IMPORTANCE_HIGH);
            ErrorChannel.enableVibration(false);
            ErrorChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            NotificationManager ErrorNotificationManager = context.getSystemService(NotificationManager.class);
            ErrorNotificationManager.createNotificationChannel(ErrorChannel);

            //普通通知
            NotificationChannel OrdinaryChannel = new NotificationChannel("OrdinaryNotification", "普通通知", NotificationManager.IMPORTANCE_HIGH);
            OrdinaryChannel.enableVibration(false);
            OrdinaryChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            NotificationManager OrdinaryNotificationManager = context.getSystemService(NotificationManager.class);
            OrdinaryNotificationManager.createNotificationChannel(OrdinaryChannel);
        }

        /*
         *添加快捷方式
         */
        try {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            Class[] ShortCutClass = {ServerA.class, ServerB.class, ServerC.class, ServerD.class};
            String[] ShortCutShortID = {"ServerA", "ServerB", "ServerC", "ServerD"};
            String[] ShortCutLabel = {"从2600互锤", "从5000互锤", "从1114互锤", "从1938互锤"};
            int[] ShortCutIcon = {R.drawable.shortcut_2600, R.drawable.shortcut_5000, R.drawable.shortcut_1114, R.drawable.shortcut_1938};
            
            if (shortcutManager.getDynamicShortcuts().size() < 4) {
                for (int i = 0; i < ShortCutShortID.length; i++) {
                    ShortCut.AddShortCut(context, ShortCutClass[i], ShortCutLabel[i], ShortCutShortID[i], ShortCutIcon[i]);
                }
            } else {
                for (int i = 0; i < ShortCutShortID.length; i++) {
                    ShortCut.UpdateShortCut(context, ShortCutClass[i], ShortCutLabel[i], ShortCutShortID[i], ShortCutIcon[i]);
                }
            }
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
        }
    }
}



package com.mojin.qidon;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.Random;

public class AppNotification {
    public static void send(Activity context, String Title, String textContent, int ID) {
        //设置服务
        Intent intent = new Intent(context, FrontPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        }

        //创建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "ProgressNotification")
            .setSmallIcon(R.drawable.ccz)
            .setContentTitle(Title)
            .setContentText(textContent)
            .setContentIntent(pendingIntent)
            .setProgress(100, 0, false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        //发送通知
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(ID, builder.build());

    }

    public static void update(Activity context, String Title, String textContent, int ID, int progress) {
        //设置服务
        Intent intent = new Intent(context, FrontPage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        }

        //创建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "ProgressNotification")
            .setSmallIcon(R.drawable.ccz)
            .setContentTitle(Title)
            .setContentText(textContent)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        //修改通知
        if (progress == 100) {
            builder.setOngoing(false);
        }

        //发送通知
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(ID, builder.build());
    }

    public static void error(Context context, String ErrorText) {
        try {
            //随机ID
            Random r = new Random();
            int ID = r.nextInt(2147483647);
            
            //设置服务
            Intent intent = new Intent(context, ErrorGet.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            }

            //创建通知
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "ErrorNotification")
                .setSmallIcon(R.drawable.ccz)
                .setContentTitle("应用发生了意想不到的错误")
                .setContentText("错误内容：" + ErrorText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ccz, "复制报错", pendingIntent);

            //发送通知
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(ID, builder.build());
        } catch (Exception e) {
            //获取剪贴板管理器：  
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);  
            // 创建普通字符型ClipData  
            ClipData mClipData = ClipData.newPlainText("Label", ErrorGet.Log(e));  
            // 将ClipData内容放到系统剪贴板里。  
            cm.setPrimaryClip(mClipData);
        }
    }

    public static void ordinary(Context context, String Title, String textContent, int ID, PendingIntent pendingIntent, boolean Delete) {
        //创建通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "OrdinaryNotification")
            .setSmallIcon(R.drawable.ccz)
            .setContentTitle(Title)
            .setContentText(textContent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(Delete)
            .setAutoCancel(true);

        //设置通知点击事件
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        //发送通知
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(ID, builder.build());
    }

    public static void delete(Context context, int ID) {
        //发送通知
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(ID);
    }
}

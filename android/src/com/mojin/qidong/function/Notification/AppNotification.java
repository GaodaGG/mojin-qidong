package com.mojin.qidong.function.Notification;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mojin.qidong.R;
import com.mojin.qidong.ui.layout.MainActivity;

import java.util.Random;

public class AppNotification {
	public static void download(Activity context, String Title, String textContent, int ID) {
		//设置服务
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

		PendingIntent pendingIntent;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		} else {
			pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		}

		//创建通知
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "ProgressNotification")
				.setSmallIcon(R.drawable.avatar)
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
		Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

		PendingIntent pendingIntent;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		} else {
			pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		}

		//创建通知
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "ProgressNotification")
				.setSmallIcon(R.drawable.avatar)
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

	public static void error(Context context, String errorText) {
		try {
			//随机ID
			Random r = new Random();
			int ID = r.nextInt(2147483647);

			//设置服务
			Intent intent = new Intent(context, CopyReceiver.class);
			intent.putExtra("errorText", errorText);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

			PendingIntent pendingIntent;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
			} else {
				pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
			}

			//创建通知
			NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "ErrorNotification")
					.setSmallIcon(R.drawable.avatar)
					.setContentTitle("应用发生了意想不到的错误")
					.setContentText(errorText)
					.setPriority(NotificationCompat.PRIORITY_DEFAULT)
					.addAction(R.drawable.avatar, "复制报错", pendingIntent);

			//发送通知
			NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
			notificationManager.notify(ID, builder.build());
		} catch (Exception e) {
			//获取剪贴板管理器：
			ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			// 创建普通字符型ClipData
			ClipData mClipData = ClipData.newPlainText("Label", e.getMessage());
			// 将ClipData内容放到系统剪贴板里。
			cm.setPrimaryClip(mClipData);
		}
	}

	public static void ordinary(Context context, String Title, String textContent, int ID, PendingIntent pendingIntent, boolean Delete) {
		//创建通知
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "OrdinaryNotification")
				.setSmallIcon(R.drawable.avatar)
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
		//删除通知
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
		notificationManager.cancel(ID);
	}

	public static void NotificationPermission(Activity context) {
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
				/*
				 *检查通知渠道
				 */
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
					//下载通知
					NotificationChannel DownloadChannel = new NotificationChannel("ProgressNotification", "下载进度", NotificationManager.IMPORTANCE_MIN);
					DownloadChannel.enableVibration(false);
					DownloadChannel.setImportance(NotificationManager.IMPORTANCE_MIN);
					DownloadChannel.setSound(null, null);
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
	}

}

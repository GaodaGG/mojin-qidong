package com.mojin.qidong.function.controlEvent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.mojin.qidong.data.DownloadInfo;
import com.mojin.qidong.function.download.DownloadFile;

import org.ppsspp.ppsspp.PpssppActivity;

import java.io.File;

public class HomeEvent {
	@SuppressLint("StaticFieldLeak")
	private static Activity mActivity;

	public static void init(Activity activity) {
		mActivity = activity;
	}

	public static void buttonEvent() {
		DownloadInfo downloadInfo = new DownloadInfo()
			.setCloudPath("/GG/PSP魔禁相关/游戏本体相关/魔法禁书目录.iso")
			.setName("游戏本体")
			.setPendingIntent(null)
			.setNotificationID(1)
			.setFile(new File(mActivity.getExternalFilesDir("PSP/GAME") + "/魔法禁书目录.iso"));
		new DownloadFile(mActivity).startDownload(downloadInfo);
	}

	public static void buttonEvent2() {
		String path = "/storage/emulated/0/Android/data/com.mojin.qidong/files/PSP/GAME/魔法禁书目录.iso";//指定的文件位置
		Intent intent = new Intent(mActivity, PpssppActivity.class);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri = Uri.parse(path);
		intent.setDataAndType(uri, "*/*");
		mActivity.startActivity(intent);
	}
}

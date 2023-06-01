package com.mojin.qidong.function.controlEvent;

import android.app.Activity;
import android.os.Environment;

import com.mojin.qidong.data.DownloadInfo;
import com.mojin.qidong.function.download.DownloadFile;

import java.io.File;

public class HomeEvent {
	private static Activity mActivity;

	public static void init(Activity activity) {
		mActivity = activity;
	}

	public static void buttonEvent() {
		DownloadInfo downloadInfo = new DownloadInfo()
			.setCloudPath("/GG/PSP魔禁相关/游戏本体相关/[手机]魔禁启动姬_4.3.0.apk")
			.setName("魔禁启动姬")
			.setPendingIntent(null)
			.setNotificationID(1)
			.setFile(new File(Environment.getExternalStorageDirectory().getPath() + "/test.apk"));
		new DownloadFile(mActivity).startDownload(downloadInfo);
	}
}

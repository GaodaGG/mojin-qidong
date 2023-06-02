package com.mojin.qidong.function.controlEvent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Environment;

import com.mojin.qidong.data.DownloadInfo;
import com.mojin.qidong.function.download.DownloadFile;

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
			.setFile(new File(mActivity.getExternalFilesDir("Download") + "/test.iso"));
		new DownloadFile(mActivity).startDownload(downloadInfo);
	}
}

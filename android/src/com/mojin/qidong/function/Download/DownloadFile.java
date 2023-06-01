package com.mojin.qidong.function.Download;

import android.app.Activity;

import com.alibaba.fastjson.JSONObject;
import com.hjq.http.EasyHttp;
import com.hjq.http.lifecycle.ApplicationLifecycle;
import com.hjq.http.listener.OnDownloadListener;
import com.mojin.qidong.data.DownloadInfo;
import com.mojin.qidong.function.Log;
import com.mojin.qidong.function.Notification.AppNotification;

import java.io.File;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DownloadFile {
	public String api = "http://106.53.148.51:3100/api/fs/get";
	/** @noinspection FieldMayBeFinal*/
	private Activity mActivity;

	public DownloadFile(Activity activity) {
		mActivity = activity;
	}

	public void startDownload(DownloadInfo downloadInfo) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				EasyHttp.download(ApplicationLifecycle.getInstance())
					.url(getDownloadURL(getDownloadJSON(downloadInfo.getCloudPath())))
					.file(downloadInfo.getFile())
					.listener(new OnDownloadListener() {
						String totalByte = "0";
						String downloadByte = "0";

						@Override
						public void onStart(File file) {
							AppNotification.download(mActivity, downloadInfo.getName(), "0%", downloadInfo.getNotificationID());
						}

						@Override
						public void onProgress(File file, int progress) {
							AppNotification.update(mActivity, downloadInfo.getName(), downloadByte + "/" + totalByte + " ● " + progress + "%", downloadInfo.getNotificationID(), progress);
						}

						@Override
						public void onByte(File file, long totalByte, long downloadByte) {
							this.totalByte = b2mb(totalByte);
							this.downloadByte = b2mb(downloadByte);
						}

						@Override
						public void onComplete(File file) {
							AppNotification.ordinary(mActivity, downloadInfo.getName(), "下载成功", downloadInfo.getNotificationID(), downloadInfo.getPendingIntent(), false);
						}

						@Override
						public void onError(File file, Exception e) {
							AppNotification.ordinary(mActivity, downloadInfo.getName(), "下载失败：" + e.getMessage(), downloadInfo.getNotificationID(), downloadInfo.getPendingIntent(), false);
						}

						@Override
						public void onEnd(File file) {
							AppNotification.ordinary(mActivity, downloadInfo.getName(), "下载成功", downloadInfo.getNotificationID(), downloadInfo.getPendingIntent(), false);
						}
					}).start();
			}
		}).start();
	}

	private String getDownloadJSON(String path) {
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
			assert response.body() != null;
			return response.body().string();
		} catch (Exception e) {
			Log.sendLog(mActivity, e);
			return null;
		}
	}

	private String getDownloadURL(String apiJson) {
		JSONObject json = JSONObject.parseObject(apiJson);
		json = JSONObject.parseObject(json.getString("data"));
		return json.getString("raw_url");
	}

	private String b2mb(long length) {
		long kb = length / 1024;
		if (kb >= 1024) {
			long mb = kb / 1024;
			return mb + "MB";
		} else {
			return kb + "KB";
		}
	}
}

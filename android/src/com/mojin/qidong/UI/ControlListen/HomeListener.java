package com.mojin.qidong.UI.ControlListen;

import android.app.Activity;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import com.alibaba.fastjson.JSONObject;
import com.hjq.http.EasyHttp;
import com.hjq.http.lifecycle.ApplicationLifecycle;
import com.hjq.http.listener.OnDownloadListener;
import com.hjq.http.model.HttpMethod;
import com.mojin.qidong.R;
import com.mojin.qidong.function.Log;
import com.mojin.qidong.function.Notification.AppNotification;

import java.io.File;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeListener {
	private static Activity activity;

	public HomeListener(Activity activity) {
		HomeListener.activity = activity;
	}

	public static String url(String path) {
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
			Log.sendLog(activity, e);
			return null;
		}
	}

	public void start() {
		Button downloadButton = activity.findViewById(R.id.downloadButton);
		downloadButton.setOnClickListener(v -> {
			new Thread(new Runnable() {
				@Override
				public void run() {
					final String ApiJson = url("/GG/PSP魔禁相关/游戏本体相关/[手机]魔禁启动姬_4.3.0.apk");
					JSONObject Json = JSONObject.parseObject(ApiJson);
					Json = JSONObject.parseObject(Json.getString("data"));
					final String DownloadURL = Json.getString("raw_url");
					AppNotification.error(activity, DownloadURL);
					EasyHttp.download(ApplicationLifecycle.getInstance())
						.method(HttpMethod.GET)
						.url(DownloadURL)
						.file(new File(Environment.getExternalStorageDirectory().getPath() + "test.apk"))
						.listener(new OnDownloadListener() {
							String length = "0";

							@Override
							public void onStart(File file) {
								AppNotification.download(activity, "这里是标题", "0%", 10000);
							}

							@Override
							public void onProgress(File file, int progress) {
								AppNotification.download(activity, "这里是标题", length + " ● " + progress + "%", 10000);
							}

							@Override
							public void onByte(File file, long totalByte, long downloadByte) {
								length = b2mb(totalByte);
							}

							@Override
							public void onComplete(File file) {
								AppNotification.ordinary(activity, "这里是标题", "下载成功，可能是网络问题", 10000, null, false);
							}

							@Override
							public void onError(File file, Exception e) {
								AppNotification.ordinary(activity, "这里是标题", "下载失败" + e.getMessage(), 10000, null, false);
							}

							@Override
							public void onEnd(File file) {

							}
						});
				}
			}).start();
		});
	}

	public String b2mb(long length) {
		long kb = length / 1024;
		if (kb >= 1024) {
			long mb = kb / 1024;
			return mb + "MB";
		} else {
			return kb + "KB";
		}
	}
}

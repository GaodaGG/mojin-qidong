package com.mojin.qidong;

import android.app.Application;

import com.hjq.http.EasyConfig;
import com.mojin.qidong.function.ToolsWindow;
import com.mojin.qidong.function.download.RequestHandler;

import okhttp3.OkHttpClient;

public class MainApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		//初始化网络框架
		OkHttpClient okHttpClient = new OkHttpClient.Builder()
			.build();

		EasyConfig.with(okHttpClient)
			.setLogEnabled(true)
			.setServer("http://106.53.418.51:3100/api")
			.setHandler(new RequestHandler(this))
			// 设置请求重试次数
			.setRetryCount(3)
			.into();
	}

}

package com.mojin.qidong;

import android.app.Application;

import com.crash.crash_lib.CrashHandleUtil;

public class MainApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		CrashHandleUtil.init(this);
	}
}

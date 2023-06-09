package com.mojin.qidong.function.setting;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class SettingInfo {
	private final SharedPreferences mSharedPcreferences;
	private static final String[] mKey = {"appUpdate", "autoWindow", "networkChange", "networkPassword"};
	private static final Object[] mKeyDefault = {true, false, "御坂网络", "default"};



	public SharedPreferences getSharedPcreferences() {
		return mSharedPcreferences;
	}

	public String[] getKey(){
		return mKey;
	}

	public Object[] getKeyDefault(){
		return mKeyDefault;
	}

	public SettingInfo(Activity activity) {
		mSharedPcreferences = activity.getSharedPreferences("Setting", Context.MODE_PRIVATE);
	}
}

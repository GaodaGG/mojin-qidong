package com.mojin.qidong.function.setting;

import android.content.SharedPreferences;

public class SettingUtil {
	private static SharedPreferences mSharedPcreferences;

	public static void init(SettingInfo settingInfo){
		mSharedPcreferences = settingInfo.getSharedPcreferences();
	}

	public static String getSettingMessage(String key){
		return mSharedPcreferences.getString(key, "获取失败");
	}

	public static void addSettingMessage(String key, String message){
		mSharedPcreferences.edit().putString(key, message).apply();
	}
}

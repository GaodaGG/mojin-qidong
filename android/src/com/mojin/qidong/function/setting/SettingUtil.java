package com.mojin.qidong.function.setting;

import android.content.SharedPreferences;

public class SettingUtil {
	private static SharedPreferences mSharedPcreferences;

	public static void init(SettingInfo settingInfo){
		mSharedPcreferences = settingInfo.getSharedPcreferences();
	}

	public static String getSettingMessage(String key){
		Object message = mSharedPcreferences.getAll().get(key);
		if (message == null){
			return "获取失败";
		} else {
			return message.toString();
		}
	}

	public static void addSettingMessage(String key, Object message){
		if (message instanceof Boolean) {
			mSharedPcreferences.edit().putBoolean(key, Boolean.parseBoolean(message.toString())).apply();
		} else {
			mSharedPcreferences.edit().putString(key, message.toString()).apply();
		}
	}
}

package com.mojin.qidong.function;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.mojin.qidong.function.setting.SettingInfo;
import com.mojin.qidong.function.setting.SettingUtil;

import org.ppsspp.ppsspp.PpssppActivity;

public class FirstRun {
	private final Activity mActivity;

	private SharedPreferences msharedPreferences;

	public FirstRun(Activity activity){
		mActivity = activity;
	}

	public void start(){
		try {
			msharedPreferences = mActivity.getSharedPreferences("Setting", Context.MODE_PRIVATE);
			initPPSSPP();
			initSetting();
		} catch (Exception e) {
			Log.sendLog(mActivity, e);
		}
	}

	private void initPPSSPP() {
		if (msharedPreferences.getBoolean("FirstRun",true)){
			msharedPreferences.edit().putBoolean("FirstRun",false).apply();
			Toast.makeText(mActivity,"生成模拟器配置文件中",Toast.LENGTH_LONG).show();

			//打开ppsspp使其生成配置文件
			Intent intent = new Intent(mActivity, PpssppActivity.class);
			mActivity.startActivity(intent);
		}
	}

	private void initSetting(){
		SettingInfo settingInfo = new SettingInfo(mActivity);
		String[] key = settingInfo.getKey();
		Object[] keyDefault = settingInfo.getKeyDefault();
		for (int i = 0; i < key.length ; i++) {
			SettingUtil.addSettingMessage(key[i], keyDefault[i]);
		}
	}
}

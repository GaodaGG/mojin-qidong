package com.mojin.qidong.function;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.ppsspp.ppsspp.PpssppActivity;

public class FirstRun {
	private final Activity mActivity;

	private SharedPreferences msharedPreferences;

	public FirstRun(Activity activity){
		mActivity = activity;
	}

	public void start(){
		try {
			initPPSSPP();
			initSetting();
		} catch (Exception e) {
			Log.sendLog(mActivity, e);
		}
	}

	private void initPPSSPP() {
		msharedPreferences = mActivity.getSharedPreferences("Setting", Context.MODE_PRIVATE);
		if (msharedPreferences.getBoolean("FirstRun",true)){
			msharedPreferences.edit().putBoolean("FirstRun",false).apply();
			Toast.makeText(mActivity,"生成模拟器配置文件中",Toast.LENGTH_LONG).show();

			//打开ppsspp使其生成配置文件
			Intent intent = new Intent(mActivity, PpssppActivity.class);
			mActivity.startActivity(intent);
		}
	}

	private void initSetting(){

	}
}

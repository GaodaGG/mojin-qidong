package com.mojin.qidong.function;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


/**
 * 系统处理异常类，处理整个APP的异常
 */
public class CrashExceptionHandler extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		//Toast.makeText(context, intent.getStringExtra("errorText"), Toast.LENGTH_SHORT).show();
		if (intent.getStringExtra("errorText") == null){
			Toast.makeText(context, "悲", Toast.LENGTH_SHORT).show();
		}
		//获取剪贴板管理器
		ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		//创建普通字符型ClipData
		ClipData mClipData = ClipData.newPlainText("Label", intent.getStringExtra("errorText"));
		//将ClipData内容放到系统剪贴板里
		cm.setPrimaryClip(mClipData);
		Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show();
	}
}


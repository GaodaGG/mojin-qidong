package com.mojin.qidong;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.mojin.qidong.function.AppNotification;

import org.ppsspp.ppsspp.PpssppActivity;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

			//状态栏相关
			ImmersionBar.with(this)
					.hideBar(BarHide.FLAG_HIDE_BAR)
					.init();
			int b = 1/0;
			AppNotification.NotificationPermission(this);


		} catch (Exception e) {
			String error = Log(e);
			AppNotification.error(this, error);
		}

		LinearLayout start = findViewById(R.id.Activitymain_LinearLayout);
		start.setOnClickListener(v -> {
			String path = "/storage/emulated/0/Android/data/com.mojin.qidong/files/PSP/GAME/魔法禁书目录.iso";//指定的文件位置
			Intent intent = new Intent(this, PpssppActivity.class);
			intent.addCategory(Intent.CATEGORY_DEFAULT);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			Uri uri = Uri.parse(path);


			intent.setDataAndType(uri, "*/*");
			startActivity(intent);
			finish();
		});
	}

	public static String Log(Throwable e) {
		if (e != null) {
			StackTraceElement[] stackTrace = e.getStackTrace();
			int lineNumber = stackTrace[0].getLineNumber();
			String methodName = stackTrace[0].getMethodName();

			return "报错原因：" + e.getLocalizedMessage() + "\n"
					+ "所在类：" + stackTrace[0].getClassName() + "\n"
					+ "所在方法：" + methodName + "\n"
					+ "所在行：" + lineNumber;
		}
		return "没有获取到报错";
	}
}

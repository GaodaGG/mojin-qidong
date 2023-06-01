package com.mojin.qidong.ui.layout;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Toast;

import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;

public class BaseActivity extends Activity {
	private long firstBackTime;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//应用横屏
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		//状态栏相关
		ImmersionBar.with(this)
			.hideBar(BarHide.FLAG_HIDE_BAR)
			.init();
	}

	@Override
	public void onBackPressed() {
		if (System.currentTimeMillis() - firstBackTime > 2000) {
			Toast.makeText(this, "再按一次返回键退出程序", Toast.LENGTH_SHORT).show();
			firstBackTime = System.currentTimeMillis();
			return;
		}

		super.onBackPressed();
	}
}

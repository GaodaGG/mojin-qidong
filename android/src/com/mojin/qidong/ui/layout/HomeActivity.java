package com.mojin.qidong.ui.layout;

import android.os.Bundle;

import com.mojin.qidong.R;
import com.mojin.qidong.ui.controlListen.HomeListener;

public class HomeActivity extends BaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);

		//监听控件
		new HomeListener(this)
			.start();
	}
}

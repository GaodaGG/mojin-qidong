package com.mojin.qidong.ui.layout;

import static com.mojin.qidong.function.Log.sendLog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.mojin.qidong.R;
import com.mojin.qidong.function.FirstRun;
import com.mojin.qidong.function.notification.AppNotification;

import java.util.List;

public class MainActivity extends BaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);

			/*
			 * 权限申请
			 */
			XXPermissions.with(this)
				.permission(Permission.MANAGE_EXTERNAL_STORAGE)
				.request(new OnPermissionCallback() {
					@Override
					public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
					}

					@Override
					public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
						// 如果是被永久拒绝就跳转到应用权限系统设置页面
						if (doNotAskAgain) {
							Toast.makeText(MainActivity.this, "启动姬被永久拒绝授权，请手动授权存储权限", Toast.LENGTH_SHORT).show();
							XXPermissions.startPermissionActivity(MainActivity.this, permissions);
						} else {
							Toast.makeText(MainActivity.this, "获取存储权限失败", Toast.LENGTH_SHORT).show();
						}
					}
				});

			//创建通知通道
			AppNotification.NotificationPermission(this);

			//第一次进入应用
			new FirstRun(this).start();

		} catch (Exception e) {
			sendLog(this, e);
		}

		LinearLayout start = findViewById(R.id.Activitymain_LinearLayout);
		start.setOnClickListener(v -> {

			Intent intent = new Intent(this, HomeActivity.class);
			startActivity(intent);
		});
	}

	public void onBackPressed() {
		super.onBackPressed();
	}
}

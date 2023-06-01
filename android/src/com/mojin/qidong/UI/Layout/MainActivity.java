package com.mojin.qidong.UI.Layout;

import static com.mojin.qidong.function.Log.sendLog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hjq.http.EasyConfig;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.mojin.qidong.R;
import com.mojin.qidong.function.Notification.AppNotification;

import org.ppsspp.ppsspp.PpssppActivity;

import java.util.List;

import okhttp3.OkHttpClient;

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
				.permission(Permission.Group.STORAGE)
				.request(new OnPermissionCallback() {
					@Override
					public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
//						if(allGranted){
//							Toast.makeText(MainActivity.this, "恭喜，您已将权限全部授权", Toast.LENGTH_SHORT).show();
//						}
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


//			//初始化网络框架
//			OkHttpClient okHttpClient = new OkHttpClient.Builder()
//				.build();
//
//			EasyConfig.with(okHttpClient)
//				.setLogEnabled(true)
//				.
		} catch (Exception e) {
			sendLog(this, e);
		}


		LinearLayout start = findViewById(R.id.Activitymain_LinearLayout);
		start.setOnClickListener(v -> {
//			String path = "/storage/emulated/0/Android/data/com.mojin.qidong/files/PSP/GAME/魔法禁书目录.iso";//指定的文件位置
//			Intent intent = new Intent(this, PpssppActivity.class);
//			intent.addCategory(Intent.CATEGORY_DEFAULT);
//			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			Uri uri = Uri.parse(path);
//			intent.setDataAndType(uri, "*/*");
//			//startActivity(intent);
//			//finish();
			Intent intent = new Intent(this, HomeActivity.class);
			startActivity(intent);
		});
	}

	public void onBackPressed(){
		super.onBackPressed();
	}
}

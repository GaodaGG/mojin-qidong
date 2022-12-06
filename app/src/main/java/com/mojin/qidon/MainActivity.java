package com.mojin.qidon;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.zhangyue.we.x2c.X2C;
import com.zhangyue.we.x2c.ano.Xml;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, 
        Manifest.permission.READ_MEDIA_IMAGES, 
        Manifest.permission.READ_MEDIA_AUDIO, 
        Manifest.permission.READ_MEDIA_VIDEO, 
        Manifest.permission.POST_NOTIFICATIONS};

    List<String> mPermissionList = new ArrayList<>();
    private static final int PERMISSION_REQUEST = 1;
    Boolean frequency = false;

    //载入事件
    @Override
    @Xml(layouts = "activity_main")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        X2C.setContentView(this, R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //状态栏相关
        ImmersionBar.with(this)
            .hideBar(BarHide.FLAG_HIDE_BAR)
            .init();

        //调用检测权限
        checkPermission();
        super.onCreate(savedInstanceState);

        //检查是否有所有文件权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (!Environment.isExternalStorageManager()) { //判断是否获取到“允许管理所有文件”权限
                //请求“允许管理所有文件”权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_REQUEST);
                Toast.makeText(this, "请给予权限", Toast.LENGTH_SHORT).show();
            }
        }

        //开始代码
        try {
            StartUpMap.StatUpMap(this);
        } catch (Exception e) {
            AppNotification.error(this, ErrorGet.Log(e));
        }

    }

    //载入完毕事件
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (frequency != true) {
            frequency = true;
            //跳转首页
            try {
                Thread.currentThread().sleep(2000);
                Intent it = new Intent(MainActivity.this, FrontPage.class);
                startActivity(it);
                finish();
            } catch (Exception e) {
                AppNotification.error(this, ErrorGet.Log(e));
            }
        }
    }

    //检查权限
    private void checkPermission() {
        mPermissionList.clear();

        //判断哪些权限未授予
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }

        /**
         * 判断是否为空
         */
        if (!mPermissionList.isEmpty()) {//未授予的权限为空，表示都授予了
            //请求权限方法
            String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_REQUEST);
            //SDK33以下申请通知权限
            if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) {
                NotificationPermission(this);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        /*
         *判断权限是否给予
         */
        if (requestCode == PERMISSION_REQUEST) {
            if (!(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(getApplication(), "请在设置中给予应用权限", Toast.LENGTH_SHORT).show();
                    finish();
            }
        }
    }

    /*
     *获取通知权限
     */
    private static void NotificationPermission(Activity context) {
        //检测通知权限
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            try {
                NotificationManagerCompat manager = NotificationManagerCompat.from(context);
                // areNotificationsEnabled方法的有效性官方只最低支持到API 19，低于19的仍可调用此方法不过只会返回true，即默认为用户已经开启了通知。
                boolean isOpened = manager.areNotificationsEnabled();
                if (!isOpened) {
                    Toast.makeText(context, "检测到没有给予通知权限，请先给予权限", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    if (Build.VERSION.SDK_INT >= 26) {
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                        intent.putExtra(Notification.EXTRA_CHANNEL_ID, context.getApplicationInfo().uid);
                    } else if (Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT <= 25) {
                        intent.putExtra("app_package", context.getPackageName());
                        intent.putExtra("app_uid", context.getApplicationInfo().uid);
                    }
                    context.startActivity(intent);
                }
            } catch (Exception e) {
                Intent intent = new Intent();
                //下面这种方案是直接跳转到当前应用的设置界面。
                //https://blog.csdn.net/ysy950803/article/details/71910806
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
            }
        }
    }
}

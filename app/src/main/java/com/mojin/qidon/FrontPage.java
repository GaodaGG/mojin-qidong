package com.mojin.qidon;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.lzf.easyfloat.EasyFloat;
import com.mojin.qidon.function.GamePackage;
import com.sd.lib.switchbutton.SwitchButton;
import com.zhangyue.we.x2c.X2C;
import com.zhangyue.we.x2c.ano.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrontPage extends Activity {
    private long firstBackTime;
    private String LayoutView = "WindowMain";

    @Override
    @Xml(layouts = "froutlpage_ui")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        X2C.setContentView(this, R.layout.froutpage_ui);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        //状态栏相关
        ImmersionBar.with(this)
            .hideBar(BarHide.FLAG_HIDE_BAR)
            .init();

        //动态加载布局
        LinearLayout Window = (LinearLayout)findViewById(R.id.FroutPageUI_Window);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.windowsmain, null);
        Window.addView(view);
    }

    @Override
    @Xml(layouts = "FroutPage_UI")
    public void onBackPressed() {
        if (System.currentTimeMillis() - firstBackTime > 2000) {
            Toast.makeText(this, "再按一次返回键退出程序", Toast.LENGTH_SHORT).show();
            firstBackTime = System.currentTimeMillis();
            return;
        }

        super.onBackPressed();
    }

    //加载完毕事件
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!((MainApplication)getApplication()).onWindowEnd) {
            try {
                //获取设置内容
                FileInputStream in = new FileInputStream(new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini"));
                byte Inputbyt[] = new byte[1024];
                int len = in.read(Inputbyt);
                String SetUpText = new String(Inputbyt, 0, len);
                in.close();

                Pattern UpdateRegular = Pattern.compile("自动更新 = .*");
                Matcher Updatematcher = UpdateRegular.matcher(SetUpText);
                Updatematcher.find();
                String UpdateChecked = Updatematcher.group(0);

                if (UpdateChecked.equals("自动更新 = True")) {
                    //检测更新
                    ApkUpdate.update(this);
                }
                
                //疯狂星期四
                Calendar calendar=Calendar.getInstance();
                if(calendar.get(Calendar.DAY_OF_WEEK) == 5){
                    AppNotification.ordinary(this, "疯狂星期四", "今天是肯德基疯狂星期四，可我却因为没钱吃饭而写不出代码，v我50助我写出更好的启动姬", 114514, null, false);
                }
                
            } catch (Exception e) {
                AppNotification.error(this, ErrorGet.Log(e));
            }
        }
        ((MainApplication)getApplication()).onWindowEnd = true;
    }


    //画质设置
    public void QualitySettings(View view) {
        //动态加载布局
        try {
            if (!LayoutView.equals("QualitySettings")) {
                LinearLayout Window = (LinearLayout)findViewById(R.id.FroutPageUI_Window);
                LayoutInflater inflater = getLayoutInflater();
                View WindowView = inflater.inflate(R.layout.qualitysettings, null);
                Window.removeAllViews();
                Window.addView(WindowView);

                //加载动画
                Animation startAnimation = AnimationUtils.loadAnimation(this, R.anim.switch_page_start);
                Window.startAnimation(startAnimation);

                //单选相关
                SwitchButton LowButton = findViewById(R.id.low_quality);
                SwitchButton MediumButton = findViewById(R.id.medium_quality);
                SwitchButton HighButton = findViewById(R.id.high_quality);
                SwitchButtons.QualitySettings(LowButton, MediumButton, HighButton);

                LayoutView = "QualitySettings";
            }

            //震动
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(100);
        } catch (Exception e) {
            AppNotification.error(this, ErrorGet.Log(e));
        }
    }

    //一键安装
    public void GameDownload(View view) {
        if (!LayoutView.equals("GameDownload")) {
            //动态加载布局
            LinearLayout Window = (LinearLayout)findViewById(R.id.FroutPageUI_Window);
            LayoutInflater inflater = getLayoutInflater();
            View WindowView = inflater.inflate(R.layout.downloadgame, null);
            Window.removeAllViews();
            Window.addView(WindowView);

            //加载动画
            Animation startAnimation = AnimationUtils.loadAnimation(this, R.anim.switch_page_start);
            Window.startAnimation(startAnimation);

            LayoutView = "GameDownload";
        }

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);

    }

    //窗口首页
    public void WindowMain(View view) {
        if (!LayoutView.equals("WindowMain")) {
            //动态加载布局
            LinearLayout Window = (LinearLayout)findViewById(R.id.FroutPageUI_Window);
            LayoutInflater inflater = getLayoutInflater();
            View WindowView = inflater.inflate(R.layout.windowsmain, null);
            Window.removeAllViews();
            Window.addView(WindowView);

            //加载动画
            Animation startAnimation = AnimationUtils.loadAnimation(this, R.anim.switch_page_start);
            Window.startAnimation(startAnimation);

            LayoutView = "WindowMain";
        }

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    //一键卸载
    public void GameDelete(View view) {
        if (!LayoutView.equals("GameDelete")) {
            //动态加载布局
            LinearLayout Window = (LinearLayout)findViewById(R.id.FroutPageUI_Window);
            LayoutInflater inflater = getLayoutInflater();
            View WindowView = inflater.inflate(R.layout.deletegame, null);
            Window.removeAllViews();
            Window.addView(WindowView);

            //加载动画
            Animation startAnimation = AnimationUtils.loadAnimation(this, R.anim.switch_page_start);
            Window.startAnimation(startAnimation);

            LayoutView = "GameDelete";
        }

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    //材质包
    public void GamePackage(View view) {
        if (!LayoutView.equals("GamePackage")) {
            //动态加载布局
            LinearLayout Window = (LinearLayout)findViewById(R.id.FroutPageUI_Window);
            LayoutInflater inflater = getLayoutInflater();
            View WindowView = inflater.inflate(R.layout.gamepackage_ui, null);
            Window.removeAllViews();
            Window.addView(WindowView);

            //加载动画
            Animation startAnimation = AnimationUtils.loadAnimation(this, R.anim.switch_page_start);
            Window.startAnimation(startAnimation);

            LayoutView = "GamePackage";
        }

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    /*
     *画质设置相关
     */
    public void QualitySettingsButton(View view) {
        SwitchButton LowButton = findViewById(R.id.low_quality);
        SwitchButton MediumButton = findViewById(R.id.medium_quality);
        SwitchButton HighButton = findViewById(R.id.high_quality);
        QualitySettings.QualitySettings(LowButton, MediumButton, HighButton, this);


        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    /*
     *材质包相关
     */
    public void GamePackageButton(View view) {
        SwitchButton SuperGamePackage = findViewById(R.id.SuperGamePackage);
        SwitchButton StoryGamePackage = findViewById(R.id.StoryGamePackage);
        SwitchButton ModelGamePackage = findViewById(R.id.ModelGamePackage);
       // GamePackage.GamePackage(SuperGamePackage, StoryGamePackage, ModelGamePackage, this);

        GamePackage gamePackage =new GamePackage(SuperGamePackage,StoryGamePackage,ModelGamePackage,this);
        gamePackage.start();
        
        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    /*
     *一键下载相关
     */
    public void GameDownloadButton(View view) {
        SwitchButton GameIso = findViewById(R.id.Game_ISO);
        SwitchButton MisakaFcn = findViewById(R.id.Misaka_FCN);
        SwitchButton GamePPSSPP = findViewById(R.id.GamePPSSPP);
        SwitchButton GameSave = findViewById(R.id.GameSave);
        GameDownload.GameDownload(GameIso, MisakaFcn, GamePPSSPP, GameSave, this);

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    /*
     *一键卸载相关
     */
    public void GameDeleteButton(View view) {
        SwitchButton GameIso = findViewById(R.id.Delete_Game_ISO);
        SwitchButton MisakaFcn = findViewById(R.id.Delete_Misaka_FCN);
        SwitchButton GamePPSSPP = findViewById(R.id.Delete_GamePPSSPP);
        SwitchButton GameSave = findViewById(R.id.Delete_GameSave);
        GameDelete.GameDelete(GameIso, MisakaFcn, GamePPSSPP, GameSave, this);

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    /*
     *一键优化相关
     */
    public void PPSSPPSetting(View view) {
        boolean TOF = StartGame.PPSSPPSetting(this);
        if (TOF) {
            Toast.makeText(this, "是不是很能干呢(疯狂暗示)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "似乎出了点问题", Toast.LENGTH_SHORT).show();
        }

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    /*
     *悬浮窗相关
     */
    public void FloatingWindow(View view) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "让我看看…悬浮窗权限好像没开？！开好了就返回启动姬吧", Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
            } else {
                if (!EasyFloat.isShow("PingFloat")) {
                    PingFloatingWindow.start(this);
                } else {
                    PingFloatingWindow.stop(this);
                }
            }
        }

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    /*
     *开始游戏相关
     */
    public void StartGame(View view) {
        StartGame.startGame(this);

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    /*
     *检查更新相关
     */
    public void UpdateApk(View view) {
        ApkUpdate.update(this);

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    /*
     *设置相关 
     */
    public void AppSetting(View view) {
        //返回首页
        try {
            Intent it = new Intent(this, AppSettingActivity.class);
            this.startActivity(it);
            this.finish();
        } catch (Exception e) {
            AppNotification.error(this, ErrorGet.Log(e));
        }

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

}



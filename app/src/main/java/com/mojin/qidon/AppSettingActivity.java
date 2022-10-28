package com.mojin.qidon;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.gyf.immersionbar.BarHide;
import com.gyf.immersionbar.ImmersionBar;
import com.sd.lib.switchbutton.SwitchButton;
import com.zhangyue.we.x2c.X2C;
import com.zhangyue.we.x2c.ano.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppSettingActivity extends Activity {
    private boolean OnWindowEnd = false;

    @Override
    @Xml(layouts = "appsettinglayout")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        X2C.setContentView(this, R.layout.appsettinglayout);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        //状态栏相关
        ImmersionBar.with(this)
            .hideBar(BarHide.FLAG_HIDE_BAR)
            .init();

        //显示设置内容
        show(this);
    }

    @Override
    public void onBackPressed() {
        //返回首页
        try {
            Intent it = new Intent(this, FrontPage.class);
            this.startActivity(it);
            this.finish();
        } catch (Exception e) {
            AppNotification.error(this, ErrorGet.Log(e));
        }

        super.onBackPressed();
    }

    //返回
    public void BackFrontPage(View v) {
        //返回首页
        try {
            Intent it = new Intent(this, FrontPage.class);
            this.startActivity(it);
            this.finish();
        } catch (Exception e) {
            AppNotification.error(this, ErrorGet.Log(e));
        }

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    //立即应用
    public void AppSettingUse(View view) {
        //写入新设置
        SetUp(this);

        //Toast提示
        Toast.makeText(this, "应用成功", Toast.LENGTH_SHORT).show();

        //震动
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }

    //显示设置
    private static void show(final Activity context) {
        try {
            //获取设置内容
            FileInputStream in = new FileInputStream(new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini"));
            byte Inputbyt[] = new byte[1024];
            int len = in.read(Inputbyt);
            String SetUpText = new String(Inputbyt, 0, len);
            in.close();

            //获取模拟器设置内容
            File GameSetting = new File("/storage/emulated/0/PSP/SYSTEM/ULJS00329_ppsspp.ini");
            File PPSSPPSetting = new File("/storage/emulated/0/PSP/SYSTEM/ppsspp.ini");
            String PSPSetUpText;
            if (GameSetting.exists()) {
                FileInputStream PSPin = new FileInputStream(GameSetting);
                byte[] PSPInputbyt = Files.readAllBytes(GameSetting.toPath());
                int PSPlen = PSPin.read(PSPInputbyt);
                PSPSetUpText = new String(PSPInputbyt, 0, PSPlen);
                PSPin.close();
            } else {
                FileInputStream PSPin = new FileInputStream(PPSSPPSetting);
                byte[] PSPInputbyt = Files.readAllBytes(PPSSPPSetting.toPath());
                int PSPlen = in.read(PSPInputbyt);
                PSPSetUpText = new String(PSPInputbyt, 0, PSPlen);
                PSPin.close();
            }

            /*
             *功能性
             */
            //打开游戏自动打开悬浮窗
            Pattern ServerRegular = Pattern.compile("自动打开悬浮窗 = .*");
            Matcher Servermatcher = ServerRegular.matcher(SetUpText);
            Servermatcher.find();
            String ServerChecked = Servermatcher.group(0);

            if (ServerChecked.equals("自动打开悬浮窗 = True")) {
                SwitchButton ButtonA = context.findViewById(R.id.APPSettingLayout_A);
                ButtonA.setChecked(true, false, true);
            }

            //启动自动检测更新
            Pattern UpdateRegular = Pattern.compile("自动更新 = .*");
            Matcher Updatematcher = UpdateRegular.matcher(SetUpText);
            Updatematcher.find();
            String UpdateChecked = Updatematcher.group(0);

            if (UpdateChecked.equals("自动更新 = True")) {
                SwitchButton ButtonB = context.findViewById(R.id.APPSettingLayout_B);
                ButtonB.setChecked(true, false, true);
            }

            /*
             *御坂网络
             */

            //密码
            Pattern PassWordRegular = Pattern.compile("密码 = .*");
            Matcher PassWordMatcher = PassWordRegular.matcher(SetUpText);
            PassWordMatcher.find();
            String Password = PassWordMatcher.group(0);
            String PasswordA[] = Password.split("= ");
            EditText PassWordView = context.findViewById(R.id.APPSettingLayoutPassword);
            PassWordView.setText(PasswordA[1]);

            //游戏昵称
            Pattern NameRegular = Pattern.compile("NickName = .*");
            Matcher Namematcher = NameRegular.matcher(PSPSetUpText);
            Namematcher.find();
            String GameName = Namematcher.group(0);
            String GameNameA[] = GameName.split("= ");
            EditText GameNameView = context.findViewById(R.id.APPSettingLayoutUserName);
            GameNameView.setText(GameNameA[1]);

            //御坂网络选择
            Pattern FCNRegular = Pattern.compile("御坂网络选择 = .*");
            Matcher FCNmatcher = FCNRegular.matcher(SetUpText);
            FCNmatcher.find();
            String FCNChecked = FCNmatcher.group(0);

            if (FCNChecked.equals("御坂网络选择 = FCN")) {
                SwitchButton ButtonC = context.findViewById(R.id.APPSettingLayout_C);
                ButtonC.setChecked(true, false, true);
            }
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
        }

    }

    //写入新设置
    private static void SetUp(final Activity context) {
        try {
            //获取设置内容
            FileInputStream in = new FileInputStream(new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini"));
            byte Inputbyt[] = new byte[1024];
            int len = in.read(Inputbyt);
            String SetUpText = new String(Inputbyt, 0, len);
            in.close();

            //设置输出流
            FileOutputStream out = new FileOutputStream(new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini"));
            FileOutputStream PSPOut;

            //获取模拟器设置内容
            File GameSetting = new File("/storage/emulated/0/PSP/SYSTEM/ULJS00329_ppsspp.ini");
            File PPSSPPSetting = new File("/storage/emulated/0/PSP/SYSTEM/ppsspp.ini");
            String PSPSetUpText;
            if (GameSetting.exists()) {
                FileInputStream PSPin = new FileInputStream(GameSetting);
                byte[] PSPInputbyt = Files.readAllBytes(GameSetting.toPath());
                int PSPlen = PSPin.read(PSPInputbyt);
                PSPSetUpText = new String(PSPInputbyt, 0, PSPlen);
                PSPin.close();
                PSPOut = new FileOutputStream(GameSetting);
            } else {
                FileInputStream PSPin = new FileInputStream(PPSSPPSetting);
                byte[] PSPInputbyt = Files.readAllBytes(PPSSPPSetting.toPath());
                int PSPlen = in.read(PSPInputbyt);
                PSPSetUpText = new String(PSPInputbyt, 0, PSPlen);
                PSPin.close();
                PSPOut = new FileOutputStream(PPSSPPSetting);
            }

            /*
             *功能性
             */
            //打开游戏自动打开悬浮窗
            Pattern ServerRegular = Pattern.compile("自动打开悬浮窗 = .*");
            Matcher Servermatcher = ServerRegular.matcher(SetUpText);
            Servermatcher.find();
            String ServerChecked = Servermatcher.group(0);

            SwitchButton ButtonA = context.findViewById(R.id.APPSettingLayout_A);
            if (ButtonA.isChecked()) {
                SetUpText = SetUpText.replace(ServerChecked, "自动打开悬浮窗 = True");
            } else {
                SetUpText = SetUpText.replace(ServerChecked, "自动打开悬浮窗 = False");
            }

            //启动自动检测更新
            Pattern UpdateRegular = Pattern.compile("自动更新 = .*");
            Matcher Updatematcher = UpdateRegular.matcher(SetUpText);
            Updatematcher.find();
            String UpdateChecked = Updatematcher.group(0);

            SwitchButton ButtonB = context.findViewById(R.id.APPSettingLayout_B);
            if (ButtonB.isChecked()) {
                SetUpText = SetUpText.replace(UpdateChecked, "自动更新 = True");
            } else {
                SetUpText = SetUpText.replace(UpdateChecked, "自动更新 = False");
            }


            /*
             *御坂网络
             */
            //密码
            Pattern PassWordRegular = Pattern.compile("密码 = .*");
            Matcher PassWordMatcher = PassWordRegular.matcher(SetUpText);
            PassWordMatcher.find();
            String Password = PassWordMatcher.group(0);

            EditText PassWordView = context.findViewById(R.id.APPSettingLayoutPassword);
            SetUpText = SetUpText.replace(Password, "密码 = " + PassWordView.getText());

            //游戏昵称
            Pattern NameRegular = Pattern.compile("NickName = .*");
            Matcher Namematcher = NameRegular.matcher(PSPSetUpText);
            Namematcher.find();
            String GameName = Namematcher.group(0);

            EditText GameNameView = context.findViewById(R.id.APPSettingLayoutUserName);
            PSPSetUpText = PSPSetUpText.replace(GameName, "NickName = " + GameNameView.getText());

            //御坂网络选择
            Pattern FCNRegular = Pattern.compile("御坂网络选择 = .*");
            Matcher FCNmatcher = FCNRegular.matcher(SetUpText);
            FCNmatcher.find();
            String FCNChecked = FCNmatcher.group(0);

            SwitchButton ButtonC = context.findViewById(R.id.APPSettingLayout_C);
            if (ButtonC.isChecked()) {
                SetUpText = SetUpText.replace(FCNChecked, "御坂网络选择 = FCN");
            } else {
                SetUpText = SetUpText.replace(FCNChecked, "御坂网络选择 = 御坂网络");
            }

            //输出设置
            byte Outputbuy[] = SetUpText.getBytes();
            out.write(Outputbuy);
            out.close();

            //输出模拟器设置
            byte PSPOutputbuy[] = PSPSetUpText.getBytes();
            PSPOut.write(PSPOutputbuy);
            PSPOut.close();


        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
        }
    }
}

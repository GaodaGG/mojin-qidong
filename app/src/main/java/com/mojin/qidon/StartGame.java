package com.mojin.qidon;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;
import com.sd.lib.switchbutton.SwitchButton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;

public class StartGame {
    //设置文件和目录的变量
    final static String api = "http://106.53.148.51:3100/api/fs/get";
    private static View ServerView;
    private static PopupWindow popupWindow;
    public static String password;
    public static void startGame(final Activity context) {
        try {
            //弹窗相关
            ServerView = context.getLayoutInflater().inflate(R.layout.selectserver, null);
            popupWindow = new PopupWindow(ServerView, -1, -1);
            popupWindow.setClippingEnabled(false);
            popupWindow.showAtLocation(context.findViewById(R.id.StartGameView), Gravity.CENTER, 0, 0);

            //单选相关
            final SwitchButton ServerA = ServerView.findViewById(R.id.ServerA);
            final SwitchButton ServerB = ServerView.findViewById(R.id.ServerB);
            final SwitchButton ServerC = ServerView.findViewById(R.id.ServerC);
            final SwitchButton ServerD = ServerView.findViewById(R.id.ServerD);
            SwitchButtons.SelectServer(ServerA, ServerB, ServerC, ServerD);

            //背景
            ServerView.findViewById(R.id.SelectServerBackGround).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

            ServerView.findViewById(R.id.SelectServerCard).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });

            //返回应用
            ServerView.findViewById(R.id.SelectServerBack).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();

                        //震动
                        Vibrator vibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
                        vibrator.vibrate(100);
                    }
                });

            //开始游戏
            ServerView.findViewById(R.id.SelectServerStart).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            //获取设置内容
                            FileInputStream in = new FileInputStream(new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini"));
                            byte Inputbyt[] = new byte[1024];
                            int len = in.read(Inputbyt);
                            String SetUpText = new String(Inputbyt, 0, len);
                            in.close();
                            //密码
                            Pattern PassWordRegular = Pattern.compile("密码 = .*");
                            Matcher PassWordMatcher = PassWordRegular.matcher(SetUpText);
                            PassWordMatcher.find();
                            String Password = PassWordMatcher.group(0);
                            String PasswordA[] = Password.split("= ");
                            password = PasswordA[1];
                        } catch (IOException e) {}



                        if (!ServerA.isChecked() && !ServerB.isChecked() && !ServerC.isChecked() && !ServerD.isChecked()) {
                            start(context, ServerA, ServerB, ServerC, ServerD);
                        } else if (password.equals("default")) {
                            Toast.makeText(context, "请先到设置修改密码", Toast.LENGTH_SHORT).show();
                        } else {
                            if (!isAccessibilitySettingsOn(context, AccessibilitySampleService.class.getName())) {// 判断服务是否开启
                                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                                Toast.makeText(context, "请选择御坂网络辅助", Toast.LENGTH_SHORT).show();
                                new Thread(new Runnable(){
                                        @Override
                                        public void run() {
                                            while (true) {
                                                if (isAccessibilitySettingsOn(context, AccessibilitySampleService.class.getName())) {
                                                    start(context, ServerA, ServerB, ServerC, ServerD);
                                                    break;
                                                }
                                            }
                                        }
                                    }).start();
                            } else {
                                start(context, ServerA, ServerB, ServerC, ServerD);
                            }
                        }
                        //震动
                        Vibrator vibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
                        vibrator.vibrate(100);
                    }
                });
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
        }
    }

    public static boolean isAccessibilitySettingsOn(Context context, String className) {
        if (context == null) {
            return false;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices =
            activityManager.getRunningServices(100);// 获取正在运行的服务列表
        if (runningServices.size() < 0) {
            return false;
        }
        for (int i=0;i < runningServices.size();i++) {
            ComponentName service = runningServices.get(i).service;
            if (service.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    private static boolean StartApp(Context context, String AppPackage) {
        try {
            Intent intent = new Intent();
            intent = context.getPackageManager().getLaunchIntentForPackage(AppPackage);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void start(final Activity context, final SwitchButton ServerA, final SwitchButton ServerB, final SwitchButton ServerC, final SwitchButton ServerD) {
        try {
            if (!ServerA.isChecked() && !ServerB.isChecked() && !ServerC.isChecked() && !ServerD.isChecked()) {
                try {
                    //打开ppsspp
                    StartApp(context, "org.ppsspp.ppsspp");
                    String path = "/storage/emulated/0/PSP/GAME/魔法禁书目录.iso";//指定的文件位置 
                    Intent intent = new Intent(Intent.ACTION_VIEW); 
                    intent.addCategory(Intent.CATEGORY_DEFAULT); 
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                    Uri uri = Uri.parse(path);
                    intent.setDataAndType(uri, "*/*");
                    ComponentName app1 = new ComponentName("org.ppsspp.ppsspp", "org.ppsspp.ppsspp.PpssppActivity");//指定的程序包名和文件名
                    intent.setComponent(app1);
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "您似乎还没有安装模拟器呢，快去安装吧！", Toast.LENGTH_SHORT).show();
                }
            } else {
                //获取设置内容
                FileInputStream in = new FileInputStream(new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini"));
                byte Inputbyt[] = new byte[1024];
                int len = in.read(Inputbyt);
                String SetUpText = new String(Inputbyt, 0, len);
                in.close();

                Pattern StartRegular = Pattern.compile("自动输入账号密码 = .*");
                Matcher Startmatcher = StartRegular.matcher(SetUpText);
                Startmatcher.find();
                String Start = Startmatcher.group(0);

                Pattern FCNRegular = Pattern.compile("御坂网络选择 = .*");
                Matcher FCNmatcher = FCNRegular.matcher(SetUpText);
                FCNmatcher.find();
                String FCN = FCNmatcher.group(0);

                /*
                 *修改设置
                 */
                String NewSetUp = SetUpText.replace(Start, "自动输入账号密码 = True");
                FileOutputStream out = new FileOutputStream(new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini"));

                //服务器选择
                if (ServerA.isChecked()) {
                    Pattern ServerRegular = Pattern.compile("服务器自动选择 = .*");
                    Matcher Servermatcher = ServerRegular.matcher(NewSetUp);
                    Servermatcher.find();
                    String ServerChecked = Servermatcher.group(0);
                    NewSetUp = NewSetUp.replace(ServerChecked, "服务器自动选择 = 2600");
                } else if (ServerB.isChecked()) {
                    Pattern ServerRegular = Pattern.compile("服务器自动选择 = .*");
                    Matcher Servermatcher = ServerRegular.matcher(NewSetUp);
                    Servermatcher.find();
                    String ServerChecked = Servermatcher.group(0);
                    NewSetUp = NewSetUp.replace(ServerChecked, "服务器自动选择 = 5000");

                } else if (ServerC.isChecked()) {
                    Pattern ServerRegular = Pattern.compile("服务器自动选择 = .*");
                    Matcher Servermatcher = ServerRegular.matcher(NewSetUp);
                    Servermatcher.find();
                    String ServerChecked = Servermatcher.group(0);
                    NewSetUp = NewSetUp.replace(ServerChecked, "服务器自动选择 = 1114");
                } else if (ServerD.isChecked()) {
                    Pattern ServerRegular = Pattern.compile("服务器自动选择 = .*");
                    Matcher Servermatcher = ServerRegular.matcher(NewSetUp);
                    Servermatcher.find();
                    String ServerChecked = Servermatcher.group(0);
                    NewSetUp = NewSetUp.replace(ServerChecked, "服务器自动选择 = 1938");
                }

                byte Outputbuy[] = NewSetUp.getBytes();
                out.write(Outputbuy);
                out.close();

                //修改模拟器设置
                PPSSPPSetting(context);

                //检测VPN，若已连接则跳转魔禁并按照设置打开悬浮窗
                new Thread(new Runnable(){
                        @Override
                        public void run() {
                            while (true) {
                                if (isVpn.isVPNConnected(context)) {
                                    try {
                                        //获取设置内容
                                        FileInputStream in = new FileInputStream(new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini"));
                                        byte Inputbyt[] = new byte[1024];
                                        int len = in.read(Inputbyt);
                                        String SetUpText = new String(Inputbyt, 0, len);
                                        in.close();

                                        Pattern ServerRegular = Pattern.compile("自动打开悬浮窗 = .*");
                                        Matcher Servermatcher = ServerRegular.matcher(SetUpText);
                                        Servermatcher.find();
                                        String ServerChecked = Servermatcher.group(0);

                                        if (ServerChecked.equals("自动打开悬浮窗 = True")) {
                                            if (Build.VERSION.SDK_INT >= 23) {
                                                if (!Settings.canDrawOverlays(context)) {
                                                    context.runOnUiThread(new Runnable(){
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(context, "让我看看…悬浮窗权限好像没开？！开好了就返回启动姬吧", Toast.LENGTH_SHORT).show();
                                                                context.startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName())), 0);
                                                            }
                                                        });
                                                    new Thread(new Runnable(){
                                                            @Override
                                                            public void run() {
                                                                while (true) {
                                                                    if (Settings.canDrawOverlays(context)) {
                                                                        context.runOnUiThread(new Runnable(){
                                                                                @Override
                                                                                public void run() {
                                                                                    try {
                                                                                        PingFloatingWindow.start(context);

                                                                                        //打开ppsspp
                                                                                        StartApp(context, "org.ppsspp.ppsspp");
                                                                                        String path = "/storage/emulated/0/PSP/GAME/魔法禁书目录.iso";//指定的文件位置 
                                                                                        Intent intent = new Intent(Intent.ACTION_VIEW); 
                                                                                        intent.addCategory(Intent.CATEGORY_DEFAULT); 
                                                                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                                                                                        Uri uri = Uri.parse(path);
                                                                                        intent.setDataAndType(uri, "*/*");
                                                                                        ComponentName app1 = new ComponentName("org.ppsspp.ppsspp", "org.ppsspp.ppsspp.PpssppActivity");//指定的程序包名和文件名
                                                                                        intent.setComponent(app1);
                                                                                        context.startActivity(intent);
                                                                                    } catch (Exception e) {
                                                                                        Toast.makeText(context, "您似乎还没有安装模拟器呢，快去安装吧！", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                }
                                                                            });
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                        }).start();
                                                } else {
                                                    context.runOnUiThread(new Runnable(){
                                                            @Override
                                                            public void run() {
                                                                try {
                                                                    PingFloatingWindow.start(context);

                                                                    //打开ppsspp
                                                                    StartApp(context, "org.ppsspp.ppsspp");
                                                                    String path = "/storage/emulated/0/PSP/GAME/魔法禁书目录.iso";//指定的文件位置 
                                                                    Intent intent = new Intent(Intent.ACTION_VIEW); 
                                                                    intent.addCategory(Intent.CATEGORY_DEFAULT); 
                                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                                                                    Uri uri = Uri.parse(path);
                                                                    intent.setDataAndType(uri, "*/*");
                                                                    ComponentName app1 = new ComponentName("org.ppsspp.ppsspp", "org.ppsspp.ppsspp.PpssppActivity");//指定的程序包名和文件名
                                                                    intent.setComponent(app1);
                                                                    context.startActivity(intent);
                                                                } catch (Exception e) {
                                                                    Toast.makeText(context, "您似乎还没有安装模拟器呢，快去安装吧！", Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        AppNotification.error(context, ErrorGet.Log(e));
                                    }
                                    break;
                                }
                            }
                        }
                    }).start();

                //跳转御坂网络
                if (FCN.equals("御坂网络选择 = 御坂网络")) {
                    boolean MisakaApp = StartApp(context, "com.yubanwangluo.lanren");
                    if (!MisakaApp) {
                        boolean FCNApp = StartApp(context, "com.xfconnect.fcn");
                        if (!FCNApp) {
                            Toast.makeText(context, "咦？御坂网络呢，没装好可不行", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    boolean FCNApp = StartApp(context, "com.xfconnect.fcn");
                    if (!FCNApp) {
                        boolean MisakaApp = StartApp(context, "com.yubanwangluo.lanren");
                        if (!MisakaApp) {
                            Toast.makeText(context, "咦？御坂网络呢，没装好可不行", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
        }
    }

    public static boolean PPSSPPSetting(Context context) {
        try {
            File GameSetting = new File("/storage/emulated/0/PSP/SYSTEM/ULJS00329_ppsspp.ini");
            File PPSSPPSetting = new File("/storage/emulated/0/PSP/SYSTEM/ppsspp.ini");
            String SetUpText = null;
            boolean FileExists;
            //获取内容
            if (GameSetting.exists()) {
                FileInputStream in = new FileInputStream(GameSetting);
                byte[] Inputbyt = Files.readAllBytes(GameSetting.toPath());
                int len = in.read(Inputbyt);
                SetUpText = new String(Inputbyt, 0, len);
                in.close();
                FileExists = true;
            } else {
                FileInputStream in = new FileInputStream(PPSSPPSetting);
                byte[] Inputbyt = Files.readAllBytes(PPSSPPSetting.toPath());
                int len = in.read(Inputbyt);
                SetUpText = new String(Inputbyt, 0, len);
                in.close();
                FileExists = false;
            }

            //启用联网
            Pattern ServerRegular = Pattern.compile("EnableWlan = .*");
            Matcher Servermatcher = ServerRegular.matcher(SetUpText);
            Servermatcher.find();
            String ServerText = Servermatcher.group(0);
            SetUpText = SetUpText.replace(ServerText, "EnableWlan = True");

            //更改IP
            Pattern IPRegular = Pattern.compile("proAdhocServer = .*");
            Matcher IPmatcher = IPRegular.matcher(SetUpText);
            IPmatcher.find();
            String IPText = IPmatcher.group(0);
            SetUpText = SetUpText.replace(IPText, "proAdhocServer = 10.10.0.1");

            //端口偏移
            Pattern PortRegular = Pattern.compile("PortOffset = .*");
            Matcher Portmatcher = PortRegular.matcher(SetUpText);
            Portmatcher.find();
            String PortText = Portmatcher.group(0);
            SetUpText = SetUpText.replace(PortText, "PortOffset = 5000");

            //随机Mac
            Random random = new Random();
            String[] Mac = {
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff)),
                String.format("%02x", random.nextInt(0xff))
            };
            String NewMacText = String.join(":", Mac);
            Pattern MacRegular = Pattern.compile("MacAddress = .*");
            Matcher Macmatcher = MacRegular.matcher(SetUpText);
            Macmatcher.find();
            String MacText = Macmatcher.group(0);
            SetUpText = SetUpText.replace(MacText, "MacAddress = " + NewMacText);

            //修改设置
            if (FileExists) {
                FileOutputStream out = new FileOutputStream(GameSetting);
                byte Outputbuy[] = SetUpText.getBytes();
                out.write(Outputbuy);
                out.close();
            } else {
                FileOutputStream out = new FileOutputStream(PPSSPPSetting);
                byte Outputbuy[] = SetUpText.getBytes();
                out.write(Outputbuy);
                out.close();
            }

            return true;
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
            return false;
        }
    }
}

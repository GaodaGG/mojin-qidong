package com.mojin.qidon.StartServer;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import com.mojin.qidon.AppNotification;
import com.mojin.qidon.ErrorGet;
import com.mojin.qidon.PingFloatingWindow;
import com.mojin.qidon.StartGame;
import com.mojin.qidon.isVpn;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShortCutStartGame {
    /*
     *开始游戏代码
     */
    public static void Start(final Activity context, String Server) {
        try {
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
            Pattern ServerRegular = Pattern.compile("服务器自动选择 = .*");
            Matcher Servermatcher = ServerRegular.matcher(NewSetUp);
            Servermatcher.find();
            String ServerChecked = Servermatcher.group(0);
            NewSetUp = NewSetUp.replace(ServerChecked, "服务器自动选择 = " + Server);

            byte Outputbuy[] = NewSetUp.getBytes();
            out.write(Outputbuy);
            out.close();

            //修改模拟器设置
            StartGame.PPSSPPSetting(context);

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
                                                                                    
                                                                                    //结束活动
                                                                                    context.finish();
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
                                                                
                                                                //结束活动
                                                                context.finish();
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
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
        }
    }
    
    private static boolean StartApp(Activity context, String AppPackage) {
        try {
            Intent intent = new Intent();
            intent = context.getPackageManager().getLaunchIntentForPackage(AppPackage);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
            return false;
        }
    }
}

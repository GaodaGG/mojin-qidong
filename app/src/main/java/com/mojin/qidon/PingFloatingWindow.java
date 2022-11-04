package com.mojin.qidon;
import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import com.lzf.easyfloat.EasyFloat;
import com.lzf.easyfloat.enums.ShowPattern;
import java.net.InetAddress;
import java.util.Date;

public class PingFloatingWindow {
    private static long TimeA;
    private static long TimeB;
    private static TextView PingTextView;
    public static boolean start = true;
    private static Thread Ping;

    public static void start(final Activity context) {
        try {
            start = true;
            
            //显示悬浮窗
            EasyFloat.with(context)
                .setShowPattern(ShowPattern.ALL_TIME)
                .setLayout(R.layout.floatingwindow)
                .setDragEnable(true)
                .setMatchParent(false, false)
                .setGravity(Gravity.CENTER | Gravity.LEFT, 50)
                .setLayoutChangedGravity(Gravity.CENTER | Gravity.LEFT)
                .setTag("PingFloat")
                .show();
                
            //ping服务器并显示到悬浮窗上
            Ping = new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(200);
                            View view = EasyFloat.getFloatView("PingFloat");
                            PingTextView = view.findViewById(R.id.PingTextView);
                            while (start) {
                                Thread.sleep(1000);
                                TimeA = new Date().getTime();
                                boolean status = InetAddress.getByName("10.10.0.1").isReachable(10000);
                                if (status) {
                                    TimeB = new Date().getTime();
                                    context.runOnUiThread(new Runnable(){
                                            @Override
                                            public void run() {
                                                try {
                                                    CharSequence PingText = String.valueOf(TimeB - TimeA);
                                                    PingTextView.setText(PingText);
                                                } catch (Exception e) {
                                                    AppNotification.error(context, ErrorGet.Log(e));
                                                }
                                            }
                                        });
                                } else {
                                    context.runOnUiThread(new Runnable(){
                                            public void run() {
                                                try {
                                                    PingTextView.setText("超时");
                                                } catch (Exception e) {
                                                    AppNotification.error(context, ErrorGet.Log(e));
                                                }
                                            }
                                        });
                                }
                            }
                        } catch (Exception e) {
                            if(!"java.lang.InterruptedException".equals(e.toString())){
                                AppNotification.error(context, ErrorGet.Log(e));
                            }
                        }
                    }
                });
            Ping.start();
            
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
        }
    }

    public static void stop(Activity context) {
        start = false;
        Ping.interrupt();
        EasyFloat.dismiss("PingFloat");
    }
}

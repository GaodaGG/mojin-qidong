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
    static long TimeA;
    static long TimeB;
    static TextView PingTextView;
    static boolean start = true;
    static Thread Ping;
    public static void start(final Activity context) {
        try {
            if (start) {
                EasyFloat.with(context)
                    .setShowPattern(ShowPattern.ALL_TIME)
                    .setLayout(R.layout.floatingwindow)
                    .setDragEnable(true)
                    .setMatchParent(false, false)
                    .setGravity(Gravity.CENTER)
                    .show();
                //ping服务器并显示到悬浮窗上
                Ping = new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                                View view = EasyFloat.getAppFloatView();
                                PingTextView = (TextView)view.findViewById(R.id.PingTextView);
                                while (true) {
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
                                        // EasyFloat.showAppFloat();
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
                                        //  EasyFloat.showAppFloat();
                                    }
                                }
                            } catch (Exception e) {
                                if (!"java.lang.InterruptedException".equals(ErrorGet.Log(e))) {
                                    AppNotification.error(context, ErrorGet.Log(e));
                                }
                            }
                        }
                    });
                Ping.start();
                start = false;
            } else {
                EasyFloat.dismissAppFloat();
                Ping.interrupt();
                start = true;
            }
        } catch (Exception e) {
            AppNotification.error(context, ErrorGet.Log(e));
        }
    }

    public static int dpToPx(Context context, int dp) {
        return dp * (context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}

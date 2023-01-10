package com.mojin.qidon;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import java.io.*;
import java.net.NetworkInterface;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccessibilitySampleService extends AccessibilityService {
    private static String FCNServerName;
    private static String FCNPassword;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            /*
             *获取信息
             */
            //获取设置内容
            FileInputStream in = new FileInputStream(new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini"));
            byte Inputbyt[] = new byte[1024];
            int len = in.read(Inputbyt);
            String SetUpText = new String(Inputbyt, 0, len);
            in.close();

            //密码
            Pattern Regular = Pattern.compile("密码 = .*");
            Matcher matcher = Regular.matcher(SetUpText);
            matcher.find();
            String Password = matcher.group(0);
            String PasswordA[] = Password.split("= ");
            FCNPassword = PasswordA[1];

            //服务器名
            Pattern ServerRegular = Pattern.compile("服务器自动选择 = .*");
            Matcher Servermatcher = ServerRegular.matcher(SetUpText);
            Servermatcher.find();
            String Server = Servermatcher.group(0);
            String ServerA[] = Server.split("= ");
            FCNServerName = ServerA[1];

            //自动输入账号密码判定
            Pattern StartRegular = Pattern.compile("自动输入账号密码 = .*");
            Matcher Startmatcher = StartRegular.matcher(SetUpText);
            Startmatcher.find();
            String Start = Startmatcher.group(0);
            String StartA[] = Start.split("= ");
            String StartBoolean = StartA[1];
            if (Boolean.parseBoolean(StartBoolean)) {
                //获取根节点
                AccessibilityNodeInfo rootInfo = getRootInActiveWindow();

                //FCNID
                List<AccessibilityNodeInfo> FCNID = rootInfo.findAccessibilityNodeInfosByViewId("com.xfconnect.fcn:id/editFcnId");
                AccessibilityNodeInfo FCNIDNode;
                for (int i = 0; i < FCNID.size(); i++) {
                    FCNIDNode = FCNID.get(i);
                    changeInput(FCNIDNode, "1204737574");
                    //  AppNotification.error(this, FCNID.get(i).toString());
                }

                //服务器名
                List<AccessibilityNodeInfo> ServerName = rootInfo.findAccessibilityNodeInfosByViewId("com.xfconnect.fcn:id/editFcnName");
                AccessibilityNodeInfo ServerNameNode;
                for (int i = 0; i < ServerName.size(); i++) {
                    ServerNameNode = ServerName.get(i);
                    changeInput(ServerNameNode, FCNServerName);
                }

                //密码
                List<AccessibilityNodeInfo> PassWord = rootInfo.findAccessibilityNodeInfosByViewId("com.xfconnect.fcn:id/editFcnPsk");
                AccessibilityNodeInfo PassWordNode;
                for (int i = 0; i < PassWord.size(); i++) {
                    PassWordNode = PassWord.get(i);
                    if (FCNServerName.equals("1938")) {
                        changeInput(PassWordNode, "123456780");
                    } else {
                        changeInput(PassWordNode, FCNPassword);
                    }
                }

                //开始连接
                List<AccessibilityNodeInfo> Connect = rootInfo.findAccessibilityNodeInfosByText("开始连接");
                AccessibilityNodeInfo ConnectNode;
                for (int i = 0; i < Connect.size(); i++) {
                    ConnectNode = Connect.get(i);
                    ConnectNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }


                if (isVpn.isVPNConnected(this)) {
                    //修改设置
                    String NewSetUp = SetUpText.replace(Start, "自动输入账号密码 = False");
                    FileOutputStream out = new FileOutputStream(new File("/sdcard/Android/data/com.mojin.qidon/files/sz.ini"));
                    byte Outputbuy[] = NewSetUp.getBytes();
                    out.write(Outputbuy);
                    out.close();
                }
            }
        } catch (Exception e) {
            AppNotification.error(this, ErrorGet.Log(e));
            /*
             //获取剪贴板管理器：  
             ClipboardManager cm = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);  
             // 创建普通字符型ClipData  
             ClipData mClipData = ClipData.newPlainText("Label", GlobalVariable.ErrorText);  
             // 将ClipData内容放到系统剪贴板里。  
             cm.setPrimaryClip(mClipData);
             */
        }
    }

    @Override
    public void onServiceConnected() {
        FCNServerName = "5000";
        FCNPassword = "az";
        Toast.makeText(this, "无障碍服务已启动~", Toast.LENGTH_SHORT).show();
        super.onServiceConnected();
    }

    @Override 
    public void onInterrupt() {

    }

    private void changeInput(AccessibilityNodeInfo info, String text) {  //改变editText的内容
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        info.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }
}

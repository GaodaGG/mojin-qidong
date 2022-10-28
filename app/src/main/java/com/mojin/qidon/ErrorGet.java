package com.mojin.qidon;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorGet extends BroadcastReceiver {
    public static String ErrorText = "";
    @Override
    public void onReceive(Context context, Intent intent) {
        //获取剪贴板管理器：  
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);  
        // 创建普通字符型ClipData  
        ClipData mClipData = ClipData.newPlainText("Label", ErrorText);  
        // 将ClipData内容放到系统剪贴板里。  
        cm.setPrimaryClip(mClipData);
        Toast.makeText(context,"已复制",Toast.LENGTH_SHORT).show();
    }
    
    public static String Log(Throwable e){
        if(e!=null){
            StringWriter trace = new StringWriter();
            e.printStackTrace(new PrintWriter(trace));
            ErrorText = trace.toString();
            return trace.toString();
        }
        return "没有获取到报错";
    }
}

package com.mojin.qidon.StartServer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import com.mojin.qidon.AccessibilitySampleService;
import com.mojin.qidon.StartGame;
import com.mojin.qidon.StartServer.ServerA;

public class ServerD extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!StartGame.isAccessibilitySettingsOn(this, AccessibilitySampleService.class.getName())) {// 判断服务是否开启
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "请选择御坂网络辅助", Toast.LENGTH_SHORT).show();
            new Thread(new Runnable(){
                    @Override
                    public void run() {
                        while (true) {
                            if (StartGame.isAccessibilitySettingsOn(ServerD.this, AccessibilitySampleService.class.getName())) {
                                ShortCutStartGame.Start(ServerD.this, "1938");
                                break;
                            }
                        }
                    }
                }).start();
        } else {
            ShortCutStartGame.Start(this, "1938");
        }
    }
    
}

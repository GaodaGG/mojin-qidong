package com.mojin.qidon;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.widget.Toast;
import java.util.Arrays;

public class ShortCut {
    public static void AddShortCut(Context context, Class targetClass, String Label, String ID, int ShortCutIcon) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                Intent shortcutInfoIntent = new Intent(context, targetClass);
                shortcutInfoIntent.setAction(Intent.ACTION_VIEW);
                shortcutInfoIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

                ShortcutInfo shortcut = new ShortcutInfo.Builder(context, ID)
                    .setShortLabel(Label)
                    .setLongLabel(Label)
                    .setIcon(Icon.createWithResource(context, ShortCutIcon))
                    .setIntent(shortcutInfoIntent)
                    .setDisabledMessage("快捷方式好像出了点问题，请重新创建")
                    .build();

                shortcutManager.addDynamicShortcuts(Arrays.asList(shortcut));
            }
        } else {
            Toast.makeText(context, "设备不支持在桌面创建快捷图标！", Toast.LENGTH_LONG).show();
        }
    }
    
    public static void UpdateShortCut(Context context, Class targetClass, String Label, String ID, int ShortCutIcon) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                Intent shortcutInfoIntent = new Intent(context, targetClass);
                shortcutInfoIntent.setAction(Intent.ACTION_VIEW);
                shortcutInfoIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

                ShortcutInfo shortcut = new ShortcutInfo.Builder(context, ID)
                    .setShortLabel(Label)
                    .setLongLabel(Label)
                    .setIcon(Icon.createWithResource(context, ShortCutIcon))
                    .setIntent(shortcutInfoIntent)
                    .setDisabledMessage("快捷方式好像出了点问题，请重新创建")
                    .build();

                shortcutManager.updateShortcuts(Arrays.asList(shortcut));
            }
        } else {
            Toast.makeText(context, "设备不支持在桌面创建快捷图标！", Toast.LENGTH_LONG).show();
        }
    }
}

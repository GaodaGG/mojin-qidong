package com.mojin.qidon;
import com.sd.lib.switchbutton.SwitchButton;
import android.app.Activity;
import android.widget.Toast;
import java.io.File;
import android.net.Uri;
import android.content.Intent;

public class GameDelete {
    public static void GameDelete(SwitchButton Game_ISO, SwitchButton MisakaFCN, SwitchButton GamePPSSPP, SwitchButton GameSave, final Activity context) {
        boolean XuanZe = false;

        //获取开关选中状态
        boolean GameISOChecked = Game_ISO.isChecked();
        boolean MisakaFCNChecked = MisakaFCN.isChecked();
        boolean GamePPSSPPChecked = GamePPSSPP.isChecked();
        boolean GameSaveChecked = GameSave.isChecked();

        //设置文件和目录的变量
        String SaveDataPath = "/storage/emulated/0/PSP/SAVEDATA/ULJS00329DATA00/";


        if (GlobalVariable.Countdown != 0) {
            Toast.makeText(context, "在" + String.valueOf(GlobalVariable.Countdown) + "秒后可以再次尝试", Toast.LENGTH_SHORT).show();
        } else {
            //删除游戏本体
            if (GameISOChecked) {
                try {
                    File GameIso = new File("/storage/emulated/0/PSP/GAME/魔法禁书目录.iso");
                    if (GameIso.exists()) {
                        GameIso.delete();
                    } else {
                        Toast.makeText(context, "没有检测到\"魔法禁书目录.iso\"文件", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                AppNotification.error(context,ErrorGet.Log(e));
                }
            }

            //删除存档
            if (GameSaveChecked) {
                try {
                    File GameSaveDate = new File(SaveDataPath);
                    if (GameSaveDate.exists()) {
                        GameSaveDate.delete();
                    }else{
                        Toast.makeText(context, "没有检测到存档", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                AppNotification.error(context,ErrorGet.Log(e));
                }
            }
            
            //删除御坂网络安装包并卸载
            if(MisakaFCNChecked){
                try{
                    File MisakaFCNApk = new File("/storage/emulated/0/PSP/GAME/御坂网络.apk");
                    if (MisakaFCNApk.exists()) {
                        MisakaFCNApk.delete();
                    }
                    //卸载御坂网络
                    Uri MisakaApp=Uri.parse("package:com.yubanwangluo.lanren");
                    Intent MisakaUninstall = new Intent(Intent.ACTION_DELETE,MisakaApp);
                    context.startActivity(MisakaUninstall);
                    
                    //卸载fcn
                    Uri FcnApp=Uri.parse("package:com.xfconnect.fcn");
                    Intent FcnUninstall = new Intent(Intent.ACTION_DELETE,FcnApp);
                    context.startActivity(FcnUninstall);
                }catch(Exception e){
                AppNotification.error(context,ErrorGet.Log(e));
                }
            }
            
            if(GamePPSSPPChecked){
                try{
                    File PPSSPPApk = new File("/storage/emulated/0/PSP/GAME/PPSSPP.apk");
                    if (PPSSPPApk.exists()) {
                        PPSSPPApk.delete();
                    }
                    //卸载御坂网络
                    Uri PPSSPPApp=Uri.parse("package:org.ppsspp.ppsspp");
                    Intent Uninstall = new Intent(Intent.ACTION_DELETE,PPSSPPApp);
                    context.startActivity(Uninstall);
                    
                }catch(Exception e){
                AppNotification.error(context,ErrorGet.Log(e));
                }
            }
            
            //判断有无选择
            if (!GameISOChecked && !MisakaFCNChecked && !GamePPSSPPChecked && !GameSaveChecked) {
                Toast.makeText(context, "欸？一个也没有？", Toast.LENGTH_SHORT).show();
                XuanZe = true;
            }

            //提示
            if (!XuanZe) {
                Toast.makeText(context, "火力全开！", Toast.LENGTH_SHORT).show();
            }

            //CD限制
            GlobalVariable.Countdown = 15;
            new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            for (int i = 1 ;i <= 15 ;i++) {
                                GlobalVariable.Countdown--;
                                Thread.sleep(1000);
                            }
                        } catch (Exception e) {
                        AppNotification.error(context,ErrorGet.Log(e));
                        }
                    }
                }).start();
        }
    }
}

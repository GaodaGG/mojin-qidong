package com.mojin.qidon;
import com.sd.lib.switchbutton.SwitchButton;
import android.content.Context;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.widget.Toast;

public class QualitySettings {
    public static void QualitySettings(SwitchButton LowQuality, SwitchButton MediumQuality, SwitchButton HighQuality, Context context) {
        final SwitchButton LowButton = LowQuality;
        final SwitchButton MediumButton = MediumQuality;
        final SwitchButton HighButton = HighQuality;

        //获取开关选中状态
        boolean LowChecked = LowButton.isChecked();
        boolean MediumChecked = MediumButton.isChecked();
        boolean HighChecked = HighButton.isChecked();

        //路径
        String High = "hzsz/1st.zip";
        String Medium = "hzsz/2nd.zip";
        String Low = "hzsz/3rd.zip";
        String path = "/storage/emulated/0/PSP";


        //解压低画质设置
        if (LowChecked) {
            ZipInputStream ZipInput = null;
            ZipEntry entry;
            File FilePath = new File(path);

            try {
                ZipInput = new ZipInputStream(context.getResources().getAssets().open(Low));
                while (true) {
                    entry = ZipInput.getNextEntry();
                    if (entry == null) {
                        break;
                    }

                    if (entry.isDirectory()) {
                        continue;
                    }

                    File f = new File(FilePath , entry.getName());
                    if (!f.getParentFile().exists()) {
                        f.getParentFile().mkdirs();
                    }

                    int count = -1;
                    byte[] buf = new byte[1024];
                    //输出流

                    FileOutputStream fos = new FileOutputStream(f);
                    //压缩流循环读取 压缩包中的文件 直到读完
                    while ((count = ZipInput.read(buf)) != -1) {
                        //写入
                        fos.write(buf , 0 , count);
                        //流刷新（提升效率）
                        fos.flush();
                    }
                    //关闭流
                    fos.close();
                    //关闭本次条目，跳到下一个
                    ZipInput.closeEntry();
                }
            } catch (Exception e) {
                Toast.makeText(context, "解压失败", Toast.LENGTH_SHORT).show();
                AppNotification.error(context,ErrorGet.Log(e));
            } finally {
                if (ZipInput != null) {
                    try {
                        ZipInput.close();
                    } catch (IOException e) {
                        AppNotification.error(context,ErrorGet.Log(e));
                    }
                }
                Toast.makeText(context,"低画已就位",Toast.LENGTH_LONG).show();
            }
        }
        
        //解压中画质设置
        if (MediumChecked) {

            ZipInputStream ZipInput = null;
            ZipEntry entry;
            File FilePath = new File(path);

            try {
                ZipInput = new ZipInputStream(context.getResources().getAssets().open(Medium));
                while (true) {
                    entry = ZipInput.getNextEntry();
                    if (entry == null) {
                        break;
                    }

                    if (entry.isDirectory()) {
                        continue;
                    }

                    File f = new File(FilePath , entry.getName());
                    if (!f.getParentFile().exists()) {
                        f.getParentFile().mkdirs();
                    }

                    int count = -1;
                    byte[] buf = new byte[1024];
                    //输出流

                    FileOutputStream fos = new FileOutputStream(f);
                    //压缩流循环读取 压缩包中的文件 直到读完
                    while ((count = ZipInput.read(buf)) != -1) {
                        //写入
                        fos.write(buf , 0 , count);
                        //流刷新（提升效率）
                        fos.flush();
                    }
                    //关闭流
                    fos.close();
                    //关闭本次条目，跳到下一个
                    ZipInput.closeEntry();
                }
            } catch (Exception e) {
                Toast.makeText(context, "解压失败", Toast.LENGTH_SHORT).show();
                AppNotification.error(context,ErrorGet.Log(e));
            } finally {
                if (ZipInput != null) {
                    try {
                        ZipInput.close();
                    } catch (IOException e) {
                        AppNotification.error(context,ErrorGet.Log(e));
                    }
                }
                Toast.makeText(context,"这画质勉勉强强吧",Toast.LENGTH_LONG).show();
            }
        }
        
        //解压高画质设置
        if (HighChecked) {

            ZipInputStream ZipInput = null;
            ZipEntry entry;
            File FilePath = new File(path);

            try {
                ZipInput = new ZipInputStream(context.getResources().getAssets().open(High));
                while (true) {
                    entry = ZipInput.getNextEntry();
                    if (entry == null) {
                        break;
                    }

                    if (entry.isDirectory()) {
                        continue;
                    }

                    File f = new File(FilePath , entry.getName());
                    if (!f.getParentFile().exists()) {
                        f.getParentFile().mkdirs();
                    }

                    int count = -1;
                    byte[] buf = new byte[1024];
                    //输出流

                    FileOutputStream fos = new FileOutputStream(f);
                    //压缩流循环读取 压缩包中的文件 直到读完
                    while ((count = ZipInput.read(buf)) != -1) {
                        //写入
                        fos.write(buf , 0 , count);
                        //流刷新（提升效率）
                        fos.flush();
                    }
                    //关闭流
                    fos.close();
                    //关闭本次条目，跳到下一个
                    ZipInput.closeEntry();
                }
            } catch (Exception e) {
                Toast.makeText(context, "解压失败", Toast.LENGTH_SHORT).show();
                AppNotification.error(context,ErrorGet.Log(e));
            } finally {
                if (ZipInput != null) {
                    try {
                        ZipInput.close();
                    } catch (IOException e) {
                        AppNotification.error(context,ErrorGet.Log(e));
                    }
                }
                Toast.makeText(context,"超清晰！我猜的",Toast.LENGTH_LONG).show();
            }
        }
        
        //判断有无选择
        if(!LowChecked){
            if(!MediumChecked){
                if(!HighChecked){
                    Toast.makeText(context,"按自己配置选哟",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

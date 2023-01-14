package com.mojin.qidon.function;
import android.app.Activity;
import android.widget.Toast;
import com.alibaba.fastjson.JSONObject;
import com.mojin.qidon.AppNotification;
import com.mojin.qidon.DownloadFiles;
import com.mojin.qidon.ErrorGet;
import com.mojin.qidon.GameDownload;
import com.mojin.qidon.MainApplication;
import com.sd.lib.switchbutton.SwitchButton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GamePackage {
    /**
     *设置文件和目录的变量
     */
    //文件在网盘的路径
    final private static String base = "/GG/PSP魔禁相关/启动姬内材质/底包.zip"; 
    final private static String story = "/GG/PSP魔禁相关/启动姬内材质/剧情包.zip";
    final private static String model = "/GG/PSP魔禁相关/启动姬内材质/模型包.zip";
    //本地路径
    final private static String localPath = "/storage/emulated/0/PSP/TEXTURES/";
    final private static String texturesPath = "/storage/emulated/0/PSP/TEXTURES/ULJS00329/";
    //开关选中状态
    private boolean superChecked;
    private boolean storyChecked;
    private boolean modelChecked;
    //当前的Activity
    private Activity context;

    /**
     * @param superChecked 底包的开关
     * @param storyChecked 剧情包的开关
     * @param modelChecked 底包的开关
     * 获取开关选中状态
     */
    public GamePackage(SwitchButton superChecked, SwitchButton storyChecked, SwitchButton modelChecked, Activity context) {
        this.superChecked = superChecked.isChecked();
        this.storyChecked = storyChecked.isChecked();
        this.modelChecked = modelChecked.isChecked();
        this.context = context;
    }

    /**
     * @param context
     * Start
     */
    public void start() {
        if (((MainApplication)context.getApplication()).Countdown != 0) {
            Toast.makeText(context, "在" + String.valueOf(((MainApplication)context.getApplication()).Countdown) + "秒后可以再次尝试", Toast.LENGTH_SHORT).show();
        } else {
            if (!this.superChecked && !this.storyChecked && !this.modelChecked) {
                Toast.makeText(this.context, "欸？一个也没有？", Toast.LENGTH_SHORT).show();
            } else {
                if (this.superChecked) {
                    //下载超清底包
                    download(base, "超清底包", ".zip", 20);
                    Toast.makeText(this.context, "火力全开！", Toast.LENGTH_SHORT).show();
                }
                if (this.storyChecked) {
                    //下载剧情包
                    download(story, "剧情包", ".zip", 21);
                    Toast.makeText(this.context, "火力全开！", Toast.LENGTH_SHORT).show();
                }
                if (this.modelChecked) {
                    //下载模型包
                    download(model, "模型包", ".zip", 22);
                    Toast.makeText(this.context, "火力全开！", Toast.LENGTH_SHORT).show();
                }
            }

            //CD
            CD(this.context);
        }
    }

    /**
     * @param context
     * CD限制
     */
    private void CD(final Activity context) {
        //CD限制
        ((MainApplication)context.getApplication()).Countdown = 15;
        new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        for (int i = 1 ;i <= 15 ;i++) {
                            ((MainApplication)context.getApplication()).Countdown--;
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                        AppNotification.error(context, ErrorGet.Log(e));
                    }
                }
            }).start();
    }

    /**
     * @param path 网盘路径
     * @param name 文件名
     * @param suffix 后缀名
     * @param notificationID 通知ID
     * 下载文件
     */
    private void download(final String path, final String name, final String suffix, final int notificationID) {
        //创建新线程
        new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        //获取api并解析
                        final String apiJson = GameDownload.url(path, context);
                        JSONObject json = JSONObject.parseObject(apiJson);
                        json = JSONObject.parseObject(json.getString("data"));
                        final String downloadURL = json.getString("raw_url");
                        //发送通知
                        AppNotification.send(context, name, "0%", notificationID);
                        //开始下载
                        DownloadFiles.get().download(downloadURL, localPath, name + suffix, new DownloadFiles.OnDownloadListener() {
                                //下载完成
                                @Override
                                public void onDownloadSuccess() {
                                    //提示正在解压
                                    context.runOnUiThread(new Runnable(){
                                            @Override
                                            public void run() {
                                                Toast.makeText(context, "正在解压" + name, Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                    /*
                                     * 解压部分
                                     */
                                    ZipInputStream zipInput = null;
                                    ZipEntry entry;
                                    File filePath = new File(localPath);
                                    File texture = new File(texturesPath);

                                    try {
                                        //删除原来的材质包文件夹
                                        texture.delete();
                                        //循环解压
                                        zipInput = new ZipInputStream(new FileInputStream(new File(localPath + name + suffix)));
                                        while (true) {
                                            entry = zipInput.getNextEntry();
                                            if (entry == null) {
                                                break;
                                            }

                                            if (entry.isDirectory()) {
                                                continue;
                                            }

                                            File f = new File(filePath , entry.getName());
                                            if (!f.getParentFile().exists()) {
                                                f.getParentFile().mkdirs();
                                            }

                                            int count = -1;
                                            byte[] buf = new byte[1024];
                                            //输出流

                                            FileOutputStream fos = new FileOutputStream(f);
                                            //压缩流循环读取 压缩包中的文件 直到读完
                                            while ((count = zipInput.read(buf)) != -1) {
                                                //写入
                                                fos.write(buf , 0 , count);
                                                //流刷新（提升效率）
                                                fos.flush();
                                            }
                                            //关闭流
                                            fos.close();
                                            //关闭本次条目，跳到下一个
                                            zipInput.closeEntry();
                                        }
                                    } catch (Exception e) {
                                        //获取错误并发送通知
                                        String error = ErrorGet.Log(e);
                                        AppNotification.error(context, error);
                                        //提示
                                        context.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(context, "解压失败", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                    } finally {
                                        if (zipInput != null) {
                                            try {
                                                //关闭流
                                                zipInput.close();

                                                //发送通知
                                                AppNotification.delete(context, notificationID);
                                                AppNotification.ordinary(context, name, "下载成功！请到游戏里看看吧", notificationID, null, false);
                                                
                                                //删除下载的zip
                                                new File(localPath + name + suffix).delete();
                                            } catch (IOException e) {
                                                AppNotification.error(context, ErrorGet.Log(e));
                                            }
                                        }
                                    }
                                }

                                //下载进度
                                @Override
                                public void onDownloading(int progress, String length) {
                                    //更新通知
                                    AppNotification.update(context, name, length + " ● " + String.valueOf(progress) + "%", notificationID, progress);
                                }

                                //下载失败
                                @Override
                                public void onDownloadFailed() {
                                    //发送通知
                                    AppNotification.ordinary(context, name, "下载失败，可能是网络问题", notificationID, null, false);
                                }
                            });
                    } catch (Exception e) {
                        AppNotification.error(context, ErrorGet.Log(e));
                    }
                }
            }).start();
    }
}

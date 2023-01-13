package com.mojin.qidon.function;
import android.content.Context;
import android.widget.Toast;
import com.alibaba.fastjson.JSONObject;
import com.mojin.qidon.AppNotification;
import com.mojin.qidon.DownloadFiles;
import com.mojin.qidon.ErrorGet;
import com.mojin.qidon.GameDownload;
import com.sd.lib.switchbutton.SwitchButton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import android.app.Activity;

public class GamePackage {
    /**
     *设置文件和目录的变量
     */
    //下载文件的api
    final private static String api = "http://106.53.148.51:3100/api/fs/get";
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
    //开关是否选择状态
    private boolean select = false;

    /**
     * @param superChecked 底包的开关
     * @param storyChecked 剧情包的开关
     * @param modelChecked 底包的开关
     * 获取开关选中状态
     */
    public GamePackage(SwitchButton superChecked, SwitchButton storyChecked, SwitchButton modelChecked) {
        this.superChecked = superChecked.isChecked();
        this.storyChecked = storyChecked.isChecked();
        this.modelChecked = modelChecked.isChecked();
    }

    /**
     * @param context
     * @param 
     * 下载文件
     */
    public void download(final Activity context, String path, String name) {
        //创建新线程
        new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        final String ApiJson = GameDownload.url(base, context);//base改path
                        JSONObject Json = JSONObject.parseObject(ApiJson);
                        Json = JSONObject.parseObject(Json.getString("data"));
                        final String DownloadURL = Json.getString("raw_url");
                        AppNotification.send(context, "超清底包", "0%", 20);//超清底包改name
                        DownloadFiles.get().download(DownloadURL, localPath, "超清底包.zip", new DownloadFiles.OnDownloadListener() {
                                //下载完成
                                @Override
                                public void onDownloadSuccess() {
                                    //提示正在解压
                                    context.runOnUiThread(new Runnable(){
                                            @Override
                                            public void run() {
                                                Toast.makeText(context, "正在解压底包", Toast.LENGTH_SHORT).show();
                                            }
                                        });

                                    //解压部分
                                    ZipInputStream ZipInput = null;
                                    ZipEntry entry;
                                    File FilePath = new File(localPath);
                                    File TexTure = new File(texturesPath);

                                    try {
                                        TexTure.delete();
                                        ZipInput = new ZipInputStream(new FileInputStream(new File(localPath + "超清底包.zip")));
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
                                        final String error = ErrorGet.Log(e);
                                        context.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Toast.makeText(context, "解压失败", Toast.LENGTH_SHORT).show();
                                                    AppNotification.error(context, error);
                                                }
                                            });
                                    } finally {
                                        if (ZipInput != null) {
                                            try {
                                                ZipInput.close();

                                                //发送通知
                                                AppNotification.delete(context, 20);
                                                AppNotification.ordinary(context, "超清底包", "下载成功！请到游戏里看看吧", 21, null, false);
                                                new File(localPath + "超清底包.zip").delete();
                                            } catch (IOException e) {
                                                AppNotification.error(context, ErrorGet.Log(e));
                                            }
                                        }
                                    }
                                }

                                //下载进度
                                @Override
                                public void onDownloading(int progress, String length) {
                                    AppNotification.update(context, "超清底包", length + " ● " + String.valueOf(progress) + "%", 20, progress);
                                }

                                //下载失败
                                @Override
                                public void onDownloadFailed() {
                                    //发送通知
                                    AppNotification.ordinary(context, "超清底包", "下载失败，可能是网络问题", 20, null, false);
                                }
                            });
                    } catch (Exception e) {
                        AppNotification.error(context, ErrorGet.Log(e));
                    }
                }
            }).start();
    }
}

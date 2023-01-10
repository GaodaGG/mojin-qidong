package com.mojin.qidon;
import android.app.Activity;
import android.os.Environment;
import android.widget.Toast;
import com.alibaba.fastjson.JSONObject;
import com.sd.lib.switchbutton.SwitchButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.InputStream;
import java.io.FileInputStream;

public class GamePackage {
    //设置文件和目录的变量
    final static String api = "http://106.53.148.51:3100/api/fs/get";
    final static String Base = "/GG/PSP魔禁相关/启动姬内材质/底包.zip";
    final static String Story = "/GG/PSP魔禁相关/启动姬内材质/剧情包.zip";
    final static String Model = "/GG/PSP魔禁相关/启动姬内材质/模型包.zip";
    final static String path = "/storage/emulated/0/PSP/TEXTURES/";
    final static String TexTures = "/storage/emulated/0/PSP/TEXTURES/ULJS00329/";

    public static void GamePackage(SwitchButton SuperGamePackage, SwitchButton StoryGamePackage, SwitchButton ModelGamePackage, final Activity context) {
        boolean XuanZe = false;

        //获取开关选中状态
        boolean SuperChecked = SuperGamePackage.isChecked();
        boolean StoryChecked = StoryGamePackage.isChecked();
        boolean ModelChecked = ModelGamePackage.isChecked();

        if (((MainApplication)context.getApplication()).Countdown != 0) {
            Toast.makeText(context, "在" + String.valueOf(((MainApplication)context.getApplication()).Countdown) + "秒后可以再次尝试", Toast.LENGTH_SHORT).show();
        } else {
            //下载超清底包
            if (SuperChecked) {
                new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                final String ApiJson = GameDownload.url(Base, context);
                                JSONObject Json = JSONObject.parseObject(ApiJson);
                                Json = JSONObject.parseObject(Json.getString("data"));
                                final String DownloadURL = Json.getString("raw_url");
                                AppNotification.send(context, "超清底包", "0%", 20);
                                DownloadFiles.get().download(DownloadURL, path, "超清底包.zip", new DownloadFiles.OnDownloadListener() {
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
                                            File FilePath = new File(path);
                                            File TexTure = new File(TexTures);

                                            try {
                                                TexTure.delete();
                                                ZipInput = new ZipInputStream(new FileInputStream(new File(path + "超清底包.zip")));
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
                                                        new File(path + "超清底包.zip").delete();
                                                    } catch (IOException e) {
                                                        AppNotification.error(context, ErrorGet.Log(e));
                                                    }
                                                }
                                            }
                                        }

                                        //下载进度
                                        @Override
                                        public void onDownloading(int progress) {
                                            AppNotification.update(context, "超清底包", String.valueOf(progress) + "%", 20, progress);
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

            //下载剧情包
            if (StoryChecked) {
                new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                final String ApiJson = GameDownload.url(Story, context);
                                JSONObject Json = JSONObject.parseObject(ApiJson);
                                Json = JSONObject.parseObject(Json.getString("data"));
                                final String DownloadURL = Json.getString("raw_url");
                                AppNotification.send(context, "剧情包", "0%", 22);
                                DownloadFiles.get().download(DownloadURL, path, "剧情包.zip", new DownloadFiles.OnDownloadListener() {
                                        //下载完成
                                        @Override
                                        public void onDownloadSuccess() {
                                            //提示正在解压
                                            context.runOnUiThread(new Runnable(){
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(context, "正在解压剧情包", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            
                                            //解压部分
                                            ZipInputStream ZipInput = null;
                                            ZipEntry entry;
                                            File FilePath = new File(path);
                                            File TexTure = new File(TexTures);

                                            try {
                                                TexTure.delete();
                                                ZipInput = new ZipInputStream(new FileInputStream(new File(path + "剧情包.zip")));
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
                                                        AppNotification.delete(context, 22);
                                                        AppNotification.ordinary(context, "剧情包", "下载成功！请到游戏里看看吧", 23, null, false);
                                                        new File(path + "剧情包.zip").delete();
                                                    } catch (IOException e) {
                                                        AppNotification.error(context, ErrorGet.Log(e));
                                                    }
                                                }
                                            }
                                        }

                                        //下载进度
                                        @Override
                                        public void onDownloading(int progress) {
                                            AppNotification.update(context, "剧情包", String.valueOf(progress) + "%", 22, progress);
                                        }

                                        //下载失败
                                        @Override
                                        public void onDownloadFailed() {
                                            //发送通知
                                            AppNotification.ordinary(context, "剧情包", "下载失败，可能是网络问题", 22, null, false);
                                        }
                                    });
                            } catch (Exception e) {
                                AppNotification.error(context, ErrorGet.Log(e));
                            }
                        }
                    }).start();
            }

            //下载模型包
            if (ModelChecked) {
                new Thread(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                final String ApiJson = GameDownload.url(Model, context);
                                JSONObject Json = JSONObject.parseObject(ApiJson);
                                Json = JSONObject.parseObject(Json.getString("data"));
                                final String DownloadURL = Json.getString("raw_url");
                                AppNotification.send(context, "模型包", "0%", 24);
                                DownloadFiles.get().download(DownloadURL, path, "模型包.zip", new DownloadFiles.OnDownloadListener() {
                                        //下载完成
                                        @Override
                                        public void onDownloadSuccess() {
                                            //提示正在解压
                                            context.runOnUiThread(new Runnable(){
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(context, "正在解压模型包", Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                                            //解压部分
                                            ZipInputStream ZipInput = null;
                                            ZipEntry entry;
                                            File FilePath = new File(path);
                                            File TexTure = new File(TexTures);

                                            try {
                                                TexTure.delete();
                                                ZipInput = new ZipInputStream(new FileInputStream(new File(path + "模型包.zip")));
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
                                                        AppNotification.delete(context, 24);
                                                        AppNotification.ordinary(context, "模型包", "下载成功！请到游戏里看看吧", 25, null, false);
                                                        new File(path + "模型包.zip").delete();
                                                    } catch (IOException e) {
                                                        AppNotification.error(context, ErrorGet.Log(e));
                                                    }
                                                }
                                            }
                                        }

                                        //下载进度
                                        @Override
                                        public void onDownloading(int progress) {
                                            AppNotification.update(context, "模型包", String.valueOf(progress) + "%", 24, progress);
                                        }

                                        //下载失败
                                        @Override
                                        public void onDownloadFailed() {
                                            //发送通知
                                            AppNotification.ordinary(context, "模型包", "下载失败，可能是网络问题", 24, null, false);
                                        }
                                    });
                            } catch (Exception e) {
                                AppNotification.error(context, ErrorGet.Log(e));
                            }
                        }
                    }).start();
            }


            //判断有无选择
            if (!SuperChecked && !StoryChecked && !ModelChecked) {
                Toast.makeText(context, "欸？一个也没有？", Toast.LENGTH_SHORT).show();
                XuanZe = true;
            }

            //提示
            if (!XuanZe) {
                Toast.makeText(context, "火力全开！", Toast.LENGTH_SHORT).show();
            }


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
    }
}

package com.mojin.qidong.function;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.hjq.window.EasyWindow;
import com.mojin.qidong.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ToolsWindow {
	private static Activity mActivity;
	private static EasyWindow toolsWindow;

	public static void init(Activity activity){
		mActivity = activity;
		toolsWindow = new EasyWindow<>(activity)
			.setContentView(R.layout.tools_window)
			.setDraggable();
	}

	public static void show(){
		toolsWindow.show();

		new Thread(() -> {
			View contentView = toolsWindow.getContentView();
			TextView pingTextView = contentView.findViewById(R.id.pingText);
			while (true) {  // 持续循环
				try {
					Process p = Runtime.getRuntime().exec("ping -c 1 " + "10.10.0.1");
					BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
					String line;
					while ((line = in.readLine()) != null) {
						if (line.contains("time=")) {
							String latency = line.substring(line.indexOf("time=") + 5, line.indexOf(" ms"));
							mActivity.runOnUiThread(() -> pingTextView.setText(latency + "ms"));
						}
					}
				} catch (IOException e) {
					Log.sendLog(mActivity, e);
				}

				// 每2秒ping一次
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Log.sendLog(mActivity, e);
				}
			}
		}).start();
	}

}

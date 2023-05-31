package com.mojin.qidong.UI.ControlListen;

import android.app.Activity;
import android.widget.Button;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.hjq.http.EasyHttp;
import com.mojin.qidong.R;

public class HomeListener {
	public HomeListener(Activity activity){
		Button downloadButton = activity.findViewById(R.id.downloadButton);
		downloadButton.setOnClickListener(v -> {
		});
	}


package com.mojin.qidong.ui.controlListen;

import android.app.Activity;
import android.widget.Button;

import com.mojin.qidong.R;
import com.mojin.qidong.function.ControlEvent.HomeEvent;

public class HomeListener {
	/**
	 * @noinspection FieldMayBeFinal
	 */
	private Activity mActivity;

	public HomeListener(Activity activity) {
		mActivity = activity;
		HomeEvent.init(mActivity);
	}

	public void start() {
		Button downloadButton = mActivity.findViewById(R.id.downloadButton);
		downloadButton.setOnClickListener(v -> {
			HomeEvent.buttonEvent();
		});
	}
}

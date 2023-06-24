package com.mojin.qidong.ui.controlListen;

import android.app.Activity;
import android.widget.Button;
import android.widget.LinearLayout;

import com.mojin.qidong.R;
import com.mojin.qidong.function.controlEvent.HomeEvent;

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
		LinearLayout installGame = mActivity.findViewById(R.id.installGame);
		installGame.setOnClickListener(v -> {
			HomeEvent.buttonEvent();
		});

		LinearLayout deleteGame = mActivity.findViewById(R.id.deleteGame);
		deleteGame.setOnClickListener(v -> {
			HomeEvent.buttonEvent2();
		});

		LinearLayout optimize = mActivity.findViewById(R.id.optimize);
		optimize.setOnClickListener(v -> {
			HomeEvent.buttonEvent3();
		});
	}
}

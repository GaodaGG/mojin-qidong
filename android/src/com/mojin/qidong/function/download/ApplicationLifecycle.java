package com.mojin.qidong.function.download;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public final class ApplicationLifecycle implements LifecycleOwner {
	private static final ApplicationLifecycle INSTANCE = new ApplicationLifecycle();

	public static ApplicationLifecycle getInstance() {
		return INSTANCE;
	}

	private final LifecycleRegistry mLifecycle = new LifecycleRegistry(this);

	private ApplicationLifecycle() {}

	@NonNull
	@Override
	public Lifecycle getLifecycle() {
		return mLifecycle;
	}
}

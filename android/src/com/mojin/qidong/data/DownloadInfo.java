package com.mojin.qidong.data;

import android.app.PendingIntent;

import java.io.File;

public class DownloadInfo {
	private String mCloudPath;
	private String mName;
	private File mFile;
	private int mNotificationID;
	private PendingIntent mPendingIntent;

	public DownloadInfo setCloudPath(String cloudPath){
		mCloudPath = cloudPath;
		return this;
	}

	public DownloadInfo setName(String name){
		mName = name;
		return this;
	}

	public DownloadInfo setFile(File file){
		mFile = file;
		return this;
	}

	public DownloadInfo setPendingIntent(PendingIntent pendingIntent) {
		mPendingIntent = pendingIntent;
		return this;
	}

	public DownloadInfo setNotificationID(int notificationID) {
		mNotificationID = notificationID;
		return this;
	}
	public String getCloudPath(){
		return mCloudPath;
	}

	public String getName(){
		return mName;
	}

	public File getFile(){
		return mFile;
	}

	public PendingIntent getPendingIntent() {
		return mPendingIntent;
	}

	public int getNotificationID() {
		return mNotificationID;
	}

}

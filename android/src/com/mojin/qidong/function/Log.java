package com.mojin.qidong.function;

import android.content.Context;

import com.mojin.qidong.function.notification.AppNotification;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Log {
	public static void sendLog(Context context, Throwable e){
		AppNotification.error(context, getLogText(e));
	}

	private static String getLogText(Throwable e) {
		if (e != null) {
			StackTraceElement[] stackTrace = e.getStackTrace();
			int lineNumber = stackTrace[0].getLineNumber();
			String methodName = stackTrace[0].getMethodName();

			//详细报错内容
			StringWriter trace = new StringWriter();
			e.printStackTrace(new PrintWriter(trace));
			String detailedLog = trace.toString();

			return "报错原因：" + e.getLocalizedMessage() + "\n"
					+ "所在类：" + stackTrace[0].getClassName() + "\n"
					+ "所在方法：" + methodName + "\n"
					+ "所在行：" + lineNumber + "\n"
					+ "详细报错：" + detailedLog;
		}
		return "没有获取到报错";
	}
}

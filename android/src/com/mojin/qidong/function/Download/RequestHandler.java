package com.mojin.qidong.function.Download;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.hjq.http.EasyLog;
import com.hjq.http.config.IRequestHandler;
import com.hjq.http.exception.*;
import com.hjq.http.request.HttpRequest;
import com.mojin.qidong.R;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class RequestHandler implements IRequestHandler {
	Application mApplication;

	public RequestHandler(Application application){
		mApplication = application;
	}
	@NonNull
	@Override
	public Object requestSuccess(@NonNull HttpRequest<?> httpRequest, @NonNull Response response, @NonNull Type type) throws Exception {
		if (Response.class.equals(type)) {
			return response;
		}

		if (!response.isSuccessful()) {
			throw new ResponseException(String.format(mApplication.getString(R.string.http_response_error),
				response.code(), response.message()), response);
		}

		if (Headers.class.equals(type)) {
			return response.headers();
		}

		ResponseBody body = response.body();
		if (body == null) {
			throw new NullBodyException(mApplication.getString(R.string.http_response_null_body));
		}

		if (ResponseBody.class.equals(type)) {
			return body;
		}

		// 如果是用数组接收，判断一下是不是用 byte[] 类型进行接收的
		if(type instanceof GenericArrayType) {
			Type genericComponentType = ((GenericArrayType) type).getGenericComponentType();
			if (byte.class.equals(genericComponentType)) {
				return body.bytes();
			}
		}

		if (InputStream.class.equals(type)) {
			return body.byteStream();
		}

		if (Bitmap.class.equals(type)) {
			return BitmapFactory.decodeStream(body.byteStream());
		}

		String text;
		try {
			text = body.string();
		} catch (IOException e) {
			// 返回结果读取异常
			throw new DataException(mApplication.getString(R.string.http_data_explain_error), e);
		}

		// 打印这个 Json 或者文本
		EasyLog.printJson(httpRequest, text);

		if (String.class.equals(type)) {
			return text;
		}

		final Object result;

		try {
			result = JSONObject.parseObject(text, type);
		} catch (JSONException e) {
			// 返回结果读取异常
			throw new DataException(mApplication.getString(R.string.http_data_explain_error), e);
		}

		if (result instanceof HttpData) {
			HttpData<?> model = (HttpData<?>) result;
			model.setHeaders(response.headers());

			if (model.isRequestSuccess()) {
				// 代表执行成功
				return result;
			}
		}
		return result;
	}

	@NonNull
	@Override
	public Exception requestFail(@NonNull HttpRequest<?> httpRequest, @NonNull Exception e) {
		if (e instanceof SocketTimeoutException) {
			return new TimeoutException(mApplication.getString(R.string.http_server_out_time), e);
		}

		if (e instanceof UnknownHostException) {
			NetworkInfo info = ((ConnectivityManager) mApplication.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			// 判断网络是否连接
			if (info != null && info.isConnected()) {
				// 有连接就是服务器的问题
				return new ServerException(mApplication.getString(R.string.http_server_error), e);
			}
			// 没有连接就是网络异常
			return new NetworkException(mApplication.getString(R.string.http_network_error), e);
		}

		if (e instanceof IOException) {
			// 出现该异常的两种情况
			// 1. 调用 EasyHttp.cancel
			// 2. 网络请求被中断
			return new CancelException(mApplication.getString(R.string.http_request_cancel), e);
		}

		return new HttpException(e.getMessage(), e);
	}

	@NonNull
	@Override
	public Exception downloadFail(@NonNull HttpRequest<?> httpRequest, @NonNull Exception e) {
		if (e instanceof ResponseException) {
			ResponseException responseException = ((ResponseException) e);
			Response response = responseException.getResponse();
			responseException.setMessage(String.format(mApplication.getString(R.string.http_response_error),
				response.code(), response.message()));
			return responseException;
		} else if (e instanceof NullBodyException) {
			NullBodyException nullBodyException = ((NullBodyException) e);
			nullBodyException.setMessage(mApplication.getString(R.string.http_response_null_body));
			return nullBodyException;
		} else if (e instanceof FileMd5Exception) {
			FileMd5Exception fileMd5Exception = ((FileMd5Exception) e);
			fileMd5Exception.setMessage(mApplication.getString(R.string.http_response_md5_error));
			return fileMd5Exception;
		}
		return requestFail(httpRequest, e);
	}
}

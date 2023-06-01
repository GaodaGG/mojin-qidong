package com.mojin.qidong.function.Download;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.hjq.http.EasyLog;
import com.hjq.http.config.IRequestHandler;
import com.hjq.http.exception.DataException;
import com.hjq.http.exception.NullBodyException;
import com.hjq.http.exception.ResponseException;
import com.hjq.http.request.HttpRequest;
import com.mojin.qidong.R;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

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
		return null;
	}
}

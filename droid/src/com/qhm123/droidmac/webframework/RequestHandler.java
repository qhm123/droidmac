package com.qhm123.droidmac.webframework;

import android.content.Context;

import com.qhm123.droidmac.libs.NanoHTTPD.Response;

public abstract class RequestHandler {

	private Context mContext;

	public void setContext(Context context) {
		mContext = context;
	}

	public Context getContext() {
		return mContext;
	}

	public RequestHandler() {
	}

	public abstract Response handle(Request requset) throws Exception;
}
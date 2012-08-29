package com.qhm123.droidmac.web;

import com.qhm123.droidmac.api.app.AppReader;
import com.qhm123.droidmac.libs.NanoHTTPD;
import com.qhm123.droidmac.libs.NanoHTTPD.Response;
import com.qhm123.droidmac.webframework.Request;
import com.qhm123.droidmac.webframework.RequestHandler;

public class AppHandler extends RequestHandler {

	@Override
	public Response handle(Request requset) throws Exception {
		AppReader appReader = new AppReader(getContext());
		String msg = appReader.getAppsInfo();
		return new Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_JSON, msg);
	}

}
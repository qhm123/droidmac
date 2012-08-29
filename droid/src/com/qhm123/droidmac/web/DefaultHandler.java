package com.qhm123.droidmac.web;

import com.qhm123.droidmac.libs.NanoHTTPD;
import com.qhm123.droidmac.libs.NanoHTTPD.Response;
import com.qhm123.droidmac.webframework.Request;
import com.qhm123.droidmac.webframework.RequestHandler;

public class DefaultHandler extends RequestHandler {

	@Override
	public Response handle(Request requset) {
		String msg = "uri not resolved";
		return new Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_JSON, msg);
	}

}
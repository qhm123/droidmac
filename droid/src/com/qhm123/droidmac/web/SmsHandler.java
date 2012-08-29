package com.qhm123.droidmac.web;

import org.json.JSONArray;

import com.qhm123.droidmac.api.sms.SMSReader;
import com.qhm123.droidmac.libs.NanoHTTPD;
import com.qhm123.droidmac.libs.NanoHTTPD.Response;
import com.qhm123.droidmac.webframework.Request;
import com.qhm123.droidmac.webframework.RequestHandler;

public class SmsHandler extends RequestHandler {
	@Override
	public Response handle(Request requset) throws Exception {
		SMSReader smsReader = new SMSReader(getContext());
		String msg = smsReader.execute("get", new JSONArray());
		return new Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_JSON, msg);
	}
}
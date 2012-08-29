package com.qhm123.droidmac.web;

import org.json.JSONException;

import com.qhm123.droidmac.api.contact.ContactAPI;
import com.qhm123.droidmac.libs.NanoHTTPD;
import com.qhm123.droidmac.libs.NanoHTTPD.Response;
import com.qhm123.droidmac.webframework.Request;
import com.qhm123.droidmac.webframework.RequestHandler;

public class ContactHandler extends RequestHandler {
	@Override
	public Response handle(Request requset) throws JSONException {
		String msg = ContactAPI.getContacts(getContext());
		return new Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_JSON, msg);
	}
}
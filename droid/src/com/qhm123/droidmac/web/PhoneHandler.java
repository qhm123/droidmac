package com.qhm123.droidmac.web;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;

import com.qhm123.droidmac.libs.NanoHTTPD;
import com.qhm123.droidmac.libs.NanoHTTPD.Response;
import com.qhm123.droidmac.webframework.Request;
import com.qhm123.droidmac.webframework.RequestHandler;

public class PhoneHandler extends RequestHandler {

	@Override
	public Response handle(Request requset) {
		String msg = phone();
		return new Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_JSON, msg);
	}

	private String phone() {
		JSONObject jo = new JSONObject();
		try {
			jo.put("MODEL", Build.MODEL);
			jo.put("TIME", Build.TIME);
			jo.put("BOARD", Build.BOARD);
			jo.put("CPU_ABI", Build.CPU_ABI);
			jo.put("DEVICE", Build.DEVICE);
			jo.put("DISPLAY", Build.DISPLAY);
			jo.put("MANUFACTURER", Build.MANUFACTURER);
			jo.put("ID", Build.ID);
			jo.put("SDK_INT", Build.VERSION.SDK_INT);
			jo.put("CPU_ABI2", Build.CPU_ABI2);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String msg = jo.toString();
		return msg;
	}

}
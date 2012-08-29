package com.qhm123.droidmac.web;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.qhm123.droidmac.api.media.MediaReader;
import com.qhm123.droidmac.libs.NanoHTTPD;
import com.qhm123.droidmac.libs.NanoHTTPD.Response;
import com.qhm123.droidmac.webframework.Request;
import com.qhm123.droidmac.webframework.RequestHandler;

public class AudioHandler extends RequestHandler {

	@Override
	public Response handle(Request requset) throws JSONException,
			FileNotFoundException {
		MediaReader mediaReader = new MediaReader(getContext());
		String msg;
		if (requset.groups == null || requset.groups.length == 0) {
			msg = mediaReader.readAudio(null);
			return new Response(NanoHTTPD.HTTP_OK, NanoHTTPD.MIME_JSON, msg);
		} else {
			String id = requset.groups[0];
			msg = mediaReader.readAudio(id);
			JSONArray ja = new JSONArray(msg);
			JSONObject jo = ja.getJSONObject(0);
			String data = jo.getString("data");

			InputStream is = new FileInputStream(data);
			return new NanoHTTPD.Response(NanoHTTPD.HTTP_OK, "audio/ogg",
					is);
		}
	}
}
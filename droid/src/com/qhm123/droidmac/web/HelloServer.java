package com.qhm123.droidmac.web;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

import com.qhm123.droidmac.libs.NanoHTTPD;
import com.qhm123.droidmac.webframework.Request;
import com.qhm123.droidmac.webframework.RequestHandler;

public class HelloServer extends NanoHTTPD {

	private static final String TAG = "HelloServer";

	private List<Router> mRouters = new ArrayList<Router>();

	private Context mContext;

	private class Router {
		public String mPattern;
		public Class<? extends RequestHandler> mHandlerClass;

		public Router(String pattern, Class<? extends RequestHandler> handler) {
			mPattern = pattern;
			mHandlerClass = handler;
		}
	}

	public HelloServer(Context context) throws IOException {
		super(8080, new File("/unused"));
		mContext = context;
	}

	public void setRouters(List<Router> routers) {
		mRouters = routers;
	}

	public void regRouter(String pattern,
			Class<? extends RequestHandler> handler) {
		Router r = new Router(pattern, handler);
		mRouters.add(r);
	}

	public Response serve(String uri, String method, Properties header,
			Properties parms, Properties files) {
		Log.d(TAG, "uri: " + uri);

		RequestHandler handler = null;
		String[] groups = null;
		for (Router router : mRouters) {
			Pattern pattern = Pattern.compile(router.mPattern);
			Matcher matcher = pattern.matcher(uri);
			if (matcher.matches()) {
				groups = new String[matcher.groupCount()];
				for (int i = 1; i < matcher.groupCount(); i++) {
					String group = matcher.group(i);
					groups[i - 1] = group;
				}

				try {
					Log.d(TAG, "handler: " + router.mHandlerClass.getName());

					Constructor<? extends RequestHandler> ctor = router.mHandlerClass
							.getDeclaredConstructor();
					handler = (RequestHandler) ctor.newInstance();
					handler.setContext(mContext);
					break;
				} catch (Exception e) {
					return new NanoHTTPD.Response(HTTP_INTERNALERROR, "", "");
				}
			}
		}

		if (handler != null) {
			try {
				return handler.handle(new Request(uri, method, header, parms,
						files, groups));
			} catch (Exception e) {
				return new NanoHTTPD.Response(HTTP_INTERNALERROR, "", "");
			}
		} else {
			String msg = "uri not resolved";
			return new NanoHTTPD.Response(HTTP_OK, MIME_JSON, msg);
		}
	}
}
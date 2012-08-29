package com.qhm123.droidmac.web;

import java.io.IOException;

import android.content.Context;


public class WebStartup {

	public static void start(Context context) {
		try {
			HelloServer server = new HelloServer(context);
			server.regRouter("/phone", PhoneHandler.class);
			server.regRouter("/contacts", ContactHandler.class);
			server.regRouter("/contact/(.*)", ContactHandler.class);
			server.regRouter("/smss", SmsHandler.class);
			server.regRouter("/sms/(.*)", SmsHandler.class);
			server.regRouter("/apps", AppHandler.class);
			server.regRouter("/app/(.*)", AppHandler.class);
			server.regRouter("/videos", VideoHandler.class);
			server.regRouter("/video/(.*)", VideoHandler.class);
			server.regRouter("/audios", AudioHandler.class);
			server.regRouter("/audio/(.*)", AudioHandler.class);
			server.regRouter("/images", ImageHandler.class);
			server.regRouter("/image/(.*)", ImageHandler.class);
			server.regRouter(".*", DefaultHandler.class);
		} catch (IOException ioe) {
			System.err.println("Couldn't start server:\n" + ioe);
			System.exit(-1);
		}
		System.out.println("Listening on port 8080. \n");
	}
}

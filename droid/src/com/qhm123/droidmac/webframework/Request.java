package com.qhm123.droidmac.webframework;

import java.util.Properties;

public class Request {
	public String uri;
	public String method;
	public Properties header;
	public Properties parms;
	public Properties files;
	public String[] groups;

	public Request(String uri, String method, Properties header,
			Properties parms, Properties files, String[] groups) {
		this.uri = uri;
		this.method = method;
		this.header = header;
		this.parms = parms;
		this.files = files;
		this.groups = groups;
	}
}
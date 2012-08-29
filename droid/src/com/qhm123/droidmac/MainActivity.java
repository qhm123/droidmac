package com.qhm123.droidmac;

import android.app.Activity;
import android.os.Bundle;

import com.qhm123.droidmac.web.WebStartup;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		WebStartup.start(this);
	}
}

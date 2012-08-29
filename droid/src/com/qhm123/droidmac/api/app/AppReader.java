package com.qhm123.droidmac.api.app;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

public class AppReader {

	private Context mContext;

	public AppReader(Context context) {
		mContext = context;
	}

	public String getAppsInfo() throws JSONException {
		JSONArray ja = new JSONArray();

		List<PackageInfo> packageInfos = mContext.getPackageManager()
				.getInstalledPackages(PackageManager.GET_PERMISSIONS);
		for (PackageInfo info : packageInfos) {
			JSONObject jo = new JSONObject();
			jo.put("packageName", info.packageName);
			jo.put("versionCode", info.versionCode);
			jo.put("versionName", info.versionName);
			jo.put("isSystem",
					(info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
			jo.put("name", info.applicationInfo.name);
			jo.put("label",
					info.applicationInfo
							.loadLabel(mContext.getPackageManager()).toString());
			Drawable icon = info.applicationInfo.loadIcon(mContext
					.getPackageManager());

			BitmapDrawable bitDw = ((BitmapDrawable) icon);
			Bitmap bitmap = bitDw.getBitmap();
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			byte[] bitmapByte = stream.toByteArray();
			String iconString = new String(Base64.encode(bitmapByte,
					Base64.DEFAULT));
			jo.put("versionName", iconString);

			JSONArray jaPms = new JSONArray();
			PermissionInfo[] pms = info.permissions;
			if (pms != null) {
				for (PermissionInfo pm : pms) {
					jaPms.put(pm.name);
				}
				jo.put("permissions", jaPms);
			}

			ja.put(jo);
		}

		return ja.toString();
	}
}

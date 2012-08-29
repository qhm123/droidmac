package com.qhm123.droidmac.api.sms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.util.Log;

public class SMSReader {

	private Context mCtx;

	public SMSReader(Context ctx) {
		mCtx = ctx;
	}

	private ContentResolver getContentResolver() {
		return mCtx.getContentResolver();
	}

	public String execute(String action, JSONArray data) {
		Log.d("SMSReadPlugin", "Plugin Called");

		JSONObject messages = new JSONObject();
		if (action.equals("get")) {
			try {
				JSONObject filter = data.optJSONObject(0);
				JSONObject options = data.optJSONObject(1);
				messages = readSMS(options, filter);
				Log.d("SMSReadPlugin", "Returning " + messages.toString());

				return messages.toString();
			} catch (JSONException jsonEx) {
				Log.d("SMSReadPlugin",
						"Got JSON Exception " + jsonEx.getMessage());
			}
		} else if (action.equals("delete")) {
			try {
				String id = data.getString(0);
				try {
					Integer.parseInt(id);
				} catch (Exception e) {
				}
				int rowEffect = getContentResolver().delete(
						Uri.parse("content://sms"), "_id = " + id, null);
				if (rowEffect > 0) {
				} else {
				}
			} catch (JSONException e) {
				Log.d("SMSReadPlugin", "Got JSON Exception " + e.getMessage());
			}
		} else if (action.equals("insert")) {
			try {
				JSONObject parmas = data.getJSONObject(0);
				ContentValues cv = new ContentValues();
				if (parmas.has("thread_id")) {
					cv.put("thread_id", parmas.optString("thread_id"));
				}
				cv.put("address", parmas.optString("number"));
				if (parmas.has("person_id")) {
					cv.put("person", parmas.optString("person_id"));
				}
				cv.put("body", parmas.optString("text"));
				// cv.put("date", parmas.optString("date"));
				// cv.put("_id", "999");
				// cv.put("protocol", "0");
				// cv.put("read", "1");
				// cv.put("status", "-1");

				String type = parmas.optString("type");
				if (type == null || type.equals("all") || !inSmsType(type)) {
					type = "";
				}
				Uri ret = getContentResolver().insert(
						Uri.parse("content://sms/" + type), cv);
				if (ret == null) {
				} else {
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			Log.d("SMSReadPlugin", "Invalid action : " + action + " passed");
		}

		return "";
	}

	private boolean isFilter(String filterName) {
		String[] filterList = { "thread_id", "person", "_id" };
		for (int j = 0; j < filterList.length; j++) {
			if (filterList[j].equals(filterName)) {
				return true;
			}
		}

		return false;
	}

	private boolean isOrder(String orderName) {
		String[] order = orderName.split(" ");
		if (order.length != 2) {
			return false;
		}

		boolean isOrderKey = false;
		String[] orderList = { "date", "_id", "person" };
		for (int j = 0; j < orderList.length; j++) {
			if (orderList[j].equals(order[0])) {
				isOrderKey = true;
				break;
			}
		}
		if (!isOrderKey) {
			return false;
		}
		String[] orderDesc = { "asc", "desc" };
		for (int j = 0; j < orderDesc.length; j++) {
			if (orderDesc[j].equals(order[1])) {
				return true;
			}
		}

		return false;
	}

	private JSONObject readSMS(JSONObject options, JSONObject filter)
			throws JSONException {
		JSONObject data = new JSONObject();

		// 拼接查询
		String selection = null;
		String[] selectionArgs = null;
		if (filter != null) {
			StringBuilder sb = new StringBuilder();
			selectionArgs = new String[filter.length()];
			JSONArray names = filter.names();
			for (int i = 0; i < names.length(); i++) {
				String optionName = names.get(i).toString();
				// 过滤掉不符合的filter参数
				if (!isFilter(optionName)) {
					continue;
				}
				String option = filter.getString(optionName);
				if (!TextUtils.isEmpty(sb.toString())) {
					sb.append(" AND ");
				}
				sb.append(optionName).append(" = ?");
				selectionArgs[i] = option;
			}
			selection = sb.toString();
		}

		// 拼接options
		String op = null;
		String type = "";
		if (options != null) {
			StringBuilder sb = new StringBuilder();
			String order = options.optString("order");
			if (!isOrder(order)) {
				order = "";
			}
			if (TextUtils.isEmpty(order)) {
				order = "_id asc";
			}
			long start = options.optLong("start");
			long count = options.optLong("count");
			if (count == 0) {
				count = 1000;
			}
			op = sb.append(order).append(" limit ").append(start).append(",")
					.append(count).toString();

			type = options.optString("type");
			if (type == null || type.equals("all") || !inSmsType(type)) {
				type = "";
			}
		}

		Uri uriSMSURI = Uri.parse("content://sms/" + type);

		Cursor cur = getContentResolver().query(uriSMSURI, null, selection,
				selectionArgs, op);
		JSONArray smsList = new JSONArray();
		data.put("messages", smsList);
		while (cur.moveToNext()) {
			JSONObject sms = new JSONObject();
			sms.put("_id", cur.getString(cur.getColumnIndex("_id")));
			sms.put("number", cur.getString(cur.getColumnIndex("address")));
			sms.put("text", cur.getString(cur.getColumnIndex("body")));
			sms.put("read", cur.getString(cur.getColumnIndex("read")));
			sms.put("protocol", cur.getString(cur.getColumnIndex("protocol")));
			sms.put("thread_id", cur.getString(cur.getColumnIndex("thread_id")));
			sms.put("date", cur.getString(cur.getColumnIndex("date")));
			sms.put("person_id", cur.getString(cur.getColumnIndex("person")));
			sms.put("person_name",
					getContact(cur.getString(cur.getColumnIndex("address"))));
			smsList.put(sms);
		}
		return data;
	}

	private boolean inSmsType(String folder) {
		String[] types = new String[] { "inbox", "sent", "draft", "failed",
				"queued", "sim", "all" };
		boolean isTypeIn = false;
		for (int i = 0; i < types.length; i++) {
			if (types[i].equals(folder)) {
				isTypeIn = true;
				break;
			}
		}
		return isTypeIn;
	}

	private String getContact(String number) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));
		Cursor cur = getContentResolver().query(uri,
				new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);

		if (cur.moveToFirst()) {
			String name = cur.getString(cur
					.getColumnIndex(PhoneLookup.DISPLAY_NAME));
			cur.close();
			return name;
		}
		return null;
	}

}

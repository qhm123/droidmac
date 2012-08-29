package com.qhm123.droidmac.api.media;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class MediaReader {

	private Context mContext;

	public MediaReader(Context context) {
		mContext = context;
	}

	public String readImage(String id) throws JSONException {
		Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

		String selection = null;
		String[] selectionArgs = null;
		if (id != null) {
			selection = MediaStore.Images.Media._ID + "=?";
			selectionArgs = new String[] { id };
		}

		JSONArray ja = new JSONArray();

		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(uri, new String[] {
				MediaStore.Images.Media._ID, MediaStore.Images.Media.TITLE,
				MediaStore.Images.Media.DATA,
				MediaStore.Images.Media.DISPLAY_NAME }, selection,
				selectionArgs, MediaStore.Images.Media.DEFAULT_SORT_ORDER);
		while (cursor.moveToNext()) {
			JSONObject joMedia = new JSONObject();
			joMedia.put("id", cursor.getString(cursor
					.getColumnIndex(MediaStore.Images.Media._ID)));
			joMedia.put("title", cursor.getString(cursor
					.getColumnIndex(MediaStore.Images.Media.TITLE)));
			joMedia.put("data", cursor.getString(cursor
					.getColumnIndex(MediaStore.Images.Media.DATA)));
			joMedia.put("displayName", cursor.getString(cursor
					.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)));

			ja.put(joMedia);
		}

		return ja.toString();
	}

	public String readVideo(String id) throws JSONException {
		Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

		String selection = null;
		String[] selectionArgs = null;
		if (id != null) {
			selection = MediaStore.Video.Media._ID + "=?";
			selectionArgs = new String[] { id };
		}

		JSONArray ja = new JSONArray();

		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(uri, new String[] {
				MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE,
				MediaStore.Video.Media.DATA,
				MediaStore.Video.Media.DISPLAY_NAME }, selection,
				selectionArgs, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
		while (cursor.moveToNext()) {
			JSONObject joMedia = new JSONObject();
			joMedia.put("id", cursor.getString(cursor
					.getColumnIndex(MediaStore.Video.Media._ID)));
			joMedia.put("title", cursor.getString(cursor
					.getColumnIndex(MediaStore.Video.Media.TITLE)));
			joMedia.put("data", cursor.getString(cursor
					.getColumnIndex(MediaStore.Video.Media.DATA)));
			joMedia.put("displayName", cursor.getString(cursor
					.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)));

			ja.put(joMedia);
		}

		return ja.toString();
	}

	public String readAudio(String id) throws JSONException {
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		String selection = null;
		String[] selectionArgs = null;
		if (id != null) {
			selection = MediaStore.Audio.Media._ID + "=?";
			selectionArgs = new String[] { id };
		}

		JSONArray ja = new JSONArray();

		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(uri, new String[] {
				MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.DISPLAY_NAME }, selection,
				selectionArgs, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		while (cursor.moveToNext()) {
			JSONObject joMedia = new JSONObject();
			joMedia.put("id", cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media._ID)));
			joMedia.put("title", cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE)));
			joMedia.put("data", cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DATA)));
			joMedia.put("displayName", cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)));

			ja.put(joMedia);
		}

		return ja.toString();
	}
}

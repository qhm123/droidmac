package com.qhm123.droidmac.api.contact;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class ContactAPI {

	public static String getContacts(Context ctx) throws JSONException {

		String where = "(" + ContactsContract.RawContacts.ACCOUNT_TYPE
				+ " NOT LIKE " + "'%sim%'" + " or "
				+ ContactsContract.RawContacts.ACCOUNT_TYPE
				+ " is null) AND deleted = 0";

		final Cursor allContacts = ctx.getContentResolver().query(
				RawContacts.CONTENT_URI, new String[] { RawContacts._ID },
				where, null, null);

		if (allContacts == null) {
			return "";
		}

		if (allContacts != null && !allContacts.moveToFirst()) {
			allContacts.close();
			return "";
		}

		final ContentResolver cResolver = ctx.getContentResolver();
		Contact parseContact = new Contact();
		JSONObject jo = new JSONObject();
		jo.put("count", allContacts.getCount());
		JSONArray contactsArray = new JSONArray();
		try {
			boolean hasNext = true;
			do {
				JSONObject contactItem = new JSONObject();
				parseContact.populate(allContacts, cResolver);
				// parseContact.writeVCard(vcfBuffer);

				contactItem.put("lastName", parseContact.lastName);
				contactItem.put("firstName", parseContact.firstName);
				contactItem.put("nickName", parseContact.nickName);
				contactItem.put("displayName", parseContact.displayName);

				if (parseContact.photo != null) {
					StringBuilder sb = new StringBuilder();
					Base64Coder.mimeEncode(sb, parseContact.photo, 76, "\n");
					contactItem.put("photo", sb.toString());
				}

				class RowDataToString {

					List<Contact.RowData> mData;

					public RowDataToString(List<Contact.RowData> data) {
						mData = data;
					}

					public JSONArray getJSONArray() {
						JSONArray jaPhone = new JSONArray();
						for (Contact.RowData row : mData) {
							JSONObject joPhone = new JSONObject();
							try {
								joPhone.put("type", row.type);
								joPhone.put("data", row.data);
								
								jaPhone.put(joPhone);
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}

						return jaPhone;
					}
				}

				contactItem.put("phones", new RowDataToString(
						parseContact.phones).getJSONArray());
				contactItem.put("emails", new RowDataToString(
						parseContact.emails).getJSONArray());
				contactItem.put("urls",
						new RowDataToString(parseContact.urls).getJSONArray());
				contactItem.put("addrs",
						new RowDataToString(parseContact.addrs).getJSONArray());
				contactItem.put("ims",
						new RowDataToString(parseContact.ims).getJSONArray());

				try {
					JSONArray jaOrgs = new JSONArray();
					for (Contact.OrgData row : parseContact.orgs) {
						JSONObject joPhone = new JSONObject();
						try {
							joPhone.put("type", row.type);
							joPhone.put("data", row.title);
							joPhone.put("company", row.company);
							
							jaOrgs.put(joPhone);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					contactItem.put("orgs", jaOrgs);
				} catch (Exception e) {
				}

				// parseContact.writeVCard(vcfBuffer);

				hasNext = allContacts.moveToNext();

				contactsArray.put(contactItem);
			} while (hasNext);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (allContacts != null) {
				allContacts.close();
			}
		}

		jo.put("contacts", contactsArray);

		return jo.toString();
	}

	/*
	 * public static final int BACK_UP_SUCCEED = 0; public static final int
	 * BACK_UP_FAILED = 1; public static final int RECOVER_SUCCEED = 2; public
	 * static final int RECOVER_FAILED = 3; // vdisk.contact.backup.vcf public
	 * static final String BACKUP_PATH = Environment
	 * .getExternalStorageDirectory() + "/" + Constants.FAV_FOLDER; public
	 * static final String CONTACT_CACHE_PATH = "/vdisk.contact.backup.vcf";
	 * public static final String CONTACT_TAG_OPERATION_TIME = "operation_time";
	 * public static final String CONTACT_PREF = "vdisk_contact";
	 * 
	 * public static Object[] doExport(Context ctx, final String fileName) { try
	 * { File file = new File(fileName); if (!file.exists()) { file.mkdirs(); }
	 * File vcfFile = new File(file.getAbsolutePath() + CONTACT_CACHE_PATH);
	 * 
	 * final BufferedWriter vcfBuffer = new BufferedWriter( new
	 * FileWriter(vcfFile.getAbsolutePath()));
	 * 
	 * final ContentResolver cResolver = ctx.getContentResolver();
	 * 
	 * String where = "(" + ContactsContract.RawContacts.ACCOUNT_TYPE +
	 * " NOT LIKE " + "'%sim%'" + " or " +
	 * ContactsContract.RawContacts.ACCOUNT_TYPE + " is null) AND deleted = 0";
	 * 
	 * final Cursor allContacts = ctx.getContentResolver().query(
	 * RawContacts.CONTENT_URI, new String[] { RawContacts._ID }, where, null,
	 * null);
	 * 
	 * if (allContacts == null) { return new Object[] { BACK_UP_FAILED,
	 * ctx.getResources().getString( R.string.contact_no_contact_find) }; }
	 * 
	 * if (allContacts != null && !allContacts.moveToFirst()) {
	 * allContacts.close(); return new Object[] { BACK_UP_FAILED,
	 * ctx.getResources().getString( R.string.contact_no_contact_find) }; }
	 * 
	 * final long maxlen = allContacts.getCount();
	 * 
	 * // Start lengthy operation in a background thread long exportStatus = 0;
	 * 
	 * Contact parseContact = new Contact(); try { boolean hasNext = true; do {
	 * parseContact.populate(allContacts, cResolver);
	 * parseContact.writeVCard(vcfBuffer);
	 * 
	 * ++exportStatus;
	 * 
	 * int progress = (int) (100 * exportStatus / maxlen);
	 * 
	 * // Update the progress bar Log.d("Contact", "progress: "+ progress);
	 * hasNext = allContacts.moveToNext(); } while (hasNext);
	 * 
	 * } catch (Exception e) { e.printStackTrace(); return new Object[] {
	 * BACK_UP_FAILED, ctx.getResources().getString(
	 * R.string.contact_backup_failed) }; } finally { if (vcfBuffer != null) {
	 * vcfBuffer.close(); } if (allContacts != null) { allContacts.close(); } }
	 * upload(ctx, vcfFile);// 上传vcard文件 vcfFile.delete();// 删除缓存文件
	 * updateSharePreference(ctx); return new Object[] { BACK_UP_SUCCEED,
	 * ctx.getResources().getString( R.string.contact_backup_succeed) }; } catch
	 * (Exception e) { e.printStackTrace(); return new Object[] {
	 * BACK_UP_FAILED, ctx.getResources()
	 * .getString(R.string.contact_backup_failed) }; } }
	 * 
	 * public static Object[] doImport(Context ctx, final String fileName, final
	 * boolean replace) { try {
	 * 
	 * download(ctx);
	 * 
	 * File vcfFile = new File(fileName + ContactAPI.CONTACT_CACHE_PATH);
	 * 
	 * final BufferedReader vcfBuffer = new BufferedReader( new
	 * FileReader(vcfFile.getAbsolutePath()));
	 * 
	 * final long maxlen = vcfFile.length();
	 * 
	 * ArrayList<ContentProviderOperation> ops = new
	 * ArrayList<ContentProviderOperation>(); ContentResolver cResolver =
	 * ctx.getContentResolver(); String where = "(" +
	 * ContactsContract.RawContacts.ACCOUNT_TYPE + " NOT LIKE " + "'%sim%'" +
	 * " or " + ContactsContract.RawContacts.ACCOUNT_TYPE +
	 * " is null) AND deleted = 0"; cResolver.delete(Data.CONTENT_URI, null,
	 * null); cResolver.delete( RawContacts.CONTENT_URI .buildUpon()
	 * .appendQueryParameter( ContactsContract.CALLER_IS_SYNCADAPTER,
	 * "true").build(), where, null); //
	 * cResolver.delete(ContactsContract.RawContacts.CONTENT_URI, // where,
	 * null); long importStatus = 0; Contact parseContact = new Contact(); try {
	 * long ret = 0; int count = 0; do { ret =
	 * parseContact.parseVCard(vcfBuffer); if (ret >= 0) {
	 * parseContact.addContact(ctx.getApplicationContext(), 0, replace, ops);
	 * importStatus += parseContact.getParseLen();
	 * 
	 * count++; Log.d("Contact", "ret: " + ret + ", importStatus: " +
	 * importStatus + ", maxlen: " + maxlen + ", count: " + count);
	 * 
	 * int progress = (int) (100 * importStatus / maxlen); Log.d("Contact",
	 * "progress: " + progress);
	 * 
	 * }
	 * 
	 * if (ops.size() > 300) { cResolver.applyBatch(ContactsContract.AUTHORITY,
	 * ops); ops.clear(); } } while (ret > 0);
	 * 
	 * if (ops.size() > 0) { cResolver.applyBatch(ContactsContract.AUTHORITY,
	 * ops); ops.clear(); } vcfFile.delete();//删除缓存 return new Object[] {
	 * ContactAPI.RECOVER_SUCCEED, ctx.getResources().getString(
	 * R.string.contact_recover_succeed) }; } catch (IOException e) {
	 * e.printStackTrace(); return new Object[] { ContactAPI.RECOVER_FAILED,
	 * ctx.getResources().getString( R.string.contact_recover_failed) }; } catch
	 * (Exception e) { e.printStackTrace(); return new Object[] {
	 * ContactAPI.RECOVER_FAILED, ctx.getResources().getString(
	 * R.string.contact_recover_failed) }; } finally { if (vcfBuffer != null) {
	 * try { vcfBuffer.close(); } catch (IOException e) { } } }
	 * 
	 * } catch (FileNotFoundException e) { e.printStackTrace(); return new
	 * Object[] { ContactAPI.RECOVER_FAILED, ctx.getResources().getString(
	 * R.string.contact_recover_failed) }; } catch (Exception e) {
	 * e.printStackTrace(); return new Object[] { ContactAPI.RECOVER_FAILED,
	 * ctx.getResources().getString( R.string.contact_recover_failed) }; } }
	 * 
	 * 
	 * 
	 * public static long getLocalNum(Context ctx) {
	 * 
	 * String where = "(" + ContactsContract.RawContacts.ACCOUNT_TYPE +
	 * " NOT LIKE " + "'%sim%'" + " or " +
	 * ContactsContract.RawContacts.ACCOUNT_TYPE + " is null) AND deleted = 0";
	 * 
	 * final Cursor allContacts = ctx.getContentResolver().query(
	 * RawContacts.CONTENT_URI, new String[] { RawContacts._ID }, where, null,
	 * null);
	 * 
	 * if (allContacts == null) { return 0; }
	 * 
	 * if (allContacts != null && !allContacts.moveToFirst()) {
	 * allContacts.close(); return 0; }
	 * 
	 * long maxlen = allContacts.getCount();
	 * 
	 * if (allContacts != null) { allContacts.close(); }
	 * 
	 * return maxlen; }
	 * 
	 * // 上传vcard文件 public static void upload(Context ctx, File file) throws
	 * Exception { FileInputStream fis; fis = new FileInputStream(file);
	 * VDiskApi.uploadContact(ctx, fis, file.length(), getLocalNum(ctx)); }
	 * 
	 * // 下载vcard文件 public static void download(Context ctx) throws
	 * ClientProtocolException, VDiskException, IOException {
	 * VDiskApi.downloadContact(ctx); }
	 * 
	 * private static void updateSharePreference(Context ctx) { Calendar
	 * calendar = Calendar.getInstance();
	 * calendar.setTimeInMillis(System.currentTimeMillis()); Date time =
	 * calendar.getTime();
	 * 
	 * SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	 * String date = formatter.format(time); // 将日期时间格式化
	 * ctx.getSharedPreferences(CONTACT_PREF,
	 * Context.MODE_PRIVATE).edit().putString(CONTACT_TAG_OPERATION_TIME,
	 * date).commit(); }
	 */
}

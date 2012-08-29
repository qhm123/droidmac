package com.qhm123.droidmac.api.contact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

/**
 * A Contact item
 */
public class Contact {
	static final String NL = "\r\n";

	// Property name for Instant-message addresses
	static final String IMPROP = "X-IM-NICK";

	static final String NICKPROP = "X-NICKNAME";

	// Property parameter name for custom labels
	static final String LABEL_PARAM = "LABEL";

	// Property parameter for IM protocol
	static final String PROTO_PARAM = "PROTO";

	static final String BIRTHDAY_FIELD = "Birthday:";

	long parseLen;

	/**
	 * Contact fields declaration
	 */
	// Contact identifier
	String _id;
	// Contact displayed name
	String displayName;
	// Contact first name
	String firstName;
	// Contact last name
	String lastName;

	String nickName;

	static class RowData {
		RowData(int type, String data, boolean preferred, String customLabel) {
			this.type = type;
			this.data = data;
			this.preferred = preferred;
			this.customLabel = customLabel;
			auxData = null;
		}

		RowData(int type, String data, boolean preferred) {
			this(type, data, preferred, null);
		}

		int type;
		String data;
		boolean preferred;
		String customLabel;
		String auxData;
	}

	static class OrgData {
		OrgData(int type, String title, String company, String customLabel) {
			this.type = type;
			this.title = title;
			this.company = company;
			this.customLabel = customLabel;
		}

		int type;

		// Contact title
		String title;
		// Contact company name
		String company;

		String customLabel;
	}

	List<RowData> phones;
	List<RowData> emails;
	List<RowData> addrs;
	List<RowData> ims;
	List<RowData> urls;
	List<OrgData> orgs;
	byte[] photo;

	// not used
	String notes;
	String birthday;

	Hashtable<String, handleProp> propHandlers;

	interface handleProp {
		void parseProp(final String propName, final Vector<String> propVec,
				final String val);
	}

	// Initializer block
	{
		reset();
		propHandlers = new Hashtable<String, handleProp>();

		handleProp simpleValue = new handleProp() {
			public void parseProp(final String propName,
					final Vector<String> propVec, final String val) {
				if (propName.equals("FN")) {
					displayName = val;
				} else if (propName.equals(NICKPROP)) {
					nickName = val;
				} else if (propName.equals("NOTE")) {
					notes = val;
				} else if (propName.equals("BDAY")) {
					birthday = val;
				} else if (propName.equals("N")) {
					String[] names = StringUtil.split(val, ";");
					// We set only the first given name.
					// The others are ignored in input and will not be
					// overridden on the server in output.
					if (names.length >= 2) {
						firstName = names[1];
						lastName = names[0];
					} else {
						String[] names2 = StringUtil.split(names[0], " ");
						firstName = names2[0];
						if (names2.length > 1)
							lastName = names2[1];
					}
				}
			}
		};

		propHandlers.put("FN", simpleValue);
		propHandlers.put("NOTE", simpleValue);
		propHandlers.put("BDAY", simpleValue);
		propHandlers.put("X-IRMC-LUID", simpleValue);
		propHandlers.put("UID", simpleValue);
		propHandlers.put("N", simpleValue);
		propHandlers.put(NICKPROP, simpleValue);

		handleProp orgHandler = new handleProp() {

			@Override
			public void parseProp(String propName, Vector<String> propVec,
					String val) {
				String label = null;
				for (String prop : propVec) {
					String[] propFields = StringUtil.split(prop, "=");
					if (propFields[0].equalsIgnoreCase(LABEL_PARAM)
							&& propFields.length > 1) {
						label = propFields[1];
					}
				}
				if (propName.equals("TITLE")) {
					boolean setTitle = false;
					for (OrgData org : orgs) {
						if (label == null && org.customLabel != null)
							continue;
						if (label != null && !label.equals(org.customLabel))
							continue;

						if (org.title == null) {
							org.title = val;
							setTitle = true;
							break;
						}
					}
					if (!setTitle) {
						orgs.add(new OrgData(
								label == null ? Organization.TYPE_WORK
										: Organization.TYPE_CUSTOM, val, null,
								label));
					}
				} else if (propName.equals("ORG")) {
					String[] orgFields = StringUtil.split(val, ";");
					boolean setCompany = false;
					for (OrgData org : orgs) {
						if (label == null && org.customLabel != null)
							continue;
						if (label != null && !label.equals(org.customLabel))
							continue;

						if (org.company == null) {
							org.company = val;
							setCompany = true;
							break;
						}
					}
					if (!setCompany) {
						orgs.add(new OrgData(
								label == null ? Organization.TYPE_WORK
										: Organization.TYPE_CUSTOM, null,
								orgFields[0], label));
					}
				}
			}
		};

		propHandlers.put("ORG", orgHandler);
		propHandlers.put("TITLE", orgHandler);

		propHandlers.put("TEL", new handleProp() {
			public void parseProp(final String propName,
					final Vector<String> propVec, final String val) {
				String label = null;
				int subtype = Phone.TYPE_OTHER;
				boolean preferred = false;
				for (String prop : propVec) {
					if (prop.equalsIgnoreCase("HOME")
							|| prop.equalsIgnoreCase("VOICE")) {
						if (subtype != Phone.TYPE_FAX_HOME)
							subtype = Phone.TYPE_HOME;
					} else if (prop.equalsIgnoreCase("WORK")) {
						if (subtype == Phone.TYPE_FAX_HOME) {
							subtype = Phone.TYPE_FAX_WORK;
						} else
							subtype = Phone.TYPE_WORK;
					} else if (prop.equalsIgnoreCase("CELL")) {
						subtype = Phone.TYPE_MOBILE;
					} else if (prop.equalsIgnoreCase("FAX")) {
						if (subtype == Phone.TYPE_WORK) {
							subtype = Phone.TYPE_FAX_WORK;
						} else
							subtype = Phone.TYPE_FAX_HOME;
					} else if (prop.equalsIgnoreCase("PAGER")) {
						subtype = Phone.TYPE_PAGER;
					} else if (prop.equalsIgnoreCase("PREF")) {
						preferred = true;
					} else {
						String[] propFields = StringUtil.split(prop, "=");

						if (propFields.length > 1
								&& propFields[0].equalsIgnoreCase(LABEL_PARAM)) {
							label = propFields[1];
							subtype = Phone.TYPE_CUSTOM;
						}
					}
				}
				phones.add(new RowData(subtype, toCanonicalPhone(val),
						preferred, label));
			}
		});

		propHandlers.put("ADR", new handleProp() {
			public void parseProp(final String propName,
					final Vector<String> propVec, final String val) {
				boolean preferred = false;
				String label = null;
				int subtype = StructuredPostal.TYPE_WORK; // vCard
															// spec
															// says
															// default
															// is
															// WORK
				for (String prop : propVec) {
					if (prop.equalsIgnoreCase("WORK")) {
						subtype = StructuredPostal.TYPE_WORK;
					} else if (prop.equalsIgnoreCase("HOME")) {
						subtype = StructuredPostal.TYPE_HOME;
					} else if (prop.equalsIgnoreCase("PREF")) {
						preferred = true;
					} else {
						String[] propFields = StringUtil.split(prop, "=");

						if (propFields.length > 1
								&& propFields[0].equalsIgnoreCase(LABEL_PARAM)) {
							label = propFields[1];
							subtype = StructuredPostal.TYPE_CUSTOM;
						}
					}
				}
				String[] addressFields = StringUtil.split(val, ";");
				StringBuffer addressBuf = new StringBuffer(val.length());
				if (addressFields.length > 2) {
					addressBuf.append(addressFields[2]);
					int maxLen = Math.min(7, addressFields.length);
					for (int i = 3; i < maxLen; ++i) {
						addressBuf.append(", ").append(addressFields[i]);
					}
				}
				String address = addressBuf.toString();
				addrs.add(new RowData(subtype, address, preferred, label));
			}
		});

		propHandlers.put("EMAIL", new handleProp() {
			public void parseProp(final String propName,
					final Vector<String> propVec, final String val) {
				boolean preferred = false;
				String label = null;
				int subtype = Email.TYPE_HOME;
				for (String prop : propVec) {
					if (prop.equalsIgnoreCase("PREF")) {
						preferred = true;
					} else if (prop.equalsIgnoreCase("WORK")) {
						subtype = Email.TYPE_WORK;
					} else {
						String[] propFields = StringUtil.split(prop, "=");

						if (propFields.length > 1
								&& propFields[0].equalsIgnoreCase(LABEL_PARAM)) {
							label = propFields[1];
							subtype = Email.TYPE_CUSTOM;
						}
					}
				}
				emails.add(new RowData(subtype, val, preferred, label));
			}
		});

		propHandlers.put("URL", new handleProp() {
			@Override
			public void parseProp(String propName, Vector<String> propVec,
					String val) {
				boolean preferred = false;
				String label = null;
				int subtype = Website.TYPE_HOME;
				for (String prop : propVec) {
					if (prop.equalsIgnoreCase("PREF")) {
						preferred = true;
					} else if (prop.equalsIgnoreCase("WORK")) {
						subtype = Website.TYPE_WORK;
					} else {
						String[] propFields = StringUtil.split(prop, "=");

						if (propFields.length > 1
								&& propFields[0].equalsIgnoreCase(LABEL_PARAM)) {
							label = propFields[1];
							subtype = Website.TYPE_CUSTOM;
						}
					}
				}
				urls.add(new RowData(subtype, val, preferred, label));
			}
		});

		propHandlers.put(IMPROP, new handleProp() {
			public void parseProp(final String propName,
					final Vector<String> propVec, final String val) {
				boolean preferred = false;
				String label = null;
				String proto = null;
				int subtype = Im.TYPE_HOME;
				for (String prop : propVec) {
					if (prop.equalsIgnoreCase("PREF")) {
						preferred = true;
					} else if (prop.equalsIgnoreCase("WORK")) {
						subtype = Im.TYPE_WORK;
					} else {
						String[] propFields = StringUtil.split(prop, "=");

						if (propFields.length > 1) {
							if (propFields[0].equalsIgnoreCase(PROTO_PARAM)) {
								proto = propFields[1];
							} else if (propFields[0]
									.equalsIgnoreCase(LABEL_PARAM)) {
								label = propFields[1];
							}
						}
					}
				}
				RowData newRow = new RowData(subtype, val, preferred, label);
				newRow.auxData = proto;
				ims.add(newRow);
			}
		});

		propHandlers.put("PHOTO", new handleProp() {
			public void parseProp(final String propName,
					final Vector<String> propVec, final String val) {
				boolean isUrl = false;
				photo = new byte[val.length()];
				for (int i = 0; i < photo.length; ++i)
					photo[i] = (byte) val.charAt(i);
				for (String prop : propVec) {
					if (prop.equalsIgnoreCase("VALUE=URL")) {
						isUrl = true;
					}
				}
				if (isUrl) {
					// not Deal with photo URLS
				}
			}
		});

	}

	private void reset() {
		_id = null;
		parseLen = 0;
		displayName = null;
		nickName = null;
		notes = null;
		birthday = null;
		photo = null;
		firstName = null;
		lastName = null;
		if (phones == null)
			phones = new ArrayList<RowData>();
		else
			phones.clear();
		if (emails == null)
			emails = new ArrayList<RowData>();
		else
			emails.clear();
		if (addrs == null)
			addrs = new ArrayList<RowData>();
		else
			addrs.clear();
		if (orgs == null)
			orgs = new ArrayList<OrgData>();
		else
			orgs.clear();
		if (ims == null)
			ims = new ArrayList<RowData>();
		else
			ims.clear();
		if (urls == null) {
			urls = new ArrayList<RowData>();
		} else {
			urls.clear();
		}
	}

	// Constructors------------------------------------------------
	public Contact() {
	}

	public Contact(String vcard) {
		BufferedReader vcardReader = new BufferedReader(new StringReader(vcard));
		try {
			parseVCard(vcardReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Contact(BufferedReader vcfReader, SQLiteStatement querySyncId,
			SQLiteStatement queryPersionId, SQLiteStatement insertSyncId)
			throws IOException {
		parseVCard(vcfReader);
	}

	public Contact(Cursor peopleCur, ContentResolver cResolver,
			SQLiteStatement querySyncId, SQLiteStatement queryPersionId,
			SQLiteStatement insertSyncId) {
		populate(peopleCur, cResolver);
	}

	final static Pattern[] phonePatterns = {
			Pattern.compile("[+](1)(\\d\\d\\d)(\\d\\d\\d)(\\d\\d\\d\\d.*)"),
			Pattern.compile("[+](972)(2|3|4|8|9|50|52|54|57|59|77)(\\d\\d\\d)(\\d\\d\\d\\d.*)"), };

	/**
	 * Change the phone to canonical format (with dashes, etc.) if it's in a
	 * supported country.
	 * 
	 * @param phone
	 * @return
	 */
	String toCanonicalPhone(String phone) {
		for (final Pattern phonePattern : phonePatterns) {
			Matcher m = phonePattern.matcher(phone);
			if (m.matches()) {
				return "+" + m.group(1) + "-" + m.group(2) + "-" + m.group(3)
						+ "-" + m.group(4);
			}
		}

		return phone;
	}

	/**
	 * Set the person identifier
	 */
	public void setId(String id) {
		_id = id;
	}

	/**
	 * Get the person identifier
	 */
	public long getId() {
		return Long.parseLong(_id);
	}

	final static Pattern beginPattern = Pattern.compile("BEGIN:VCARD",
			Pattern.CASE_INSENSITIVE);
	final static Pattern propPattern = Pattern.compile("([^:]+):(.*)");
	final static Pattern propParamPattern = Pattern
			.compile("([^;=]+)(=([^;]+))?(;|$)");
	final static Pattern base64Pattern = Pattern
			.compile("\\s*([a-zA-Z0-9+/]+={0,2})\\s*$");
	final static Pattern namePattern = Pattern
			.compile("(([^,]+),(.*))|((.*?)\\s+(\\S+))");

	// Parse birthday in notes
	final static Pattern birthdayPattern = Pattern.compile("^" + BIRTHDAY_FIELD
			+ ":\\s*([^;]+)(;\\s*|\\s*$)", Pattern.CASE_INSENSITIVE);

	private static final String TAG = null;

	/**
	 * Parse the vCard string into the contacts fields
	 */
	public long parseVCard(BufferedReader vCard) throws IOException {
		// Reset the currently read values.
		reset();

		// Find Begin.
		String line = vCard.readLine();
		if (line != null)
			parseLen += line.length();
		else
			return -1;

		while (line != null && !beginPattern.matcher(line).matches()) {
			line = vCard.readLine();
			parseLen += line.length();
		}

		boolean skipRead = false;

		while (line != null) {
			if (!skipRead) {
				line = vCard.readLine();
			}

			if (line == null) {
				return 0;
			}
			parseLen += line.length();

			skipRead = false;

			// do multi-line unfolding (cr lf with whitespace immediately
			// following is removed, joining the two lines).
			vCard.mark(1);
			for (int ch = vCard.read(); ch == (int) ' ' || ch == (int) '\t'; ch = vCard
					.read()) {
				vCard.reset();
				String newLine = vCard.readLine();
				if (newLine != null) {
					line += newLine;
					parseLen += line.length();
					// parseLen += newLine.length();
				}
				vCard.mark(1);
			}
			vCard.reset();

			// parseLen += line.length(); // TODO: doesn't include CR LFs

			Matcher pm = propPattern.matcher(line);

			if (pm.matches()) {
				String prop = pm.group(1);
				String val = pm.group(2);

				if (prop.equalsIgnoreCase("END")
						&& val.equalsIgnoreCase("VCARD")) {
					// End of vCard
					return parseLen;
				}

				Matcher ppm = propParamPattern.matcher(prop);
				if (!ppm.find())
					// Doesn't seem to be a valid vCard property
					continue;

				String propName = ppm.group(1).toUpperCase();
				Vector<String> propVec = new Vector<String>();
				String charSet = "UTF-8";
				String encoding = "";
				while (ppm.find()) {
					String param = ppm.group(1);
					String paramVal = ppm.group(3);
					propVec.add(param
							+ (paramVal != null ? "=" + paramVal : ""));
					if (param.equalsIgnoreCase("CHARSET"))
						charSet = paramVal;
					else if (param.equalsIgnoreCase("ENCODING"))
						encoding = paramVal;
					Log.d(TAG, "propName: " + propName + ", paramVal: "
							+ paramVal);
				}
				if (encoding.equalsIgnoreCase("QUOTED-PRINTABLE")) {
					try {
						val = QuotedPrintable.decode(val.getBytes(charSet),
								"UTF-8");
					} catch (UnsupportedEncodingException uee) {

					}
				} else if (encoding.equalsIgnoreCase("BASE64")) {
					StringBuffer tmpVal = new StringBuffer(val);
					do {
						line = vCard.readLine();

						if ((line == null) || (line.length() == 0)
								|| (!base64Pattern.matcher(line).matches())) {
							// skipRead = true;
							break;
						}
						parseLen += line.length();
						tmpVal.append(line);
					} while (true);

					Base64Coder.decodeInPlace(tmpVal);
					val = tmpVal.toString();
				}
				handleProp propHandler = propHandlers.get(propName);
				if (propHandler != null)
					propHandler.parseProp(propName, propVec, val);
			}
		}
		return 0;
	}

	public long getParseLen() {
		return parseLen;
	}

	/**
	 * Format an email as a vCard field.
	 * 
	 * @param cardBuff
	 *            Formatted email will be appended to this buffer
	 * @param email
	 *            The rowdata containing the actual email data.
	 */
	public static void formatEmail(Appendable cardBuff, RowData email)
			throws IOException {
		cardBuff.append("EMAIL;INTERNET");
		if (email.preferred)
			cardBuff.append(";PREF");

		if (email.customLabel != null) {
			cardBuff.append(";" + LABEL_PARAM + "=");
			cardBuff.append(email.customLabel);
		}
		switch (email.type) {
		case Email.TYPE_WORK:
			cardBuff.append(";WORK");
			break;
		}

		if (!StringUtil.isASCII(email.data))
			cardBuff.append(";CHARSET=UTF-8");

		cardBuff.append(":").append(email.data.trim()).append(NL);
	}

	public static void formatUrl(Appendable cardBuff, RowData url)
			throws IOException {
		cardBuff.append("URL");

		cardBuff.append(":").append(url.data.trim()).append(NL);
	}

	/**
	 * Format a phone as a vCard field.
	 * 
	 * @param formatted
	 *            Formatted phone will be appended to this buffer
	 * @param phone
	 *            The rowdata containing the actual phone data.
	 */
	public static void formatPhone(Appendable formatted, RowData phone)
			throws IOException {
		formatted.append("TEL");
		if (phone.preferred)
			formatted.append(";PREF");

		if (phone.customLabel != null) {
			formatted.append(";" + LABEL_PARAM + "=");
			formatted.append(phone.customLabel);
		}
		switch (phone.type) {
		case Phone.TYPE_HOME:
			formatted.append(";VOICE");
			break;
		case Phone.TYPE_WORK:
			formatted.append(";VOICE;WORK");
			break;
		case Phone.TYPE_FAX_WORK:
			formatted.append(";FAX;WORK");
			break;
		case Phone.TYPE_FAX_HOME:
			formatted.append(";FAX;HOME");
			break;
		case Phone.TYPE_MOBILE:
			formatted.append(";CELL");
			break;
		case Phone.TYPE_PAGER:
			formatted.append(";PAGER");
			break;
		}

		if (!StringUtil.isASCII(phone.data))
			formatted.append(";CHARSET=UTF-8");
		formatted.append(":").append(phone.data.trim()).append(NL);
	}

	/**
	 * Format a phone as a vCard field.
	 * 
	 * @param formatted
	 *            Formatted phone will be appended to this buffer
	 * @param addr
	 *            The rowdata containing the actual phone data.
	 */
	public static void formatAddr(Appendable formatted, RowData addr)
			throws IOException {
		formatted.append("ADR");
		if (addr.preferred)
			formatted.append(";PREF");

		if (addr.customLabel != null) {
			formatted.append(";" + LABEL_PARAM + "=");
			formatted.append(addr.customLabel);
		}

		switch (addr.type) {
		case StructuredPostal.TYPE_HOME:
			formatted.append(";HOME");
			break;
		case StructuredPostal.TYPE_WORK:
			formatted.append(";WORK");
			break;
		}
		if (!StringUtil.isASCII(addr.data))
			formatted.append(";CHARSET=UTF-8");
		formatted.append(":;;").append(addr.data.replace(", ", ";").trim())
				.append(NL);
	}

	/**
	 * Format an IM contact as a vCard field.
	 * 
	 * @param formatted
	 *            Formatted im contact will be appended to this buffer
	 * @param addr
	 *            The rowdata containing the actual phone data.
	 */
	public static void formatIM(Appendable formatted, RowData im)
			throws IOException {
		formatted.append(IMPROP);
		if (im.preferred)
			formatted.append(";PREF");

		if (im.customLabel != null) {
			formatted.append(";" + LABEL_PARAM + "=");
			formatted.append(im.customLabel);
		}

		switch (im.type) {
		case Im.TYPE_HOME:
			formatted.append(";HOME");
			break;
		case Im.TYPE_WORK:
			formatted.append(";WORK");
			break;
		}

		if (im.auxData != null) {
			formatted.append(";").append(PROTO_PARAM).append("=")
					.append(im.auxData);
		}
		if (!StringUtil.isASCII(im.data))
			formatted.append(";CHARSET=UTF-8");
		formatted.append(":").append(im.data.trim()).append(NL);
	}

	/**
	 * Format Organization fields.
	 * 
	 * 
	 * 
	 * @param formatted
	 *            Formatted organization info will be appended to this buffer
	 * @param addr
	 *            The rowdata containing the actual organization data.
	 */
	public static void formatOrg(Appendable formatted, OrgData org)
			throws IOException {
		if (org.company != null) {
			formatted.append("ORG");
			if (org.customLabel != null) {
				formatted.append(";" + LABEL_PARAM + "=");
				formatted.append(org.customLabel);
			}
			if (!StringUtil.isASCII(org.company))
				formatted.append(";CHARSET=UTF-8");
			formatted.append(":").append(org.company.trim()).append(NL);
			if (org.title == null)
				formatted.append("TITLE:").append(NL);
		}
		if (org.title != null) {
			if (org.company == null)
				formatted.append("ORG:").append(NL);
			formatted.append("TITLE");
			if (org.customLabel != null) {
				formatted.append(";" + LABEL_PARAM + "=");
				formatted.append(org.customLabel);
			}
			if (!StringUtil.isASCII(org.title))
				formatted.append(";CHARSET=UTF-8");
			formatted.append(":").append(org.title.trim()).append(NL);
		}
	}

	public String toString() {
		StringWriter out = new StringWriter();
		try {
			writeVCard(out);
		} catch (IOException e) {
			// Should never happen
		}
		return out.toString();
	}

	/**
	 * Write the contact vCard to an appendable stream.
	 */
	public void writeVCard(Appendable vCardBuff) throws IOException {
		// Start vCard

		vCardBuff.append("BEGIN:VCARD").append(NL);
		vCardBuff.append("VERSION:2.1").append(NL);

		vCardBuff.append("N");

		if (!StringUtil.isASCII(lastName) || !StringUtil.isASCII(firstName))
			vCardBuff.append(";CHARSET=UTF-8");

		vCardBuff.append(":").append((lastName != null) ? lastName.trim() : "")
				.append(";")
				.append((firstName != null) ? firstName.trim() : "")
				.append(";").append(";").append(";").append(NL);

		if (nickName != null) {
			vCardBuff.append(NICKPROP);
			if (!StringUtil.isASCII(nickName))
				vCardBuff.append(";CHARSET=UTF-8");
			vCardBuff.append(":").append(nickName.trim()).append(NL);
		}

		for (RowData email : emails) {
			formatEmail(vCardBuff, email);
		}

		for (RowData url : urls) {
			formatUrl(vCardBuff, url);
		}

		for (RowData phone : phones) {
			formatPhone(vCardBuff, phone);
		}

		for (OrgData org : orgs) {
			formatOrg(vCardBuff, org);
		}

		for (RowData addr : addrs) {
			formatAddr(vCardBuff, addr);
		}

		for (RowData im : ims) {
			formatIM(vCardBuff, im);
		}

		appendField(vCardBuff, "NOTE", notes);
		appendField(vCardBuff, "BDAY", birthday);

		if (photo != null) {
			appendField(vCardBuff, "PHOTO;TYPE=JPEG;ENCODING=BASE64", " ");
			Base64Coder.mimeEncode(vCardBuff, photo, 76, NL);
			vCardBuff.append(NL);
			vCardBuff.append(NL);
		}

		// End vCard
		vCardBuff.append("END:VCARD").append(NL);
	}

	/**
	 * Append the field to the StringBuffer out if not null.
	 */
	private static void appendField(Appendable out, String name, String val)
			throws IOException {
		if (val != null && val.length() > 0) {
			out.append(name);
			if (!StringUtil.isASCII(val))
				out.append(";CHARSET=UTF-8");
			out.append(":").append(val).append(NL);
		}
	}

	/**
	 * Populate the contact fields from a cursor
	 */
	public void populate(Cursor peopleCur, ContentResolver cResolver) {
		reset();
		setPeopleFields(peopleCur);
		String personID = _id;

		//
		// Get PeopleColumns fields
		//
		Cursor structuredName = cResolver.query(Data.CONTENT_URI, null,
				Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
						+ StructuredName.CONTENT_ITEM_TYPE + "'",
				new String[] { String.valueOf(personID) }, null);

		if (structuredName != null) {
			if (structuredName.moveToFirst()) {
				int selectedColumn = structuredName
						.getColumnIndex(StructuredName.DISPLAY_NAME);
				displayName = structuredName.getString(selectedColumn);
			}
			structuredName.close();
		}

		if (displayName != null) {
			Matcher m = namePattern.matcher(displayName);
			if (m.matches()) {
				if (m.group(1) != null) {
					lastName = m.group(2);
					firstName = m.group(3);
				} else {
					firstName = m.group(5);
					lastName = m.group(6);
				}
			} else {
				firstName = displayName;
				lastName = "";
			}
		} else {
			firstName = lastName = "";
		}

		Cursor nickNameCursor = cResolver.query(Data.CONTENT_URI, null,
				Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
						+ Nickname.CONTENT_ITEM_TYPE + "'",
				new String[] { String.valueOf(personID) }, null);

		if (nickNameCursor != null) {
			if (nickNameCursor.moveToFirst()) {
				nickName = nickNameCursor.getString(nickNameCursor
						.getColumnIndex(Nickname.NAME));
				Log.d(TAG, "nickName: " + nickName);
			}
			nickNameCursor.close();
		}

		Cursor organization = cResolver.query(Data.CONTENT_URI, null,
				Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
						+ Organization.CONTENT_ITEM_TYPE + "'",
				new String[] { String.valueOf(personID) }, null);

		// Set the organization fields
		if (organization != null) {
			if (organization.moveToFirst()) {
				do {
					setOrganizationFields(organization);
				} while (organization.moveToNext());
			}
			organization.close();
		}

		// Set all the phone numbers
		Cursor phones = cResolver.query(Data.CONTENT_URI, null,
				Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
						+ Phone.CONTENT_ITEM_TYPE + "'",
				new String[] { String.valueOf(personID) }, null);

		if (phones != null) {
			if (phones.moveToFirst()) {
				do {
					setPhoneFields(phones);
				} while (phones.moveToNext());
			}
			phones.close();

		}

		// emails
		Cursor contactMethods = cResolver.query(Data.CONTENT_URI, null,
				Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
						+ Email.CONTENT_ITEM_TYPE + "'",
				new String[] { String.valueOf(personID) }, null);

		if (contactMethods != null) {
			if (contactMethods.moveToFirst()) {
				do {
					setEmailMethodsFields(contactMethods);
				} while (contactMethods.moveToNext());
			}
			contactMethods.close();
		}

		// , addresses
		Cursor addressesMethods = cResolver.query(Data.CONTENT_URI, null,
				Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
						+ StructuredPostal.CONTENT_ITEM_TYPE + "'",
				new String[] { String.valueOf(personID) }, null);

		if (addressesMethods != null) {
			if (addressesMethods.moveToFirst()) {
				do {
					setAddrMethodsFields(addressesMethods);
				} while (addressesMethods.moveToNext());
			}
			addressesMethods.close();
		}

		// ims
		Cursor imMethods = cResolver.query(Data.CONTENT_URI, null,
				Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
						+ Im.CONTENT_ITEM_TYPE + "'",
				new String[] { String.valueOf(personID) }, null);

		if (imMethods != null) {
			if (imMethods.moveToFirst()) {
				do {
					setImMethodsFields(imMethods);
				} while (imMethods.moveToNext());
			}
			imMethods.close();
		}

		// website
		Cursor c = cResolver.query(Data.CONTENT_URI, null, Data.RAW_CONTACT_ID
				+ "=?" + " AND " + Data.MIMETYPE + "='"
				+ Website.CONTENT_ITEM_TYPE + "'",
				new String[] { String.valueOf(personID) }, null);

		if (c != null) {
			while (c.moveToNext()) {
				urls.add(new RowData(Website.TYPE_OTHER, c.getString(c
						.getColumnIndex(Website.URL)), true));
			}
			c.close();
		}

		// Load a photo if one exists.
		Cursor contactPhoto = cResolver.query(Data.CONTENT_URI, null,
				Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
						+ Photo.CONTENT_ITEM_TYPE + "'",
				new String[] { String.valueOf(personID) }, null);

		if (contactPhoto != null) {
			if (contactPhoto.moveToFirst()) {
				photo = contactPhoto.getBlob(contactPhoto
						.getColumnIndex(Photo.PHOTO));
			}
			contactPhoto.close();
		}

	}

	/**
	 * Retrieve the People fields from a Cursor
	 */
	private void setPeopleFields(Cursor cur) {

		int selectedColumn;

		// Set the contact id
		selectedColumn = cur.getColumnIndex(RawContacts._ID);
		long nid = cur.getLong(selectedColumn);
		_id = String.valueOf(nid);

		// TODO: note and birthday
		// selectedColumn = cur.getColumnIndex(Contacts.People.NOTES);
		// notes = cur.getString(selectedColumn);
		// if (notes != null) {
		// Matcher ppm = birthdayPattern.matcher(notes);
		//
		// if (ppm.find()) {
		// birthday = ppm.group(1);
		// notes = ppm.replaceFirst("");
		// }
		// }
	}

	/**
	 * Retrieve the organization fields from a Cursor
	 */
	private void setOrganizationFields(Cursor cur) {

		int selectedColumn;

		//
		// Get Organizations fields
		//
		selectedColumn = cur.getColumnIndex(Organization.COMPANY);
		String company = cur.getString(selectedColumn);

		selectedColumn = cur.getColumnIndex(Organization.TITLE);
		String title = cur.getString(selectedColumn);

		selectedColumn = cur.getColumnIndex(Organization.TYPE);
		int orgType = cur.getInt(selectedColumn);

		String customLabel = null;
		if (orgType == Organization.TYPE_CUSTOM) {
			selectedColumn = cur.getColumnIndex(Organization.LABEL);
			customLabel = cur.getString(selectedColumn);
		}

		orgs.add(new OrgData(orgType, title, company, customLabel));
	}

	/**
	 * Retrieve the Phone fields from a Cursor
	 */
	private void setPhoneFields(Cursor cur) {

		int selectedColumn;
		int selectedColumnType;
		int preferredColumn;
		int phoneType;
		String customLabel = null;

		//
		// Get PhonesColums fields
		//
		selectedColumn = cur.getColumnIndex(Phone.NUMBER);
		selectedColumnType = cur.getColumnIndex(Phone.TYPE);
		preferredColumn = cur.getColumnIndex(Phone.IS_PRIMARY);
		phoneType = cur.getInt(selectedColumnType);
		String phone = cur.getString(selectedColumn);
		boolean preferred = cur.getInt(preferredColumn) != 0;
		if (phoneType == Phone.TYPE_CUSTOM) {
			customLabel = cur.getString(cur.getColumnIndex(Phone.LABEL));
		}

		phones.add(new RowData(phoneType, phone, preferred, customLabel));
	}

	private void setAddrMethodsFields(Cursor cur) {
		// int selectedColumn;
		int selectedColumnType;
		int selectedColumnPrimary;
		int selectedColumnLabel;

		int methodType;
		String customLabel = null;

		// selectedColumn =
		// cur.getColumnIndex(StructuredPostal.FORMATTED_ADDRESS);

		int cityColumn = cur.getColumnIndex(StructuredPostal.CITY);
		int countryColumn = cur.getColumnIndex(StructuredPostal.COUNTRY);
		int regionColumn = cur.getColumnIndex(StructuredPostal.REGION);
		int streetColumn = cur.getColumnIndex(StructuredPostal.STREET);
		int postCodeColumn = cur.getColumnIndex(StructuredPostal.POSTCODE);

		selectedColumnType = cur.getColumnIndex(StructuredPostal.TYPE);
		selectedColumnPrimary = cur.getColumnIndex(StructuredPostal.IS_PRIMARY);

		methodType = cur.getInt(selectedColumnType);
		// String methodData = cur.getString(selectedColumn);

		String city = cur.getString(cityColumn);
		String country = cur.getString(countryColumn);
		String region = cur.getString(regionColumn);
		String street = cur.getString(streetColumn);
		String postcode = cur.getString(postCodeColumn);

		if (city == null)
			city = "";
		if (country == null)
			country = "";
		if (region == null)
			region = "";
		if (street == null)
			street = "";
		if (postcode == null)
			postcode = "";

		// String address = street==null?"":street + ";" + city==null?"":city +
		// ";" + region==null?"":region + ";" + postcode==null?"":postcode + ";"
		// + country==null?"":country;
		String address = street + ";" + city + ";" + region + ";" + postcode
				+ ";" + country;

		boolean preferred = cur.getInt(selectedColumnPrimary) != 0;
		if (methodType == StructuredPostal.TYPE_CUSTOM) {
			selectedColumnLabel = cur.getColumnIndex(StructuredPostal.LABEL);
			customLabel = cur.getString(selectedColumnLabel);
		}

		addrs.add(new RowData(methodType, address, preferred, customLabel));
	}

	private void setImMethodsFields(Cursor cur) {
		int selectedColumn;
		int selectedColumnType;
		int selectedColumnPrimary;
		int selectedColumnLabel;

		int methodType;
		String customLabel = null;

		//
		// Get ContactsMethodsColums fields
		//
		selectedColumn = cur.getColumnIndex(Im.DATA);
		selectedColumnType = cur.getColumnIndex(Im.TYPE);
		selectedColumnPrimary = cur.getColumnIndex(Im.IS_PRIMARY);

		methodType = cur.getInt(selectedColumnType);
		String methodData = cur.getString(selectedColumn);
		boolean preferred = cur.getInt(selectedColumnPrimary) != 0;
		if (methodType == Im.TYPE_CUSTOM) {
			selectedColumnLabel = cur.getColumnIndex(Im.LABEL);
			customLabel = cur.getString(selectedColumnLabel);
		}

		RowData newRow = new RowData(methodType, methodData, preferred,
				customLabel);

		ims.add(newRow);
	}

	/**
	 * Retrieve the email fields from a Cursor
	 */
	private void setEmailMethodsFields(Cursor cur) {

		int selectedColumn;
		int selectedColumnType;
		int selectedColumnPrimary;
		int selectedColumnLabel;

		int methodType;
		String customLabel = null;

		//
		// Get ContactsMethodsColums fields
		//
		selectedColumn = cur.getColumnIndex(Email.DATA);
		selectedColumnType = cur.getColumnIndex(Email.TYPE);
		selectedColumnPrimary = cur.getColumnIndex(Email.IS_PRIMARY);

		methodType = cur.getInt(selectedColumnType);
		String methodData = cur.getString(selectedColumn);
		boolean preferred = cur.getInt(selectedColumnPrimary) != 0;
		if (methodType == Email.TYPE_CUSTOM) {
			selectedColumnLabel = cur.getColumnIndex(Email.LABEL);
			customLabel = cur.getString(selectedColumnLabel);
		}

		emails.add(new RowData(methodType, methodData, preferred, customLabel));
	}

	public ContentValues getPeopleCV() {
		ContentValues cv = new ContentValues();
		cv.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);

		StringBuffer fullname = new StringBuffer();
		if (displayName != null)
			fullname.append(displayName);
		else {
			if (firstName != null)
				fullname.append(firstName);
			if (lastName != null) {
				if (firstName != null)
					fullname.append(" ");
				fullname.append(lastName);
			}
		}

		// Use company name if only the company is given.
		if (fullname.length() == 0 && orgs.size() > 0
				&& orgs.get(0).company != null)
			fullname.append(orgs.get(0).company);

		cv.put(StructuredName.DISPLAY_NAME, fullname.toString());

		// TODO: note and birthday
		// if (!StringUtil.isNullOrEmpty(_id)) {
		// cv.put(Contacts.People._ID, _id);
		// }

		// StringBuffer allnotes = new StringBuffer();
		// if (birthday != null) {
		// allnotes.append(BIRTHDAY_FIELD).append(" ").append(birthday);
		// }
		// if (notes != null) {
		// if (birthday != null) {
		// allnotes.append(";\n");
		// }
		// allnotes.append(notes);
		// }

		// if (allnotes.length() > 0)
		// cv.put(Note.NOTE, allnotes.toString());

		return cv;
	}

	public ContentValues getOrganizationCV(OrgData org) {

		if (StringUtil.isNullOrEmpty(org.company)
				&& StringUtil.isNullOrEmpty(org.title)) {
			return null;
		}
		ContentValues cv = new ContentValues();
		cv.put(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE);

		cv.put(Organization.COMPANY, org.company);
		cv.put(Organization.TITLE, org.title);
		cv.put(Organization.TYPE, org.type);
		if (org.customLabel != null) {
			cv.put(Organization.LABEL, org.customLabel);
		}

		return cv;
	}

	public ContentValues getPhoneCV(RowData data) {
		ContentValues cv = new ContentValues();
		cv.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);

		cv.put(Phone.NUMBER, data.data);
		cv.put(Phone.TYPE, data.type);
		if (data.customLabel != null) {
			cv.put(Phone.LABEL, data.customLabel);
		}

		return cv;
	}

	public ContentValues getEmailCV(RowData data) {
		ContentValues cv = new ContentValues();
		cv.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);

		cv.put(Email.DATA, data.data);
		cv.put(Email.TYPE, data.type);
		if (data.customLabel != null) {
			cv.put(Email.LABEL, data.customLabel);
		}

		return cv;
	}

	public String getValidateStr(String str) {

		if (str != null && !str.trim().equals("")) {
			return str.trim();
		}
		return null;
	}

	public ContentValues getAddressCV(RowData data) {
		ContentValues cv = new ContentValues();
		cv.put(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);

		String[] split = data.data.split(",");

		StringBuffer sb = new StringBuffer();

		String street = getValidateStr(split[0]);
		String city = getValidateStr(split[1]);
		String region = getValidateStr(split[2]);
		String postcode = getValidateStr(split[3]);
		String country = getValidateStr(split[4]);

		if (street != null) {
			cv.put(StructuredPostal.STREET, street);
			sb.append(street);
			if (country != null || city != null || region != null
					|| postcode != null) {
				sb.append("\n");
			}
		}
		if (city != null) {
			cv.put(StructuredPostal.CITY, city);
			sb.append(city);
		}
		if (region != null) {
			cv.put(StructuredPostal.REGION, region);
			if (city != null) {
				sb.append(",");
			}
			sb.append(region);
		}
		if (postcode != null) {
			cv.put(StructuredPostal.POSTCODE, postcode);
			if (region != null) {
				sb.append(" ");
			}
			sb.append(postcode);
		}
		if (country != null) {
			cv.put(StructuredPostal.COUNTRY, country);
			if (street != null || city != null || region != null
					|| postcode != null) {
				sb.append("\n");
			}
			sb.append(country);
		}

		cv.put(StructuredPostal.FORMATTED_ADDRESS, sb.toString());
		cv.put(StructuredPostal.TYPE, data.type);
		if (data.customLabel != null) {
			cv.put(StructuredPostal.LABEL, data.customLabel);
		}

		return cv;
	}

	public ContentValues getUrlCV(RowData data) {
		ContentValues cv = new ContentValues();
		cv.put(Data.MIMETYPE, Website.CONTENT_ITEM_TYPE);

		cv.put(Website.DATA, data.data);
		cv.put(Website.TYPE, data.type);

		return cv;
	}

	public ContentValues getImCV(RowData data) {
		ContentValues cv = new ContentValues();
		cv.put(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE);

		cv.put(Im.DATA, data.data);
		cv.put(Im.TYPE, data.type);
		if (data.customLabel != null) {
			cv.put(Im.LABEL, data.customLabel);
		}

		return cv;
	}

	/**
	 * Add a new contact to the Content Resolver
	 * 
	 * @param key
	 *            the row number of the existing contact (if known)
	 * @return The row number of the inserted column
	 */
	public ArrayList<ContentProviderOperation> addContact(Context context,
			long key, boolean replace, ArrayList<ContentProviderOperation> ops) {
		int backRef = ops.size();
		Log.d(TAG, "backRef: " + backRef);

		ContentValues values = new ContentValues();
		// values.put(RawContacts.ACCOUNT_TYPE, "com.sina");
		// values.put(RawContacts.ACCOUNT_NAME, "vdisk");
		ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
				.withValues(values).build());

		ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, backRef)
				.withValues(getPeopleCV()).build());

		if (nickName != null) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, backRef)
					.withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
					.withValue(Nickname.NAME, nickName).build());
		}

		// Phones
		for (RowData phone : phones) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, backRef)
					.withValues(getPhoneCV(phone)).build());
		}

		// Organizations
		for (OrgData org : orgs) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, backRef)
					.withValues(getOrganizationCV(org)).build());
		}

		// Emails
		for (RowData email : emails) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, backRef)
					.withValues(getEmailCV(email)).build());
		}

		// Addressess
		for (RowData addr : addrs) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, backRef)
					.withValues(getAddressCV(addr)).build());
		}

		// IMs
		for (RowData im : ims) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, backRef)
					.withValues(getImCV(im)).build());
		}

		// Urls
		for (RowData url : urls) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, backRef)
					.withValues(getUrlCV(url)).build());
		}

		// Photo
		if (photo != null) {
			ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, backRef)
					.withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
					.withValue(Photo.PHOTO, photo).build());
		}

		return ops;
	}
}

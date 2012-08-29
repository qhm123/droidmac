/*
 * Funambol is a mobile platform developed by Funambol, Inc. 
 * Copyright (C) 2003 - 2007 Funambol, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission 
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY FUNAMBOL, FUNAMBOL DISCLAIMS THE 
 * WARRANTY OF NON INFRINGEMENT  OF THIRD PARTY RIGHTS.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License 
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 * 
 * You can contact Funambol, Inc. headquarters at 643 Bair Island Road, Suite 
 * 305, Redwood City, CA 94063, USA, or at email address info@funambol.com.
 * 
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 * 
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * "Powered by Funambol" logo. If the display of the logo is not reasonably 
 * feasible for technical reasons, the Appropriate Legal Notices must display
 * the words "Powered by Funambol". 
 */

package com.qhm123.droidmac.api.contact;

import java.util.Vector;

/**
 * Utility class useful when dealing with string objects. This class is a
 * collection of static functions, and the usage is:
 * 
 * StringUtil.method()
 * 
 * it is not allowed to create instances of this class
 */
public class StringUtil {

	private static final String HT = "\t";
	private static final String CRLF = "\r\n";

	// This class cannot be instantiated
	private StringUtil() {
	}

	/**
	 * Split the string into an array of strings using one of the separator in
	 * 'sep'.
	 * 
	 * @param s
	 *            the string to tokenize
	 * @param sep
	 *            a list of separator to use
	 * 
	 * @return the array of tokens (an array of size 1 with the original string
	 *         if no separator found)
	 */
	public static String[] split(String s, String sep) {
		// convert a String s to an Array, the elements
		// are delimited by sep
		Vector<Integer> tokenIndex = new Vector<Integer>(10);
		int len = s.length();
		int i;

		// Find all characters in string matching one of the separators in 'sep'
		for (i = 0; i < len; i++) {
			if (sep.indexOf(s.charAt(i)) != -1) {
				tokenIndex.addElement(new Integer(i));
			}
		}

		int size = tokenIndex.size();
		String[] elements = new String[size + 1];

		// No separators: return the string as the first element
		if (size == 0) {
			elements[0] = s;
		} else {
			// Init indexes
			int start = 0;
			int end = (tokenIndex.elementAt(0)).intValue();
			// Get the first token
			elements[0] = s.substring(start, end);

			// Get the mid tokens
			for (i = 1; i < size; i++) {
				// update indexes
				start = (tokenIndex.elementAt(i - 1)).intValue() + 1;
				end = (tokenIndex.elementAt(i)).intValue();
				elements[i] = s.substring(start, end);
			}
			// Get last token
			start = (tokenIndex.elementAt(i - 1)).intValue() + 1;
			elements[i] = (start < s.length()) ? s.substring(start) : "";
		}

		return elements;
	}

	/**
	 * Split the string into an array of strings using one of the separator in
	 * 'sep'.
	 * 
	 * @param list
	 *            the string array to join
	 * @param sep
	 *            the separator to use
	 * 
	 * @return the joined string
	 */
	public static String join(String[] list, String sep) {
		StringBuffer buffer = new StringBuffer(list[0]);
		int len = list.length;

		for (int i = 1; i < len; i++) {
			buffer.append(sep).append(list[i]);
		}
		return buffer.toString();
	}

	/**
	 * Returns the string array
	 * 
	 * @param stringVec
	 *            the Vecrot of tring to convert
	 * @return String []
	 */
	public static String[] getStringArray(Vector<String> stringVec) {

		if (stringVec == null) {
			return null;
		}

		String[] stringArray = new String[stringVec.size()];
		for (int i = 0; i < stringVec.size(); i++) {
			stringArray[i] = stringVec.elementAt(i);
		}
		return stringArray;
	}

	/**
	 * Find two consecutive newlines in a string.
	 * 
	 * @param s
	 *            - The string to search
	 * @return int: the position of the empty line
	 */
	public static int findEmptyLine(String s) {
		int ret = 0;

		// Find a newline
		while ((ret = s.indexOf("\n", ret)) != -1) {
			// Skip carriage returns, if any
			while (s.charAt(ret) == '\r') {
				ret++;
			}
			if (s.charAt(ret) == '\n') {
				// Okay, it was empty
				ret++;
				break;
			}
		}
		return ret;
	}

	/**
	 * Removes unwanted blank characters
	 * 
	 * @param content
	 * @return String
	 */
	public static String removeBlanks(String content) {

		if (content == null) {
			return null;
		}

		StringBuffer buff = new StringBuffer();
		buff.append(content);

		for (int i = buff.length() - 1; i >= 0; i--) {
			if (' ' == buff.charAt(i)) {
				buff.deleteCharAt(i);
			}
		}
		return buff.toString();
	}

	/**
	 * Removes unwanted backslashes characters
	 * 
	 * @param content
	 *            The string containing the backslashes to be removed
	 * @return the content without backslashes
	 */
	public static String removeBackslashes(String content) {

		if (content == null) {
			return null;
		}

		StringBuffer buff = new StringBuffer();
		buff.append(content);

		int len = buff.length();
		for (int i = len - 1; i >= 0; i--) {
			if ('\\' == buff.charAt(i))
				buff.deleteCharAt(i);
		}
		return buff.toString();
	}

	/**
	 * Builds a list of the recipients email addresses each on a different line,
	 * starting just from the second line with an HT ("\t") separator at the
	 * head of the line. This is an implementation of the 'folding' concept from
	 * the RFC 2822 (par. 2.2.3)
	 * 
	 * @param recipients
	 *            A string containing all recipients comma-separated
	 * @return A string containing the email list of the recipients spread over
	 *         more lines, ended by CRLF and beginning from the second with the
	 *         WSP defined in the RFC 2822
	 */
	public static String fold(String recipients) {
		String[] list = StringUtil.split(recipients, ",");

		StringBuffer buffer = new StringBuffer();

		for (int i = 0; i < list.length; i++) {
			String address = list[i] + (i != list.length - 1 ? "," : "");
			buffer.append(i == 0 ? address + CRLF : HT + address + CRLF);
		}

		return buffer.toString();
	}

	/**
	 * This method is missing in CLDC 1.0 String implementation
	 */
	public static boolean equalsIgnoreCase(String string1, String string2) {
		// Strings are both null, return true
		if (string1 == null && string2 == null) {
			return true;
		}
		// One of the two is null, return false
		if (string1 == null || string2 == null) {
			return false;
		}
		// Both are not null, compare the lowercase strings
		if ((string1.toLowerCase()).equals(string2.toLowerCase())) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Util method for retrieve a boolean primitive type from a String.
	 * Implemented because Boolean class doesn't provide parseBoolean() method
	 */
	public static boolean getBooleanValue(String string) {
		if ((string == null) || string.equals("")) {
			return false;
		} else {
			if (StringUtil.equalsIgnoreCase(string, "true")) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Removes characters 'c' from the beginning and the end of the string
	 */
	public static String trim(String s, char c) {
		int start = 0;
		int end = s.length() - 1;

		while (s.charAt(start) == c) {
			if (++start >= end) {
				// The string is made by c only
				return "";
			}
		}

		while (s.charAt(end) == c) {
			if (--end <= start) {
				return "";
			}
		}

		return s.substring(start, end + 1);
	}

	/**
	 * Returns true if the given string is null or empty.
	 */
	public static boolean isNullOrEmpty(String str) {
		if (str == null) {
			return true;
		}
		if (str.trim().equals("")) {
			return true;
		}
		return false;
	}

	/**
	 * Returns true if the string does not fit in standard ASCII
	 */
	public static boolean isASCII(String str) {
		if (str == null)
			return true;
		for (int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);
			if (c < 0 || c > 0x7f)
				return false;
		}
		return true;
	}

}

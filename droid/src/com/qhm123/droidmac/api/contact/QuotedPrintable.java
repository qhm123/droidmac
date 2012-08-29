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

import java.io.UnsupportedEncodingException;

import android.util.Log;

/**
 * A class containing static methods to perform decoding from <b>quoted
 * printable</b> content transfer encoding and to encode into
 */
public class QuotedPrintable {

	private static final String TAG = QuotedPrintable.class.getSimpleName();

	private static byte HT = 0x09; // \t
	private static byte LF = 0x0A; // \n
	private static byte CR = 0x0D; // \r

	/**
	 * A method to decode quoted printable encoded data. It overrides the same
	 * input byte array to save memory. Can be done because the result is surely
	 * smaller than the input.
	 * 
	 * @param qp
	 *            a byte array to decode.
	 * @return the length of the decoded array.
	 */
	public static int decode(byte[] qp) {
		int qplen = qp.length;
		int retlen = 0;

		for (int i = 0; i < qplen; i++) {
			// Handle encoded chars
			if (qp[i] == '=') {
				if (qplen - i > 2) {
					// The sequence can be complete, check it
					if (qp[i + 1] == CR && qp[i + 2] == LF) {
						// soft line break, ignore it
						i += 2;
						continue;

					} else if (isHexDigit(qp[i + 1]) && isHexDigit(qp[i + 2])) {
						// convert the number into an integer, taking
						// the ascii digits stored in the array.
						qp[retlen++] = (byte) (getHexValue(qp[i + 1]) * 16 + getHexValue(qp[i + 2]));

						i += 2;
						continue;

					} else {
						Log.e(TAG, "decode: Invalid sequence = " + qp[i + 1]
								+ qp[i + 2]);
					}
				}
				// In all wrong cases leave the original bytes
				// (see RFC 2045). They can be incomplete sequence,
				// or a '=' followed by non hex digit.
			}

			// RFC 2045 says to exclude control characters mistakenly
			// present (unencoded) in the encoded stream.
			// As an exception, we keep unencoded tabs (0x09)
			if ((qp[i] >= 0x20 && qp[i] <= 0x7f) || qp[i] == HT || qp[i] == CR
					|| qp[i] == LF) {
				qp[retlen++] = qp[i];
			}
		}

		return retlen;
	}

	private static boolean isHexDigit(byte b) {
		return ((b >= 0x30 && b <= 0x39) || (b >= 0x41 && b <= 0x46));
	}

	private static byte getHexValue(byte b) {
		return (byte) Character.digit((char) b, 16);
	}

	/**
	 * 
	 * @param qp
	 *            Byte array to decode
	 * @param enc
	 *            The character encoding of the returned string
	 * @return The decoded string.
	 */
	public static String decode(byte[] qp, String enc) {
		int len = decode(qp);
		try {
			return new String(qp, 0, len, enc);
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "qp.decode: " + enc + " not supported. " + e.toString());
			return new String(qp, 0, len);
		}
	}

	/**
	 * A method to encode data in quoted printable
	 * 
	 * @param content
	 *            The string to be encoded
	 * @return the encoded string.
	 * @throws Exception
	 * 
	 *             public static byte[] encode(String content, String enc)
	 *             throws Exception { // TODO: to be implemented (has to return
	 *             a String) throw new
	 *             Exception("This method is not implemented!"); }
	 */
	/**
	 * A method to encode data in quoted printable
	 * 
	 * @param content
	 *            The string to be encoded
	 * @return the encoded string.
	 * @throws Exception
	 * 
	 *             public static byte[] encode(byte[] content) throws Exception
	 *             { // TODO: to be implemented (has to return a String) throw
	 *             new Exception("This method is not implemented!"); }
	 */

}

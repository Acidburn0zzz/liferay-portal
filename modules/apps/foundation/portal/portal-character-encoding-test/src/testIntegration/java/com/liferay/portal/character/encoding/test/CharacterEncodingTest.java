/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.character.encoding.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.character.encoding.filter.CharacterEncodingFilter;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Matthew Tambara
 */
@RunWith(Arquillian.class)
public class CharacterEncodingTest {

	@Test
	public void testWithCharacterEncoding() throws IOException {
		String output = _testCharacterEncoding(true);

		Assert.assertEquals(output, _JAPANESE_TEST, output);
	}

	@Test
	public void testWithoutCharacterEncoding() throws IOException {
		String output = _testCharacterEncoding(false);

		Assert.assertNotEquals(output, _JAPANESE_TEST, output);
	}

	private String _testCharacterEncoding(boolean addCharacterEncoding)
		throws IOException {

		URL url = new URL("http://localhost:8080");

		HttpURLConnection httpURLConnection =
			(HttpURLConnection)url.openConnection();

		httpURLConnection.setRequestMethod("POST");

		String contentType = "application/x-www-form-urlencoded";

		if (addCharacterEncoding) {
			contentType = contentType + "; charset=" + _CHARSET;
		}

		httpURLConnection.addRequestProperty("Content-Type", contentType);

		httpURLConnection.setDoOutput(true);

		try (OutputStream outputStream = httpURLConnection.getOutputStream()) {
			outputStream.write(_PARAMETER_STRING.getBytes(_CHARSET));
		}

		StringBundler sb = new StringBundler(1);

		try (BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(
					httpURLConnection.getInputStream(), _CHARSET))) {

			String output = null;

			while ((output = bufferedReader.readLine()) != null) {
				sb.append(output);
			}
		}
		finally {
			httpURLConnection.disconnect();
		}

		return sb.toString();
	}

	private static final Charset _CHARSET = Charset.forName("Shift_JIS");

	private static final String _JAPANESE_TEST = "テスト";

	private static final String _PARAMETER_STRING =
		CharacterEncodingFilter.REQUEST_PARAMETER_NAME + StringPool.EQUAL +
			_JAPANESE_TEST;

}
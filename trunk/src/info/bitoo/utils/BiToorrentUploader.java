/*
 * Copyright (c) 2004 Fabio Di Fabio
 * All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc., 675
 * Mass Ave, Cambridge, MA 02139, USA.
 */
package info.bitoo.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BiToorrentUploader {

	private final static String lineEnd = "\r\n";

	private final static String twoHyphens = "--";

	private final static String boundary = "***BiToo***";

	private final static int maxBufferSize = 1024 * 100;

	public static void main(String[] args) throws Exception {
		URL uploadURL = new URL("http://www.bitoo.info/tracker/newtorrents.php");

		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		DataInputStream inStream = null;

		File torrentFile = new File(args[0]);

		//------------------ CLIENT REQUEST

		FileInputStream fileInputStream = new FileInputStream(torrentFile);

		// open a URL connection to the Servlet

		// Open a HTTP connection to the URL

		conn = (HttpURLConnection) uploadURL.openConnection();

		// Allow Inputs
		conn.setDoInput(true);

		// Allow Outputs
		conn.setDoOutput(true);

		// Don't use a cached copy.
		conn.setUseCaches(false);

		// Use a post method.
		conn.setRequestMethod("POST");

		//conn.setRequestProperty("Connection", "Keep-Alive");

		conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="
				+ boundary);

		dos = new DataOutputStream(conn.getOutputStream());

		dos.writeBytes(twoHyphens + boundary + lineEnd);
		dos.writeBytes("Content-Disposition: form-data; name=\"username\""
				+ lineEnd);
		dos.writeBytes(lineEnd);
		dos.writeBytes(args[1]);
		dos.writeBytes(lineEnd);
		dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

		dos.writeBytes(twoHyphens + boundary + lineEnd);
		dos.writeBytes("Content-Disposition: form-data; name=\"password\""
				+ lineEnd);
		dos.writeBytes(lineEnd);
		dos.writeBytes(args[2]);
		dos.writeBytes(lineEnd);
		dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

		dos.writeBytes(twoHyphens + boundary + lineEnd);
		dos.writeBytes("Content-Disposition: form-data; name=\"filename\""
				+ lineEnd);
		dos.writeBytes(lineEnd);
		dos.writeBytes(torrentFile.getName().substring(0,
				torrentFile.getName().lastIndexOf('.')));
		dos.writeBytes(lineEnd);
		dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

		dos.writeBytes(twoHyphens + boundary + lineEnd);
		dos
				.writeBytes("Content-Disposition: form-data; name=\"url\""
						+ lineEnd);
		dos.writeBytes(lineEnd);
		dos.writeBytes("http://www.bitoo.info/torrents/"
				+ torrentFile.getName());
		dos.writeBytes(lineEnd);
		dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

		dos.writeBytes(twoHyphens + boundary + lineEnd);
		dos.writeBytes("Content-Disposition: form-data; name=\"torrent\";"
				+ " filename=\"" + torrentFile.getName() + "\"" + lineEnd);
		dos.writeBytes(lineEnd);

		// create a buffer of maximum size

		int bytesAvailable = fileInputStream.available();
		int bufferSize = Math.min(bytesAvailable, maxBufferSize);
		byte[] buffer = new byte[bufferSize];

		// read file and write it into form...

		int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

		while (bytesRead > 0) {
			dos.write(buffer, 0, bufferSize);
			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		}

		// send multipart form data necesssary after file data...

		dos.writeBytes(lineEnd);
		dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

		// close streams

		fileInputStream.close();
		dos.flush();
		dos.close();

		//------------------ read the SERVER RESPONSE

		inStream = new DataInputStream(conn.getInputStream());
		String str;
		while ((str = inStream.readLine()) != null) {
			System.out.println("Server response is: [" + str + "]");
		}
		inStream.close();

	}
}
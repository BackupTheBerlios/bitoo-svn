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

package info.bitoo.clientadapters;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class AzureusWebUI implements ClientAdapter {
	private final static Logger logger = Logger.getLogger(AzureusWebUI.class
			.getName());

	private final static String lineEnd = "\r\n";

	private final static String twoHyphens = "--";

	private final static String boundary = "***BiToo***";

	private final static int maxBufferSize = 1024 * 100;

	private URL uploadURL;

	private InetAddress address;

	private int port;

	public void init(Properties props) throws ClientAdapterException {
		try {
			uploadURL = new URL(props.getProperty("clientUploadURL"));
		} catch (MalformedURLException e) {
			ClientAdapterException.generate(logger, "Bad client upload url: ["
					+ props.getProperty("clientUploadURL") + "]", e);
		}

		try {
			address = InetAddress.getByName(props.getProperty("clientAddress",
					"localhost"));
		} catch (UnknownHostException e1) {
			ClientAdapterException
					.generate(logger, "Bad client address: ["
							+ props.getProperty("clientAddress", "localhost")
							+ "]", e1);
		}

		try {
			port = Integer.parseInt(props.getProperty("clientPort", "6881"));
		} catch (NumberFormatException nfe) {
			ClientAdapterException.generate(logger, "Bad client port: ["
					+ props.getProperty("clientPort", "localhost") + "]", nfe);

		}
	}

	public boolean launch(File torrentFile) throws ClientAdapterException {
		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		DataInputStream inStream = null;
		try {
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

			conn.setRequestProperty("Content-Type",
					"multipart/form-data;boundary=" + boundary);

			dos = new DataOutputStream(conn.getOutputStream());

			dos.writeBytes(twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data; name=\"upload\";"
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

		} catch (MalformedURLException ex) {
			ClientAdapterException.generate(logger, "Bad upload URL: ["
					+ uploadURL + "]", ex);
		}

		catch (IOException ioe) {
			ClientAdapterException.generate(logger, "Upload failed", ioe);
		}

		//------------------ read the SERVER RESPONSE

		try {
			inStream = new DataInputStream(conn.getInputStream());
			String str;
			while ((str = inStream.readLine()) != null) {
				logger.debug("Server response is: [" + str + "]");
			}
			inStream.close();

		} catch (IOException ioex) {
			ClientAdapterException.generate(logger, "Failing to read response",
					ioex);

		}

		if (conn != null) {
			try {
				return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
			} catch (IOException e) {
				ClientAdapterException.generate(logger,
						"Fail to read response code", e);
			}
		}
		return false;
	}

	public InetAddress getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

}
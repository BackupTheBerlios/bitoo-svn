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

package info.bitoo.downloaders;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class HttpAlternativeDownloader implements AlternativeDownloader {
	
	private final static String NAME_VERSION = HttpAlternativeDownloader.class.getName() + "_0.1";
	
	private URL url;
	
	private HttpURLConnection conn;
	
	private InputStream is;
	
	public HttpAlternativeDownloader(URL url) {
		this.url = url;
	}

	public void start(long startOffset, long endOffset) throws IOException {
		
		conn = (HttpURLConnection) url.openConnection();

		conn.addRequestProperty("Range", "bytes=" + startOffset + "-" + endOffset);

		conn.connect();

		is = new BufferedInputStream(conn.getInputStream());

	}

	
	
	public int read() throws IOException {
		return is.read();
	}

	public void stop() throws IOException {
		is.close();
		conn.disconnect();
	}
	
	public String toString() {
		return NAME_VERSION + " for: (" + url.toString() + ")";
	}

	public void init(Properties props) {
		
	}

}

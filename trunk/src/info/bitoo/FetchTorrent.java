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
package info.bitoo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.gudy.azureus2.core3.util.BDecoder;

public class FetchTorrent {
	private URL torrentURL;

	
	public FetchTorrent(URL torrentURL) {
		this.torrentURL = torrentURL;
	}
	
	public Map getTorrent() throws IOException {
		BufferedInputStream isTorrent = new BufferedInputStream(torrentURL.openStream());
		Map torrentMap = BDecoder.decode(isTorrent);
		return torrentMap;
	}
}

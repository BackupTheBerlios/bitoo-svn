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

import java.io.File;
import java.net.InetAddress;
import java.util.Properties;

public interface ClientAdapter {
	
	public void init(Properties props) throws ClientAdapterException;
	
	public boolean launch(File torrent) throws ClientAdapterException;
	
	public InetAddress getAddress();
	
	public int getPort();
}
